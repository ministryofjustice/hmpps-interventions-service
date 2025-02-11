package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.component.EmailSender
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.AppointmentEvent
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.AppointmentEventType
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AppointmentType
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Attended
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.DeliverySession
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ReferralServiceUserData
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.SampleData
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.AppointmentFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.DeliverySessionFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ReferralFactory
import java.time.OffsetDateTime
import java.util.UUID

class NotifyAppointmentServiceTest {
  private val emailSender = mock<EmailSender>()
  private val referralService = mock<ReferralService>()
  private val appointmentFactory = AppointmentFactory()
  private val deliverySessionFactory = DeliverySessionFactory()
  private val referralFactory = ReferralFactory()

  private fun appointmentEvent(
    type: AppointmentEventType,
    notifyPP: Boolean,
    appointmentType: AppointmentType = AppointmentType.SUPPLIER_ASSESSMENT,
    deliverySession: DeliverySession? = null,
    notifyProbationPractitionerOfBehaviour: Boolean? = null,
    notifyProbationPractitionerOfConcerns: Boolean? = null,
  ): AppointmentEvent = AppointmentEvent(
    "source",
    type,
    appointmentFactory.create(
      id = deliverySession?.currentAppointment?.id ?: UUID.randomUUID(),
      referral = referralFactory.createSent(
        id = UUID.fromString("68df9f6c-3fcb-4ec6-8fcf-96551cd9b080"),
        serviceUserData = ReferralServiceUserData(firstName = "Bob", lastName = "Green"),
      ),
    ),
    "http://localhost:8080/appointment/42c7d267-0776-4272-a8e8-a673bfe30d0d",
    notifyPP,
    appointmentType,
    deliverySession,
    notifyProbationPractitionerOfBehaviour,
    notifyProbationPractitionerOfConcerns,
  )

  private fun createDeliverySession(attended: Attended, notifyPP: Boolean): DeliverySession = deliverySessionFactory.createAttended(
    id = UUID.fromString("42c7d267-0776-4272-a8e8-a673bfe30d0d"),
    deliusAppointmentId = 12345L,
    referral = SampleData.sampleReferral(
      "X123456",
      "Harmony Living",
      id = UUID.fromString("68df9f6c-3fcb-4ec6-8fcf-96551cd9b080"),
      referenceNumber = "HAS71263",
      sentAt = OffsetDateTime.parse("2020-12-04T10:42:43+00:00"),
      relevantSentenceId = 123456,
      supplementaryRiskId = UUID.randomUUID(),
      serviceUserData = ReferralServiceUserData(firstName = "Bob", lastName = "Green"),
    ),
    createdBy = SampleData.sampleAuthUser(),
    attended = attended,
    notifyPPOfAttendanceBehaviour = notifyPP,
  )

  private fun notifyService(): NotifyAppointmentService = NotifyAppointmentService(
    "appointmentNotAttendedTemplate",
    "poorBehaviourTemplate",
    "concerningBehaviourTemplate",
    "initialAssessmentScheduledTemplate",
    "http://example.com",
    "/probation-practitioner/referrals/{id}/progress",
    "/probation-practitioner/referrals/{id}/supplier-assessment/post-session-feedback",
    "/probation-practitioner/referrals/{id}/session/{sessionNumber}/appointment/{appointmentId}/post-session-feedback",
    emailSender,
    referralService,
  )

  @Test
  fun `supplier assessment appointment attendance recorded event does not send email when user details are not available`() {
    whenever(referralService.getResponsibleProbationPractitioner(any())).thenThrow(RuntimeException::class.java)
    assertThrows<RuntimeException> {
      notifyService().onApplicationEvent(appointmentEvent(AppointmentEventType.ATTENDANCE_RECORDED, true))
    }
    verifyNoInteractions(emailSender)
  }

  @Test
  fun `delivery session appointment attendance recorded event does not send email when user details are not available`() {
    whenever(referralService.getResponsibleProbationPractitioner(any())).thenThrow(RuntimeException::class.java)
    assertThrows<RuntimeException> {
      notifyService().onApplicationEvent(appointmentEvent(AppointmentEventType.ATTENDANCE_RECORDED, true, AppointmentType.SERVICE_DELIVERY, createDeliverySession(Attended.NO, true)))
    }
    verifyNoInteractions(emailSender)
  }

  @Test
  fun `supplier assessment appointment attendance recorded event does not send email when notifyPP is false`() {
    notifyService().onApplicationEvent(appointmentEvent(AppointmentEventType.ATTENDANCE_RECORDED, false, AppointmentType.SERVICE_DELIVERY))
    verifyNoInteractions(emailSender)
  }

  @Test
  fun `delivery session appointment attendance recorded event does not send email when notifyPP is false`() {
    notifyService().onApplicationEvent(appointmentEvent(AppointmentEventType.ATTENDANCE_RECORDED, false, AppointmentType.SERVICE_DELIVERY, createDeliverySession(Attended.YES, false)))
    verifyNoInteractions(emailSender)
  }

  @Test
  fun `supplier assessment appointment attendance recorded event calls email client`() {
    whenever(referralService.getResponsibleProbationPractitioner(any())).thenReturn(ResponsibleProbationPractitioner("abc", "abc@abc.com", null, null, "def"))

    notifyService().onApplicationEvent(appointmentEvent(AppointmentEventType.ATTENDANCE_RECORDED, true))
    val personalisationCaptor = argumentCaptor<Map<String, String>>()
    verify(emailSender).sendEmail(eq("appointmentNotAttendedTemplate"), eq("abc@abc.com"), personalisationCaptor.capture())
    Assertions.assertThat(personalisationCaptor.firstValue["ppFirstName"]).isEqualTo("abc")
    Assertions.assertThat(personalisationCaptor.firstValue["popfullname"]).isEqualTo("Bob Green")
    Assertions.assertThat(personalisationCaptor.firstValue["attendanceUrl"]).isEqualTo("http://example.com/probation-practitioner/referrals/68df9f6c-3fcb-4ec6-8fcf-96551cd9b080/supplier-assessment/post-session-feedback")
  }

  @Test
  fun `delivery session appointment attendance recorded event calls email client`() {
    whenever(referralService.getResponsibleProbationPractitioner(any())).thenReturn(ResponsibleProbationPractitioner("abc", "abc@abc.com", null, null, "def"))

    val deliverySession = createDeliverySession(Attended.NO, true)
    val appointmentId = deliverySession.currentAppointment?.id

    notifyService().onApplicationEvent(appointmentEvent(AppointmentEventType.ATTENDANCE_RECORDED, true, AppointmentType.SERVICE_DELIVERY, deliverySession))
    val personalisationCaptor = argumentCaptor<Map<String, String>>()
    verify(emailSender).sendEmail(eq("appointmentNotAttendedTemplate"), eq("abc@abc.com"), personalisationCaptor.capture())
    Assertions.assertThat(personalisationCaptor.firstValue["ppFirstName"]).isEqualTo("abc")
    Assertions.assertThat(personalisationCaptor.firstValue["popfullname"]).isEqualTo("Bob Green")
    Assertions.assertThat(personalisationCaptor.firstValue["attendanceUrl"]).isEqualTo("http://example.com/probation-practitioner/referrals/68df9f6c-3fcb-4ec6-8fcf-96551cd9b080/session/1/appointment/$appointmentId/post-session-feedback")
  }

  @Test
  fun `supplier assessment appointment session feedback recorded event does not send email when user details are not available`() {
    whenever(referralService.getResponsibleProbationPractitioner(any())).thenThrow(RuntimeException::class.java)
    assertThrows<RuntimeException> {
      notifyService().onApplicationEvent(appointmentEvent(AppointmentEventType.SESSION_FEEDBACK_RECORDED, true))
    }
    verifyNoInteractions(emailSender)
  }

  @Test
  fun `delivery session appointment session feedback recorded event does not send email when user details are not available`() {
    whenever(referralService.getResponsibleProbationPractitioner(any())).thenThrow(RuntimeException::class.java)
    assertThrows<RuntimeException> {
      notifyService().onApplicationEvent(appointmentEvent(AppointmentEventType.SESSION_FEEDBACK_RECORDED, true, AppointmentType.SERVICE_DELIVERY, createDeliverySession(Attended.NO, true)))
    }
    verifyNoInteractions(emailSender)
  }

  @Test
  fun `supplier assessment appointment session feedback recorded event does not send email when notifyPP is false`() {
    notifyService().onApplicationEvent(appointmentEvent(AppointmentEventType.SESSION_FEEDBACK_RECORDED, false))
    verifyNoInteractions(emailSender)
  }

  @Test
  fun `delivery session appointment session feedback recorded event does not send email when notifyPP is false`() {
    notifyService().onApplicationEvent(appointmentEvent(AppointmentEventType.SESSION_FEEDBACK_RECORDED, false, AppointmentType.SERVICE_DELIVERY, createDeliverySession(Attended.YES, false)))
    verifyNoInteractions(emailSender)
  }

  @Test
  fun `supplier assessment appointment session feedback recorded event calls email client with concerningBehaviourTemplate when notifyProbationPractitionerOfConcerns is true`() {
    whenever(referralService.getResponsibleProbationPractitioner(any())).thenReturn(ResponsibleProbationPractitioner("abc", "abc@abc.com", null, null, "def"))

    notifyService().onApplicationEvent(
      appointmentEvent(
        AppointmentEventType.SESSION_FEEDBACK_RECORDED,
        true,
        notifyProbationPractitionerOfBehaviour = false,
        notifyProbationPractitionerOfConcerns = true,
      ),
    )
    val personalisationCaptor = argumentCaptor<Map<String, String>>()
    verify(emailSender).sendEmail(eq("concerningBehaviourTemplate"), eq("abc@abc.com"), personalisationCaptor.capture())
    verify(emailSender, never()).sendEmail(eq("poorBehaviourTemplate"), eq("abc@abc.com"), personalisationCaptor.capture())
    Assertions.assertThat(personalisationCaptor.firstValue["ppFirstName"]).isEqualTo("abc")
    Assertions.assertThat(personalisationCaptor.firstValue["popfullname"]).isEqualTo("Bob Green")
    Assertions.assertThat(personalisationCaptor.firstValue["sessionUrl"]).isEqualTo("http://example.com/probation-practitioner/referrals/68df9f6c-3fcb-4ec6-8fcf-96551cd9b080/supplier-assessment/post-session-feedback")
  }

  @Test
  fun `supplier assessment appointment session feedback recorded event calls email client with poorBehaviourTemplate when notifyProbationPractitionerOfBehaviour is true`() {
    whenever(referralService.getResponsibleProbationPractitioner(any())).thenReturn(ResponsibleProbationPractitioner("abc", "abc@abc.com", null, null, "def"))

    notifyService().onApplicationEvent(
      appointmentEvent(
        AppointmentEventType.SESSION_FEEDBACK_RECORDED,
        true,
        notifyProbationPractitionerOfBehaviour = true,
        notifyProbationPractitionerOfConcerns = false,
      ),
    )
    val personalisationCaptor = argumentCaptor<Map<String, String>>()
    verify(emailSender).sendEmail(eq("poorBehaviourTemplate"), eq("abc@abc.com"), personalisationCaptor.capture())
    verify(emailSender, never()).sendEmail(eq("concerningBehaviourTemplate"), eq("abc@abc.com"), personalisationCaptor.capture())
    Assertions.assertThat(personalisationCaptor.firstValue["ppFirstName"]).isEqualTo("abc")
    Assertions.assertThat(personalisationCaptor.firstValue["popfullname"]).isEqualTo("Bob Green")
    Assertions.assertThat(personalisationCaptor.firstValue["sessionUrl"]).isEqualTo("http://example.com/probation-practitioner/referrals/68df9f6c-3fcb-4ec6-8fcf-96551cd9b080/supplier-assessment/post-session-feedback")
  }

  @Test
  fun `supplier assessment appointment session feedback recorded event calls email client and sends both poorBehaviourTemplate and concerningBehaviourTemplate emails when notifyProbationPractitionerOfBehaviour and notifyProbationPractitionerOfConcerns is true`() {
    whenever(referralService.getResponsibleProbationPractitioner(any())).thenReturn(ResponsibleProbationPractitioner("abc", "abc@abc.com", null, null, "def"))

    notifyService().onApplicationEvent(
      appointmentEvent(
        AppointmentEventType.SESSION_FEEDBACK_RECORDED,
        true,
        notifyProbationPractitionerOfBehaviour = true,
        notifyProbationPractitionerOfConcerns = true,
      ),
    )
    val personalisationCaptor = argumentCaptor<Map<String, String>>()
    verify(emailSender).sendEmail(eq("poorBehaviourTemplate"), eq("abc@abc.com"), personalisationCaptor.capture())
    verify(emailSender).sendEmail(eq("concerningBehaviourTemplate"), eq("abc@abc.com"), personalisationCaptor.capture())
    Assertions.assertThat(personalisationCaptor.firstValue["ppFirstName"]).isEqualTo("abc")
    Assertions.assertThat(personalisationCaptor.firstValue["popfullname"]).isEqualTo("Bob Green")
    Assertions.assertThat(personalisationCaptor.firstValue["sessionUrl"]).isEqualTo("http://example.com/probation-practitioner/referrals/68df9f6c-3fcb-4ec6-8fcf-96551cd9b080/supplier-assessment/post-session-feedback")
  }

  @Test
  fun `delivery session appointment session feedback recorded event calls email client`() {
    whenever(referralService.getResponsibleProbationPractitioner(any())).thenReturn(ResponsibleProbationPractitioner("abc", "abc@abc.com", null, null, "def"))
    val deliverySession = createDeliverySession(Attended.NO, true)
    val appointmentId = deliverySession.currentAppointment?.id

    notifyService().onApplicationEvent(
      appointmentEvent(
        AppointmentEventType.SESSION_FEEDBACK_RECORDED,
        true,
        AppointmentType.SERVICE_DELIVERY,
        deliverySession,
        notifyProbationPractitionerOfBehaviour = false,
        notifyProbationPractitionerOfConcerns = true,
      ),
    )
    val personalisationCaptor = argumentCaptor<Map<String, String>>()
    verify(emailSender).sendEmail(eq("concerningBehaviourTemplate"), eq("abc@abc.com"), personalisationCaptor.capture())
    Assertions.assertThat(personalisationCaptor.firstValue["ppFirstName"]).isEqualTo("abc")
    Assertions.assertThat(personalisationCaptor.firstValue["popfullname"]).isEqualTo("Bob Green")
    Assertions.assertThat(personalisationCaptor.firstValue["sessionUrl"]).isEqualTo("http://example.com/probation-practitioner/referrals/68df9f6c-3fcb-4ec6-8fcf-96551cd9b080/session/1/appointment/$appointmentId/post-session-feedback")
  }

  @Test
  fun `appointment event does not send email for appointment event time service delivery`() {
    notifyService().onApplicationEvent(appointmentEvent(AppointmentEventType.SESSION_FEEDBACK_RECORDED, false, AppointmentType.SERVICE_DELIVERY))
    verifyNoInteractions(emailSender)
  }

  @Test
  fun `appointment scheduled event sends email to pp`() {
    whenever(referralService.getResponsibleProbationPractitioner(any())).thenReturn(ResponsibleProbationPractitioner("abc", "abc@abc.com", null, null, "def"))

    notifyService().onApplicationEvent(appointmentEvent(AppointmentEventType.SCHEDULED, true))
    val personalisationCaptor = argumentCaptor<Map<String, String>>()
    verify(emailSender).sendEmail(eq("initialAssessmentScheduledTemplate"), eq("abc@abc.com"), personalisationCaptor.capture())
    Assertions.assertThat(personalisationCaptor.firstValue["ppFirstName"]).isEqualTo("abc")
    Assertions.assertThat(personalisationCaptor.firstValue["referenceNumber"]).isEqualTo("JS18726AC")
    Assertions.assertThat(personalisationCaptor.firstValue["referralUrl"]).isEqualTo("http://example.com/probation-practitioner/referrals/68df9f6c-3fcb-4ec6-8fcf-96551cd9b080/progress")
  }
}
