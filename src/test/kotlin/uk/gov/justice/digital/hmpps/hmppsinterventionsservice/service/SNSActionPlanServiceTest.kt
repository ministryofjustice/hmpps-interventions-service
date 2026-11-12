package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.component.SNSPublisher
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.EventDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.PersonReference
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.ActionPlanEvent
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.ActionPlanEventType
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AuthUser
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.SampleData
import java.time.OffsetDateTime
import java.util.UUID

internal class SNSActionPlanServiceTest {
  private val snsPublisher = mock<SNSPublisher>()
  private val actionPlanService = mock<ActionPlanService>()

  private val actionPlan = SampleData.sampleActionPlan(
    id = UUID.fromString("77df9f6c-3fcb-4ec6-8fcf-96551cd9b080"),
    referral = SampleData.sampleReferral(
      "X123456",
      "Harmony Living",
      id = UUID.fromString("68df9f6c-3fcb-4ec6-8fcf-96551cd9b080"),
      referenceNumber = "HAS71263",
      sentAt = OffsetDateTime.parse("2020-12-04T10:42:43+00:00"),
    ),
    submittedBy = AuthUser("abc123", "auth", "abc123"),
    submittedAt = OffsetDateTime.parse("2020-12-04T10:42:43+00:00"),
  )
  private val actionPlanSubmittedEvent = ActionPlanEvent(
    "source",
    ActionPlanEventType.SUBMITTED,
    actionPlan.id,
    "http://localhost:8080/action-plan/77df9f6c-3fcb-4ec6-8fcf-96551cd9b080",
  )

  @Test
  fun `actionPlan assigned event publishes message with valid json`() {
    whenever(actionPlanService.getActionPlan(actionPlan.id)).thenReturn(actionPlan)
    snsActionPlanService().onApplicationEvent(actionPlanSubmittedEvent)
    val snsEvent = EventDTO(
      "intervention.action-plan.submitted",
      "A draft action plan has been submitted",
      "http://localhost:8080/action-plan/${actionPlan.id}",
      actionPlan.submittedAt!!,
      mapOf(
        "actionPlanId" to UUID.fromString("77df9f6c-3fcb-4ec6-8fcf-96551cd9b080"),
        "submittedBy" to actionPlan.submittedBy!!.userName,
        "referralId" to UUID.fromString("68df9f6c-3fcb-4ec6-8fcf-96551cd9b080"),
        "referralReference" to "HAS71263",
        "contractTypeName" to "Accommodation",
        "primeProviderName" to "Harmony Living",
        "actionPlanProbationUserUrl" to "https://base/url/action-plans/68df9f6c-3fcb-4ec6-8fcf-96551cd9b080",
      ),
      PersonReference.crn(actionPlan.referral.serviceUserCRN),
    )
    verify(snsPublisher).publish(
      actionPlan.referral.id,
      actionPlan.submittedBy!!,
      snsEvent,
    )
  }

  @Test
  fun `approved event reloads the plan and publishes approval details`() {
    val approvedAt = OffsetDateTime.parse("2020-12-05T10:42:43Z")
    val approvedBy = AuthUser("approver", "auth", "approver")
    actionPlan.approvedAt = approvedAt
    actionPlan.approvedBy = approvedBy
    whenever(actionPlanService.getActionPlan(actionPlan.id)).thenReturn(actionPlan)
    val event = ActionPlanEvent("source", ActionPlanEventType.APPROVED, actionPlan.id, actionPlanSubmittedEvent.detailUrl)

    snsActionPlanService().onApplicationEvent(event)

    verify(actionPlanService).getActionPlan(actionPlan.id)
    val captor = argumentCaptor<EventDTO>()
    verify(snsPublisher).publish(eq(actionPlan.referral.id), eq(approvedBy), captor.capture())
    assertThat(captor.firstValue.eventType).isEqualTo("intervention.action-plan.approved")
    assertThat(captor.firstValue.occurredAt).isEqualTo(approvedAt)
    assertThat(captor.firstValue.additionalInformation).containsEntry("approvedBy", approvedBy.userName)
    assertThat(captor.firstValue.detailUrl).isEqualTo(event.detailUrl)
  }

  private fun snsActionPlanService(): SNSActionPlanService = SNSActionPlanService(snsPublisher, "https://base/url", "/action-plans/{id}", actionPlanService)
}
