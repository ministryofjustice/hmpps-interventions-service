package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.listeners

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.component.CommunityAPIClient
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.component.EmailSender
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.component.SNSPublisher
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.EventDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.PersonReference
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.ReferralConcludedEvent
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AuthUser
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.EndOfServiceReport
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ReferralAssignment
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.SampleData
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.HMPPSAuthService
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.NotificationCreateRequestDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.ReferralConcludedState
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.UserDetail
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.AssignmentsFactory
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
  endOfServiceReport: EndOfServiceReport? = null,
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
      endOfServiceReport = endOfServiceReport,
      assignments = listOf(
        ReferralAssignment(
          OffsetDateTime.parse("2020-12-04T10:42:43+00:00"),
          authUserFactory.createSP("irrelevant"),
          authUserFactory.createSP("abc123"),
        ),
      ),
    ),
    "http://localhost:8080/sent-referral/68df9f6c-3fcb-4ec6-8fcf-96551cd9b080",
  )
}

internal class ReferralConcludedListenerTest {
  private val snsPublisher = mock<SNSPublisher>()
  private val listener = ReferralConcludedListener(snsPublisher)

  @ParameterizedTest
  @EnumSource(ReferralConcludedState::class)
  fun `publishes referral concluded event`(state: ReferralConcludedState) {
    val referralConcludedEvent = referralConcludedEvent(state)
    listener.onApplicationEvent(referralConcludedEvent)
    val snsEvent = EventDTO(
      "intervention.referral.concluded",
      "The referral has concluded, no more work is needed",
      "http://localhost:8080" + "/sent-referral/${referralConcludedEvent.referral.id}",
      referralConcludedEvent.referral.concludedAt!!,
      mapOf(
        "deliveryState" to state.name,
        "referralId" to UUID.fromString("68df9f6c-3fcb-4ec6-8fcf-96551cd9b080"),
      ),
      PersonReference.crn(referralConcludedEvent.referral.serviceUserCRN),
    )
    verify(snsPublisher).publish(referralConcludedEvent.referral.id, AuthUser.interventionsServiceUser, snsEvent)
  }
}

internal class ReferralConcludedIntegrationListenerTest {
  private val communityAPIClient = mock<CommunityAPIClient>()
  private val listener = ReferralConcludedIntegrationListener(
    "http://testUrl",
    "/pp/referral/{id}/end-of-service-report",
    "secure/offenders/crn/{crn}/sentence/{sentenceId}/notifications/context/{contextName}",
    "commissioned-rehabilitation-services",
    communityAPIClient,
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
      Unit::class.java,
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
      Unit::class.java,
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
        concludedAt = concludedAtDefault,
      ),
      submittedAt = submittedAtDefault,
    )
}

internal class ReferralConcludedNotificationListenerTest {
  private val emailSender = mock<EmailSender>()
  private val hmppsAuthService = mock<HMPPSAuthService>()
  private val referralFactory = ReferralFactory()
  private val assignmentsFactory = AssignmentsFactory()

  private val sentReferral = referralFactory.createSent(
    id = UUID.fromString("68df9f6c-3fcb-4ec6-8fcf-96551cd9b080"),
    referenceNumber = "JS8762AC",
    assignments = assignmentsFactory.create(1),
  )

  private val referralCancelledEvent = ReferralConcludedEvent(
    "source",
    ReferralConcludedState.CANCELLED,
    sentReferral,
    "http://localhost:8080/sent-referral/68df9f6c-3fcb-4ec6-8fcf-96551cd9b080",
  )

  private fun notifyService(): ReferralConcludedNotificationListener {
    return ReferralConcludedNotificationListener(
      "cancelledTemplateID",
      emailSender,
      hmppsAuthService,
    )
  }

  @Test
  fun `referral sent event generates valid url and sends an email`() {
    whenever(hmppsAuthService.getUserDetail(any<AuthUser>())).thenReturn(UserDetail("tom", "tom@tom.tom", "jones"))
    notifyService().onApplicationEvent(referralCancelledEvent)
    val personalisationCaptor = argumentCaptor<Map<String, String>>()
    verify(emailSender).sendEmail(
      eq("cancelledTemplateID"),
      eq("tom@tom.tom"),
      personalisationCaptor.capture(),
    )
    Assertions.assertThat(personalisationCaptor.firstValue["sp_first_name"]).isEqualTo("tom")
    Assertions.assertThat(personalisationCaptor.firstValue["referral_number"]).isEqualTo("JS8762AC")
  }
}
