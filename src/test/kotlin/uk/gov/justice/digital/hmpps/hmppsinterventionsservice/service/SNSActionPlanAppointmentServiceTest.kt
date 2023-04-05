package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.component.SNSPublisher
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.AuthUserDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.EventDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.PersonReference
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.ActionPlanAppointmentEvent
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.ActionPlanAppointmentEventType
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Attended
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ActionPlanFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.DeliverySessionFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ReferralFactory
import java.time.OffsetDateTime
import java.util.UUID

internal class SNSActionPlanAppointmentServiceTest {
  private val publisher = mock<SNSPublisher>()
  private val snsAppointmentService = SNSActionPlanAppointmentService(
    publisher,
    "http://baseUrl",
    "/probation-practitioner/referrals/{id}/appointment/{sessionNumber}/post-session-feedback",
  )

  private val referralFactory = ReferralFactory()
  private val deliverySessionFactory = DeliverySessionFactory()
  private val actionPlan = ActionPlanFactory().create(
    referral = referralFactory.createSent(id = UUID.fromString("56b40f96-0657-4e01-925c-da208a6fbcfd")),
  )
  private val now = OffsetDateTime.now()
  private fun attendanceRecordedEvent(attendance: Attended) = ActionPlanAppointmentEvent.from(
    "source",
    ActionPlanAppointmentEventType.ATTENDANCE_RECORDED,
    deliverySessionFactory.createAttended(
      referral = referralFactory.createSent(
        id = UUID.fromString("56b40f96-0657-4e01-925c-da208a6fbcfd"),
        actionPlans = mutableListOf(actionPlan),
      ),
      createdBy = actionPlan.createdBy,
      attended = attendance,
      attendanceSubmittedAt = now,
      attendanceSubmittedBy = actionPlan.createdBy,
      appointmentFeedbackSubmittedBy = actionPlan.createdBy,
      appointmentFeedbackSubmittedAt = now,
    ),
    "http://localhost:8080/action-plan/77df9f6c-3fcb-4ec6-8fcf-96551cd9b080/session/1",
  )

  private fun sessionFeedbackEvent(attendance: Attended) = ActionPlanAppointmentEvent.from(
    "source",
    ActionPlanAppointmentEventType.SESSION_FEEDBACK_RECORDED,
    deliverySessionFactory.createAttended(
      referral = referralFactory.createSent(
        id = UUID.fromString("56b40f96-0657-4e01-925c-da208a6fbcfd"),
        actionPlans = mutableListOf(actionPlan),
      ),
      createdBy = actionPlan.createdBy,
      attended = attendance,
      attendanceSubmittedAt = now,
      appointmentFeedbackSubmittedBy = actionPlan.createdBy,
      appointmentFeedbackSubmittedAt = now,
      deliusAppointmentId = 123L,
    ),
    "http://localhost:8080/action-plan/77df9f6c-3fcb-4ec6-8fcf-96551cd9b080/session/1",
  )

  @Test
  fun `appointment attendance recorded event publishes message with valid DTO`() {
    snsAppointmentService.onApplicationEvent(attendanceRecordedEvent(Attended.NO))

    val referralId = UUID.fromString("56b40f96-0657-4e01-925c-da208a6fbcfd")
    val eventDTO = EventDTO(
      eventType = "intervention.session-appointment.missed",
      description = "Attendance was recorded for a session appointment",
      detailUrl = "http://localhost:8080/action-plan/77df9f6c-3fcb-4ec6-8fcf-96551cd9b080/session/1",
      occurredAt = now,
      additionalInformation = mapOf(
        "serviceUserCRN" to "X123456",
        "referralId" to referralId,
      ),
      PersonReference.crn("X123456"),
    )

    verify(publisher).publish(referralId, AuthUserDTO.from(actionPlan.createdBy), eventDTO)
  }

  @Test
  fun `appointment feedback submitted event publishes message with valid DTO`() {
    snsAppointmentService.onApplicationEvent(sessionFeedbackEvent(Attended.YES))

    val referralId = UUID.fromString("56b40f96-0657-4e01-925c-da208a6fbcfd")
    val eventDTO = EventDTO(
      eventType = "intervention.session-appointment.session-feedback-submitted",
      description = "Session feedback submitted for a session appointment",
      detailUrl = "http://localhost:8080/action-plan/77df9f6c-3fcb-4ec6-8fcf-96551cd9b080/session/1",
      occurredAt = now,
      additionalInformation = mapOf(
        "serviceUserCRN" to "X123456",
        "referralId" to referralId,
        "referralReference" to "JS18726AC",
        "contractTypeName" to "Accommodation",
        "primeProviderName" to "Harmony Living",
        "deliusAppointmentId" to "123",
        "referralProbationUserURL" to "http://baseUrl/probation-practitioner/referrals/56b40f96-0657-4e01-925c-da208a6fbcfd/appointment/1/post-session-feedback",
      ),
      PersonReference.crn("X123456"),
    )

    verify(publisher).publish(referralId, AuthUserDTO.from(actionPlan.createdBy), eventDTO)
  }

  @Test
  fun `appointment attendance recorded event type reflects attendance status`() {
    snsAppointmentService.onApplicationEvent(attendanceRecordedEvent(Attended.YES))
    snsAppointmentService.onApplicationEvent(attendanceRecordedEvent(Attended.LATE))

    val eventCaptor = argumentCaptor<EventDTO>()
    verify(publisher, times(2)).publish(eq(actionPlan.referral.id), eq(AuthUserDTO.from(actionPlan.createdBy)), eventCaptor.capture())
    eventCaptor.allValues.forEach {
      assertThat(it.eventType).isEqualTo("intervention.session-appointment.attended")
    }
  }
}
