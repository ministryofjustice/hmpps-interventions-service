package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.component.RamDeliusClient
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.AppointmentEvent
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.AppointmentEventType.SESSION_FEEDBACK_RECORDED
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AppointmentType.SUPPLIER_ASSESSMENT
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Attended.LATE
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Attended.NO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Attended.YES
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.SampleData
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.AppointmentFactory
import java.time.OffsetDateTime
import java.util.UUID

class CommunityAPIAppointmentEventServiceTest {

  private val deliusRamClient = mock<RamDeliusClient>()

  private val appointmentFactory = AppointmentFactory()

  private val communityAPIService = CommunityAPIAppointmentEventService(
    "http://baseUrl",
    "/probation-practitioner/referrals/{id}/supplier-assessment/post-session-feedback",
    true,
    "/probation-case/{crn}/referrals/{referralId}/appointments",
    deliusRamClient,
  )

  @Test
  fun `notifies community-api of late attended appointment outcome`() {
    appointmentEvent.appointment.attended = LATE
    appointmentEvent.appointment.notifyPPOfAttendanceBehaviour = false

    communityAPIService.onApplicationEvent(appointmentEvent)

    verifyNotification(LATE.name, false)
  }

  @Test
  fun `notifies community-api of attended appointment outcome`() {
    appointmentEvent.appointment.attended = YES
    appointmentEvent.appointment.notifyPPOfAttendanceBehaviour = false

    communityAPIService.onApplicationEvent(appointmentEvent)

    verifyNotification(YES.name, false)
  }

  @Test
  fun `notifies community-api of non attended appointment outcome and notify PP set is always set`() {
    appointmentEvent.appointment.attended = NO
    appointmentEvent.appointment.notifyPPOfAttendanceBehaviour = false

    communityAPIService.onApplicationEvent(appointmentEvent)

    verifyNotification(NO.name, true)
  }

  @Test
  fun `notifies community-api of appointment outcome with notify PP set`() {
    appointmentEvent.appointment.attended = YES
    appointmentEvent.appointment.notifyPPOfAttendanceBehaviour = true

    communityAPIService.onApplicationEvent(appointmentEvent)

    verifyNotification(YES.name, true)
  }

  @Test
  fun `does not notify when not enabled`() {
    val communityAPIService = CommunityAPIAppointmentEventService(
      "http://baseUrl",
      "/probation-practitioner/referrals/{id}/supplier-assessment/post-session-feedback",
      false,
      "/probation-case/{crn}/referrals/{referralId}/appointments",
      deliusRamClient,
    )

    appointmentEvent.appointment.attended = YES
    appointmentEvent.appointment.notifyPPOfAttendanceBehaviour = true

    communityAPIService.onApplicationEvent(appointmentEvent)

    verifyNoInteractions(deliusRamClient)
  }

  private fun verifyNotification(attended: String, notifyPP: Boolean) {
    val urlCaptor = argumentCaptor<String>()
    val payloadCaptor = argumentCaptor<AppointmentMerge>()
    verify(deliusRamClient).makePutAppointmentRequest(urlCaptor.capture(), payloadCaptor.capture())

    val ma = payloadCaptor.firstValue
    assertThat(urlCaptor.firstValue).isEqualTo("/probation-case/CRN123/referrals/356d0712-5266-4d18-9070-058244873f2c/appointments")
    assertThat(ma.notes).isEqualTo(
      "Session Feedback Recorded for Accommodation Referral X123456 with Prime Provider TOP Service Provider\n" +
        "http://baseUrl/probation-practitioner/referrals/356d0712-5266-4d18-9070-058244873f2c/supplier-assessment/post-session-feedback",
    )
    assertThat(ma.outcome?.attended?.name).isEqualTo(attended.uppercase())
    assertThat(ma.outcome?.notify).isEqualTo(notifyPP)
  }

  private val appointmentEvent = AppointmentEvent(
    "source",
    SESSION_FEEDBACK_RECORDED,
    appointmentFactory.create(
      id = UUID.fromString("68df9f6c-3fcb-4ec6-8fcf-96551cd9b080"),
      referral = SampleData.sampleReferral("CRN123", "TOP Service Provider", UUID.fromString("356d0712-5266-4d18-9070-058244873f2c"), "X123456"),
      appointmentTime = OffsetDateTime.now(),
      durationInMinutes = 60,
      createdBy = SampleData.sampleAuthUser("userId", "auth", "me"),
      additionalAttendanceInformation = "dded notes",
      attendanceSubmittedAt = OffsetDateTime.now(),
      deliusAppointmentId = 123456L,
    ),
    "http://localhost:8080/url/68df9f6c-3fcb-4ec6-8fcf-96551cd9b080",
    true,
    SUPPLIER_ASSESSMENT,
  )
}
