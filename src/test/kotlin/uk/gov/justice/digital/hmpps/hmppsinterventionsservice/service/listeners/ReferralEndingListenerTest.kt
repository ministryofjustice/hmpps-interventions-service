package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.listeners

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.component.CommunityAPIClient
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.component.SNSPublisher
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.EventDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.PersonIdentifier
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.PersonReference
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.ReferralEndingEvent
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AuthUser
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ReferralAssignment
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.ReferralConcludedState
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.ReferralEndRequest
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.AuthUserFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ReferralFactory
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

private val sentAtDefault = OffsetDateTime.of(2020, 1, 1, 1, 1, 1, 1, ZoneOffset.UTC)
private val cancelledAtDefault = OffsetDateTime.of(2020, 2, 2, 2, 2, 2, 2, ZoneOffset.UTC)

private fun referralEndingEvent(state: ReferralConcludedState): ReferralEndingEvent {
  val referralFactory = ReferralFactory()
  val authUserFactory = AuthUserFactory()
  return ReferralEndingEvent(
    "source",
    state,
    referralFactory.createEnded(
      id = UUID.fromString("68df9f6c-3fcb-4ec6-8fcf-96551cd9b080"),
      referenceNumber = "HAS71263",
      relevantSentenceId = 123456789,
      serviceUserCRN = "T123000",
      sentAt = sentAtDefault,
      endRequestedAt = cancelledAtDefault,
      endRequestedBy = AuthUser("ecd7b8d690", "irrelevant", "irrelevant"),
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

internal class ReferralEndingListenerTest {
  private val snsPublisher = mock<SNSPublisher>()
  private val communityAPIClient = mock<CommunityAPIClient>()
  private val listener = ReferralEndingListener(
    "http://testUrl",
    "/pp/referral/{id}",
    "secure/offenders/crn/{crn}/referral/end/context/{contextName}",
    "commissioned-rehabilitation-services",
    snsPublisher,
    communityAPIClient,
  )

  @ParameterizedTest
  @EnumSource(ReferralConcludedState::class)
  fun `publishes domain event`(state: ReferralConcludedState) {
    val event = referralEndingEvent(state)
    listener.onApplicationEvent(event)

    val eventPayload = EventDTO(
      eventType = "intervention.referral.ended",
      description = "The referral has ended. It might still require admin work from providers",
      detailUrl = "http://localhost:8080/sent-referral/68df9f6c-3fcb-4ec6-8fcf-96551cd9b080",
      occurredAt = cancelledAtDefault,
      additionalInformation = mapOf(
        "deliveryState" to state.name,
        "referralId" to "68df9f6c-3fcb-4ec6-8fcf-96551cd9b080",
        "referralURN" to "urn:hmpps:interventions-referral:68df9f6c-3fcb-4ec6-8fcf-96551cd9b080",
        "referralProbationUserURL" to "http://testUrl/pp/referral/68df9f6c-3fcb-4ec6-8fcf-96551cd9b080",
      ),
      personReference = PersonReference(
        identifiers = listOf(
          PersonIdentifier(type = "CRN", value = "T123000")
        )
      ),
    )
    verify(snsPublisher).publish(event.referral.id, AuthUser.interventionsServiceUser, eventPayload)
  }

  @ParameterizedTest
  @EnumSource(ReferralConcludedState::class)
  fun `terminates the NSI`(state: ReferralConcludedState) {
    val event = referralEndingEvent(state)
    listener.onApplicationEvent(event)

    verify(communityAPIClient).makeAsyncPostRequest(
      "secure/offenders/crn/T123000/referral/end/context/commissioned-rehabilitation-services",
      ReferralEndRequest(
        contractType = "ACC",
        startedAt = sentAtDefault,
        endedAt = cancelledAtDefault,
        sentenceId = 123456789,
        referralId = event.referral.id,
        endType = state.name,
        notes = "Referral Ended for Accommodation Referral HAS71263 with Prime Provider Harmony Living\n" +
          "http://testUrl/pp/referral/68df9f6c-3fcb-4ec6-8fcf-96551cd9b080",
      )
    )
  }
}
