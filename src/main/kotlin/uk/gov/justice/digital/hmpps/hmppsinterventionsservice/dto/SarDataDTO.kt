package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto

import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Appointment
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Referral
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.SarsReferralData

class SarDataDTO(
  val content: SarsRandmDto,
) {
  companion object {
    fun from(crn: String, sarsReferralData: List<SarsReferralData>): SarDataDTO = SarDataDTO(
      content = SarsRandmDto.from(crn, sarsReferralData),
    )
  }
}

class SarsRandmDto(
  val crn: String,
  val referral: List<SarsReferralDTO>,
) {
  companion object {
    fun from(crn: String, sarsReferralData: List<SarsReferralData>): SarsRandmDto = SarsRandmDto(
      crn = crn,
      referral = sarsReferralData.map { SarsReferralDTO.from(it.referral, it.appointments) },
    )
  }
}

class SarsReferralDTO(
  val referral_number: String?,
  val accessibility_needs: String,
  val additional_needs_information: String,
  val when_unavailable: String,
  val end_requested_comments: String,
  val appointment: List<SarsAppointmentDTO>,
  val action_plan_activity: List<SarsActionPlanActivityDTO>?,
  val end_of_service_report: SarsEndOfServiceReportDTO?,
) {
  companion object {
    fun from(referral: Referral, appointments: List<Appointment>): SarsReferralDTO = SarsReferralDTO(
      referral_number = referral.referenceNumber,
      accessibility_needs = referral.accessibilityNeeds ?: "",
      additional_needs_information = referral.additionalNeedsInformation ?: "",
      when_unavailable = referral.whenUnavailable ?: "",
      end_requested_comments = referral.endRequestedComments ?: "",
      appointment = appointments.map { SarsAppointmentDTO.from(it) },
      action_plan_activity = referral.actionPlans?.map { SarsActionPlanActivityDTO(it.activities.map { actionPlanActivity -> actionPlanActivity.description }) },
      end_of_service_report = referral.endOfServiceReport?.let { SarsEndOfServiceReportDTO(it.outcomes.map { outcome -> SarsEndOfServiceReportOutcomesDTO(outcome.progressionComments ?: "", outcome.additionalTaskComments ?: "") }) },
    )
  }
}

class SarsAppointmentDTO(
  val session_summary: String,
  val session_response: String,
  val session_concerns: String,
  val late_reason: String,
  val future_session_plan: String,
) {
  companion object {
    fun from(appointment: Appointment): SarsAppointmentDTO = SarsAppointmentDTO(
      session_summary = appointment.sessionSummary ?: "",
      session_response = appointment.sessionResponse ?: "",
      session_concerns = appointment.sessionConcerns ?: "",
      late_reason = appointment.lateReason ?: "",
      future_session_plan = appointment.futureSessionPlans ?: "",
    )
  }
}

class SarsActionPlanActivityDTO(
  val description: List<String>?,
)

class SarsEndOfServiceReportOutcomesDTO(
  val progression_comments: String,
  val additional_task_comments: String,
)

class SarsEndOfServiceReportDTO(
  val end_of_service_outcomes: List<SarsEndOfServiceReportOutcomesDTO>,

)
