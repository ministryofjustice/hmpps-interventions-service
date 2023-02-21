package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.listeners

import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.component.CommunityAPIClient
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
private val concludedAtDefault = OffsetDateTime.of(2020, 2, 2, 2, 2, 2, 2, ZoneOffset.UTC)

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
      sentAt = sentAtDefault,
      concludedAt = concludedAtDefault,
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
  private val communityAPIClient = mock<CommunityAPIClient>()
  private val listener = ReferralEndingListener(
    "http://testUrl",
    "/pp/referral/{id}",
    "secure/offenders/crn/{crn}/referral/end/context/{contextName}",
    "commissioned-rehabilitation-services",
    communityAPIClient
  )

  @Test
  fun `terminates the NSI for the cancelled referral`() {
    val event = referralEndingEvent(ReferralConcludedState.CANCELLED)
    listener.onApplicationEvent(event)

    verify(communityAPIClient).makeAsyncPostRequest(
      "secure/offenders/crn/X123456/referral/end/context/commissioned-rehabilitation-services",
      ReferralEndRequest(
        "ACC",
        sentAtDefault,
        concludedAtDefault,
        123456789,
        event.referral.id,
        "CANCELLED",
        "Referral Ended for Accommodation Referral HAS71263 with Prime Provider Harmony Living\n" +
          "http://testUrl/pp/referral/68df9f6c-3fcb-4ec6-8fcf-96551cd9b080",
      )
    )
  }

  @Test
  fun `terminates the NSI for the prematurely ended referral`() {
    val event = referralEndingEvent(ReferralConcludedState.PREMATURELY_ENDED)
    listener.onApplicationEvent(event)

    verify(communityAPIClient).makeAsyncPostRequest(
      "secure/offenders/crn/X123456/referral/end/context/commissioned-rehabilitation-services",
      ReferralEndRequest(
        "ACC",
        sentAtDefault,
        concludedAtDefault,
        123456789,
        event.referral.id,
        "PREMATURELY_ENDED",
        "Referral Ended for Accommodation Referral HAS71263 with Prime Provider Harmony Living\n" +
          "http://testUrl/pp/referral/68df9f6c-3fcb-4ec6-8fcf-96551cd9b080",
      )
    )
  }

  @Test
  fun `terminates the NSI for the completed referral`() {
    val event = referralEndingEvent(ReferralConcludedState.COMPLETED)
    listener.onApplicationEvent(event)

    verify(communityAPIClient).makeAsyncPostRequest(
      "secure/offenders/crn/X123456/referral/end/context/commissioned-rehabilitation-services",
      ReferralEndRequest(
        "ACC",
        sentAtDefault,
        concludedAtDefault,
        123456789,
        event.referral.id,
        "COMPLETED",
        "Referral Ended for Accommodation Referral HAS71263 with Prime Provider Harmony Living\n" +
          "http://testUrl/pp/referral/68df9f6c-3fcb-4ec6-8fcf-96551cd9b080",
      )
    )
  }
}
