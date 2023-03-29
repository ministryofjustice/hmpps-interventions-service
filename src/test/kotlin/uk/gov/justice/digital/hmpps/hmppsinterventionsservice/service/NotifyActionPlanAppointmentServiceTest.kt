package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.component.EmailSender
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.ActionPlanAppointmentEvent
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.ActionPlanAppointmentEventType
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Attended
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.SampleData
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ActionPlanFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.AuthUserFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.DeliverySessionFactory
import java.time.OffsetDateTime
import java.util.UUID

class NotifyActionPlanAppointmentServiceTest {
  private val emailSender = mock<EmailSender>()
  private val referralService = mock<ReferralService>()
  private val deliverySessionFactory = DeliverySessionFactory()
  private val actionPlanFactory = ActionPlanFactory()
  private val authUserFactory = AuthUserFactory()

  private fun generateAppointmentEvent(type: ActionPlanAppointmentEventType, notifyPP: Boolean = false, attended: Attended = Attended.YES): ActionPlanAppointmentEvent {
    return ActionPlanAppointmentEvent.from(
      "source",
      type,
      deliverySessionFactory.createAttended(
        id = UUID.fromString("42c7d267-0776-4272-a8e8-a673bfe30d0d"),
        deliusAppointmentId = 12345L,
        referral = SampleData.sampleReferral(
          "X123456",
          "Harmony Living",
          id = UUID.fromString("68df9f6c-3fcb-4ec6-8fcf-96551cd9b080"),
          referenceNumber = "HAS71263",
          sentAt = OffsetDateTime.parse("2020-12-04T10:42:43+00:00"),
          sentBy = authUserFactory.create(),
          actionPlans = mutableListOf(actionPlanFactory.create(id = UUID.fromString("4907ffb5-94cf-4eff-8cf9-dcf09765be42"))),
          relevantSentenceId = 123456,
          supplementaryRiskId = UUID.randomUUID(),
        ),
        createdBy = SampleData.sampleAuthUser(),
        attended = attended,
        notifyPPOfAttendanceBehaviour = notifyPP,
      ),
      "http://localhost:8080/appointment/42c7d267-0776-4272-a8e8-a673bfe30d0d",
    )
  }
  private fun notifyService(): NotifyActionPlanAppointmentService {
    return NotifyActionPlanAppointmentService(
      "template",
      "template",
      "http://example.com",
      "/pp/referrals/{id}/session/{sessionNumber}/appointment/{appointmentId}/feedback",
      emailSender,
      referralService,
    )
  }

  @Test
  fun `appointment attendance recorded event does not send email when user details are not available`() {
    whenever(referralService.getResponsibleProbationPractitioner(any())).thenThrow(RuntimeException::class.java)
    assertThrows<RuntimeException> {
      notifyService().onApplicationEvent(generateAppointmentEvent(ActionPlanAppointmentEventType.ATTENDANCE_RECORDED, attended = Attended.NO))
    }
    verifyNoInteractions(emailSender)
  }

  @Test
  fun `appointment attendance recorded event does not send email when attended is YES`() {
    notifyService().onApplicationEvent(generateAppointmentEvent(ActionPlanAppointmentEventType.ATTENDANCE_RECORDED, false, Attended.YES))
    verifyNoInteractions(emailSender)
  }

  @Test
  fun `appointment attendance recorded event calls email client when attended is NO`() {
    whenever(referralService.getResponsibleProbationPractitioner(any(), any(), any())).thenReturn(ResponsibleProbationPractitioner("abc", "abc@abc.com", null, null, "def"))

    notifyService().onApplicationEvent(generateAppointmentEvent(ActionPlanAppointmentEventType.ATTENDANCE_RECORDED, false, Attended.NO))
    val personalisationCaptor = argumentCaptor<Map<String, String>>()
    verify(emailSender).sendEmail(eq("template"), eq("abc@abc.com"), personalisationCaptor.capture())
    Assertions.assertThat(personalisationCaptor.firstValue["ppFirstName"]).isEqualTo("abc")
    Assertions.assertThat(personalisationCaptor.firstValue["referenceNumber"]).isEqualTo("HAS71263")
    Assertions.assertThat(personalisationCaptor.firstValue["attendanceUrl"]).isEqualTo("http://example.com/pp/referrals/68df9f6c-3fcb-4ec6-8fcf-96551cd9b080/session/1/appointment/12345/feedback")
  }

  @Test
  fun `appointment behaviour recorded event does not send email when user details are not available`() {
    whenever(referralService.getResponsibleProbationPractitioner(any())).thenThrow(RuntimeException::class.java)
    assertThrows<RuntimeException> {
      notifyService().onApplicationEvent(generateAppointmentEvent(ActionPlanAppointmentEventType.BEHAVIOUR_RECORDED, true))
    }
    verifyNoInteractions(emailSender)
  }

  @Test
  fun `appointment behaviour recorded event does not send email when notifyPP is false`() {
    notifyService().onApplicationEvent(generateAppointmentEvent(ActionPlanAppointmentEventType.BEHAVIOUR_RECORDED, false))
    verifyNoInteractions(emailSender)
  }

  @Test
  fun `appointment behaviour recorded event calls email client`() {
    whenever(referralService.getResponsibleProbationPractitioner(any(), any(), any())).thenReturn(ResponsibleProbationPractitioner("abc", "abc@abc.com", null, null, "def"))

    notifyService().onApplicationEvent(generateAppointmentEvent(ActionPlanAppointmentEventType.BEHAVIOUR_RECORDED, true))
    val personalisationCaptor = argumentCaptor<Map<String, String>>()
    verify(emailSender).sendEmail(eq("template"), eq("abc@abc.com"), personalisationCaptor.capture())
    Assertions.assertThat(personalisationCaptor.firstValue["ppFirstName"]).isEqualTo("abc")
    Assertions.assertThat(personalisationCaptor.firstValue["referenceNumber"]).isEqualTo("HAS71263")
    Assertions.assertThat(personalisationCaptor.firstValue["sessionUrl"]).isEqualTo("http://example.com/pp/referrals/68df9f6c-3fcb-4ec6-8fcf-96551cd9b080/session/1/appointment/12345/feedback")
  }
}
