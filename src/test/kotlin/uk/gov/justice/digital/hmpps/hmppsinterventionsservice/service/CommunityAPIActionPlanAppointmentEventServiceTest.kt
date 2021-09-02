package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service

import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.component.CommunityAPIClient
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.ActionPlanAppointmentEvent
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.ActionPlanAppointmentEventType.SESSION_FEEDBACK_RECORDED
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Attended.LATE
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.SampleData
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ActionPlanFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ActionPlanSessionFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ReferralFactory
import java.time.OffsetDateTime
import java.util.UUID

class CommunityAPIActionPlanAppointmentEventServiceTest {

  private val communityAPIClient = mock<CommunityAPIClient>()

  private val actionPlanSessionFactory = ActionPlanSessionFactory()
  private val referralFactory = ReferralFactory()
  private val actionPlanFactory = ActionPlanFactory()

  @Test
  fun `got service successfully`() {

    val communityAPIService = CommunityAPIActionPlanAppointmentEventService(
      "http://baseUrl",
      "/probation-practitioner/referrals/{id}/appointment/{sessionNumber}/post-session-feedback",
      "/secure/offenders/crn/{crn}/appointments/{appointmentId}/outcome/context/{contextName}",
      "commissioned-rehabilitation-services",
      communityAPIClient
    )
    appointmentEvent.actionPlanSession.referral.referenceNumber = "X123456"

    communityAPIService.onApplicationEvent(appointmentEvent)

    val urlCaptor = argumentCaptor<String>()
    val payloadCaptor = argumentCaptor<Any>()
    val referralId = appointmentEvent.actionPlanSession.referral.id
    verify(communityAPIClient).makeAsyncPostRequest(urlCaptor.capture(), payloadCaptor.capture())
    assertThat(urlCaptor.firstValue).isEqualTo("/secure/offenders/crn/CRN123/appointments/123456/outcome/context/commissioned-rehabilitation-services")
    assertThat(payloadCaptor.firstValue.toString()).isEqualTo(
      AppointmentOutcomeRequest(
        "Session Feedback Recorded for Accommodation Referral X123456 with Prime Provider Harmony Living\n" +
          "http://baseUrl/probation-practitioner/referrals/$referralId/appointment/1/post-session-feedback",
        "LATE",
        true
      ).toString()
    )
  }

  private val appointmentEvent = ActionPlanAppointmentEvent(
    "source",
    SESSION_FEEDBACK_RECORDED,
    actionPlanSessionFactory.createAttended(
      id = UUID.fromString("68df9f6c-3fcb-4ec6-8fcf-96551cd9b080"),
      referral = referralFactory.createSent(serviceUserCRN = "CRN123", actionPlans = mutableListOf(actionPlanFactory.create())),
      sessionNumber = 1,
      appointmentTime = OffsetDateTime.now(),
      durationInMinutes = 60,
      createdBy = SampleData.sampleAuthUser("userId", "auth", "me"),
      attended = LATE,
      additionalAttendanceInformation = "dded notes",
      attendanceSubmittedAt = OffsetDateTime.now(),
      notifyPPOfAttendanceBehaviour = true,
      deliusAppointmentId = 123456L
    ),
    "http://localhost:8080/url/68df9f6c-3fcb-4ec6-8fcf-96551cd9b080",
    true
  )
}
