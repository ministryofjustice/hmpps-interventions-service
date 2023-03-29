package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service

import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.component.CommunityAPIClient
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.ActionPlanEvent
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.ActionPlanEventType
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.ActionPlanEventType.APPROVED
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.ActionPlanEventType.SUBMITTED
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ActionPlanFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ReferralFactory
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

class CommunityAPIActionPlanEventServiceTest {

  private val communityAPIClient = mock<CommunityAPIClient>()

  private val sentAtDefault = OffsetDateTime.of(2020, 1, 1, 1, 1, 1, 1, ZoneOffset.UTC)
  private val submittedAtDefault = OffsetDateTime.of(2020, 2, 2, 2, 2, 2, 2, ZoneOffset.UTC)
  private val approvedAtDefault = OffsetDateTime.of(2020, 3, 3, 3, 3, 3, 3, ZoneOffset.UTC)

  private val actionPlanFactory = ActionPlanFactory()
  private val referralFactory = ReferralFactory()

  val communityAPIService = CommunityAPIActionPlanEventService(
    "http://testUrl",
    "/probation-practitioner/referral/{id}/action-plan",
    "/secure/offenders/crn/{crn}/sentence/{sentenceId}/notifications/context/{contextName}",
    "commissioned-rehabilitation-services",
    communityAPIClient,
  )

  @Test
  fun `notify submitted action plan`() {
    val event = getEvent(SUBMITTED)
    communityAPIService.onApplicationEvent(event)

    verify(communityAPIClient).makeAsyncPostRequest(
      "/secure/offenders/crn/X123456/sentence/1234/notifications/context/commissioned-rehabilitation-services",
      NotificationCreateRequestDTO(
        "ACC",
        sentAtDefault,
        event.actionPlan.referral.id,
        submittedAtDefault,
        "Action Plan Submitted for Accommodation Referral XX1234 with Prime Provider Harmony Living\n" +
          "http://testUrl/probation-practitioner/referral/${event.actionPlan.referral.id}/action-plan",
      ),
    )
  }

  @Test
  fun `notify approved action plan`() {
    val event = getEvent(APPROVED)
    communityAPIService.onApplicationEvent(event)

    verify(communityAPIClient).makeAsyncPostRequest(
      "/secure/offenders/crn/X123456/sentence/1234/notifications/context/commissioned-rehabilitation-services",
      NotificationCreateRequestDTO(
        "ACC",
        sentAtDefault,
        event.actionPlan.referral.id,
        approvedAtDefault,
        "Action Plan Approved for Accommodation Referral XX1234 with Prime Provider Harmony Living\n" +
          "http://testUrl/probation-practitioner/referral/${event.actionPlan.referral.id}/action-plan",
      ),
    )
  }

  private fun getEvent(
    ActionPlanEventType: ActionPlanEventType,
  ): ActionPlanEvent =
    ActionPlanEvent(
      "source",
      ActionPlanEventType,
      actionPlanFactory.create(
        id = UUID.fromString("120b1a45-8ac7-4920-b05b-acecccf4734b"),
        submittedAt = submittedAtDefault,
        approvedAt = approvedAtDefault,
        referral = referralFactory.createSent(
          serviceUserCRN = "X123456",
          relevantSentenceId = 1234L,
          sentAt = sentAtDefault,
          referenceNumber = "XX1234",
        ),
      ),
      "http://localhost:8080/submit-action-plan/68df9f6c-3fcb-4ec6-8fcf-96551cd9b080",
    )
}
