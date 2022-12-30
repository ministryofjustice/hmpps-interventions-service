package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.listeners

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.component.CommunityAPIClient
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.component.SNSPublisher
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.EventDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.ReferralConcludedEvent
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AuthUser
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.EndOfServiceReport
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ReferralAssignment
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.SampleData
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.NotificationCreateRequestDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.ReferralConcludedState
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.AuthUserFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ReferralFactory
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

private val sentAtDefault = OffsetDateTime.of(2020, 1, 1, 1, 1, 1, 1, ZoneOffset.UTC)
private val concludedAtDefault = OffsetDateTime.of(2020, 2, 2, 2, 2, 2, 2, ZoneOffset.UTC)
private val submittedAtDefault = OffsetDateTime.of(2020, 3, 3, 3, 3, 3, 3, ZoneOffset.UTC)

private fun referralConcludedEvent(
  eventType: ReferralConcludedState,
  endOfServiceReport: EndOfServiceReport? = null
): ReferralConcludedEvent {
  val referralFactory = ReferralFactory()
  val authUserFactory = AuthUserFactory()
  return ReferralConcludedEvent(
    "source",
    eventType,
    referralFactory.createEnded(
      id = UUID.fromString("68df9f6c-3fcb-4ec6-8fcf-96551cd9b080"),
      referenceNumber = "HAS71263",
      relevantSentenceId = 123456789,
      sentAt = sentAtDefault,
      concludedAt = concludedAtDefault,
      endRequestedBy = AuthUser("ecd7b8d690", "irrelevant", "irrelevant"),
      endOfServiceReport = endOfServiceReport,
      assignments = listOf(
        ReferralAssignment(
          OffsetDateTime.parse("2020-12-04T10:42:43+00:00"),
          authUserFactory.createSP("irrelevant"),
          authUserFactory.createSP("abc123")
        )
      )
    ),
    "http://localhost:8080/sent-referral/68df9f6c-3fcb-4ec6-8fcf-96551cd9b080"
  )
}

internal class ReferralConcludedListenerTest {
  private val snsPublisher = mock<SNSPublisher>()
  private val listener = ReferralConcludedListener(snsPublisher)

  @Test
  fun `publishes referral cancelled domain event`() {
    val referralConcludedEvent = referralConcludedEvent(ReferralConcludedState.CANCELLED)
    listener.onApplicationEvent(referralConcludedEvent)
    val snsEvent = EventDTO(
      "intervention.referral.cancelled",
      "A referral has been cancelled",
      "http://localhost:8080" + "/sent-referral/${referralConcludedEvent.referral.id}",
      referralConcludedEvent.referral.concludedAt!!,
      mapOf("referralId" to UUID.fromString("68df9f6c-3fcb-4ec6-8fcf-96551cd9b080"))
    )
    verify(snsPublisher).publish(
      referralConcludedEvent.referral.id,
      referralConcludedEvent.referral.endRequestedBy!!,
      snsEvent
    )
  }

  @Test
  fun `publishes referral prematurely ended domain event`() {
    val referralConcludedEvent = referralConcludedEvent(ReferralConcludedState.PREMATURELY_ENDED)
    listener.onApplicationEvent(referralConcludedEvent)
    val snsEvent = EventDTO(
      "intervention.referral.prematurely-ended",
      "A referral has been ended prematurely",
      "http://localhost:8080" + "/sent-referral/${referralConcludedEvent.referral.id}",
      referralConcludedEvent.referral.concludedAt!!,
      mapOf("referralId" to UUID.fromString("68df9f6c-3fcb-4ec6-8fcf-96551cd9b080"))
    )
    verify(snsPublisher).publish(
      referralConcludedEvent.referral.id,
      referralConcludedEvent.referral.endRequestedBy!!,
      snsEvent
    )
  }

  @Test
  fun `publishes referral completed domain event with actor as Interventions service user`() {
    val referralConcludedEvent = referralConcludedEvent(ReferralConcludedState.COMPLETED)
    listener.onApplicationEvent(referralConcludedEvent)
    val snsEvent = EventDTO(
      "intervention.referral.completed",
      "A referral has been completed",
      "http://localhost:8080" + "/sent-referral/${referralConcludedEvent.referral.id}",
      referralConcludedEvent.referral.concludedAt!!,
      mapOf("referralId" to UUID.fromString("68df9f6c-3fcb-4ec6-8fcf-96551cd9b080"))
    )
    verify(snsPublisher).publish(
      referralConcludedEvent.referral.id,
      AuthUser("00000000-0000-0000-0000-000000000000", "urn:hmpps:interventions", "hmpps-interventions-service"),
      snsEvent
    )
  }
}

internal class ReferralConcludedIntegrationListenerTest {
  private val communityAPIClient = mock<CommunityAPIClient>()
  private val listener = ReferralConcludedIntegrationListener(
    "http://testUrl",
    "/pp/referral/{id}/end-of-service-report",
    "secure/offenders/crn/{crn}/sentence/{sentenceId}/notifications/context/{contextName}",
    "commissioned-rehabilitation-services",
    communityAPIClient
  )

  @Test
  fun `notifies Delius that an end of service report has been submitted on a prematurely ended referral`() {
    val event = referralConcludedEvent(ReferralConcludedState.PREMATURELY_ENDED, endOfServiceReport)
    listener.onApplicationEvent(event)

    verify(communityAPIClient).makeSyncPostRequest(
      "secure/offenders/crn/X123456/sentence/123456789/notifications/context/commissioned-rehabilitation-services",
      NotificationCreateRequestDTO(
        "ACC",
        sentAtDefault,
        event.referral.id,
        submittedAtDefault,
        "End of Service Report Submitted for Accommodation Referral HAS71263 with Prime Provider Harmony Living\n" +
          "http://testUrl/pp/referral/120b1a45-8ac7-4920-b05b-acecccf4734b/end-of-service-report",
      ),
      Unit::class.java
    )
  }

  @Test
  fun `throws exception when called without end-of-service report on a prematurely ended referral`() {
    val event = referralConcludedEvent(ReferralConcludedState.PREMATURELY_ENDED)
    assertThrows<IllegalStateException> { listener.onApplicationEvent(event) }
  }

  @Test
  fun `notifies Delius that an end of service report has been submitted on a completed referral`() {
    val event = referralConcludedEvent(ReferralConcludedState.COMPLETED, endOfServiceReport)
    listener.onApplicationEvent(event)

    verify(communityAPIClient).makeSyncPostRequest(
      "secure/offenders/crn/X123456/sentence/123456789/notifications/context/commissioned-rehabilitation-services",
      NotificationCreateRequestDTO(
        "ACC",
        sentAtDefault,
        event.referral.id,
        submittedAtDefault,
        "End of Service Report Submitted for Accommodation Referral HAS71263 with Prime Provider Harmony Living\n" +
          "http://testUrl/pp/referral/120b1a45-8ac7-4920-b05b-acecccf4734b/end-of-service-report",
      ),
      Unit::class.java
    )
  }

  @Test
  fun `throws exception when called without end-of-service report on a completed referral`() {
    val event = referralConcludedEvent(ReferralConcludedState.COMPLETED)
    assertThrows<IllegalStateException> { listener.onApplicationEvent(event) }
  }

  private val endOfServiceReport =
    SampleData.sampleEndOfServiceReport(
      id = UUID.fromString("120b1a45-8ac7-4920-b05b-acecccf4734b"),
      referral = SampleData.sampleReferral(
        id = UUID.fromString("68df9f6c-3fcb-4ec6-8fcf-96551cd9b080"),
        referenceNumber = "HAS71263",
        crn = "X123456",
        relevantSentenceId = 123456789,
        serviceProviderName = "Harmony Living",
        sentAt = sentAtDefault,
        concludedAt = concludedAtDefault
      ),
      submittedAt = submittedAtDefault
    )
}
