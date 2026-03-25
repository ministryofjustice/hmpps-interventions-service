package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto

import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ActionPlan
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Appointment
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Attended
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.CaseNote
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.DraftReferral
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.DynamicFrameworkContract
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Referral
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.SarsReferralData
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

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
  val draft_referral: List<SarsDraftReferralDTO>,
) {
  companion object {
    fun from(crn: String, sarsReferralData: List<SarsReferralData>): SarsRandmDto = SarsRandmDto(
      crn = crn,
      referral = sarsReferralData.mapNotNull { data ->
        data.referral?.let { referral ->
          SarsReferralDTO.from(
            referral,
            data.appointments,
            data.dynamicFrameworkContract!!,
            data.caseNotes,
          )
        }
      },
      draft_referral = sarsReferralData
        .flatMap { it.draftReferrals }
        .distinctBy { it.id }
        .map { draftReferral ->
          SarsDraftReferralDTO.from(draftReferral, draftReferral.intervention.dynamicFrameworkContract)
        },
    )
  }
}

class SarsReferralDTO(
  val referral_number: String?,
  val created_at: OffsetDateTime?,
  val has_additional_responsibilities: Boolean? = null,
  val service_user_crn: String? = null,
  val sent_at: OffsetDateTime,
  val end_requested_at: OffsetDateTime? = null,
  val concluded_at: OffsetDateTime? = null,
  val draft_supplementary_risk_updated_at: OffsetDateTime? = null,
  val withdrawal_comments: String? = null,
  val status: String? = null,
  val assigned_at: OffsetDateTime? = null,
  val accessibility_needs: String,
  val additional_needs_information: String,
  val when_unavailable: String,
  val end_requested_comments: String,
  val interpreting_language: String? = null,
  val needs_interpreter: Boolean = false,
  val relevant_sentence_end_date: LocalDate? = null,
  val appointment: List<SarsAppointmentDTO>,
  val action_plans: List<SarsActionPlansDto>?,
  val contract: SarsContractDTO?,
  val end_of_service_report: SarsEndOfServiceReportDTO?,
  val cancellation_reason: String? = null,
  val case_notes: List<SarsCaseNotesesDTO>? = null,
  val probation_practitioner_details: SarsProbationPractitionerDetailsDTO? = null,
  val referral_details: SarsReferralDetailsDTO? = null,
  val referal_location: SarsReferralLocationDTO? = null,
  val service_provider_name: String? = null,
) {
  companion object {
    fun from(
      referral: Referral,
      appointments: List<Appointment>,
      dynamicFrameworkContract: DynamicFrameworkContract,
      caseNotes: List<CaseNote>,
    ): SarsReferralDTO = SarsReferralDTO(
      referral_number = referral.referenceNumber,
      created_at = referral.createdAt,
      has_additional_responsibilities = referral.hasAdditionalResponsibilities,
      service_user_crn = referral.serviceUserCRN,
      sent_at = referral.sentAt ?: referral.createdAt,
      end_requested_at = referral.endRequestedAt,
      concluded_at = referral.concludedAt,
      draft_supplementary_risk_updated_at = referral.additionalRiskInformationUpdatedAt,
      withdrawal_comments = referral.withdrawalComments,
      status = referral.status?.name,
      assigned_at = referral.assignments.maxByOrNull { it.assignedAt }?.assignedAt,
      accessibility_needs = referral.accessibilityNeeds ?: "",
      additional_needs_information = referral.additionalNeedsInformation ?: "",
      when_unavailable = referral.whenUnavailable ?: "",
      end_requested_comments = referral.endRequestedComments ?: "",
      needs_interpreter = referral.needsInterpreter ?: false,
      interpreting_language = referral.interpreterLanguage,
      relevant_sentence_end_date = referral.relevantSentenceEndDate,
      appointment = appointments.map { SarsAppointmentDTO.from(it) },
      action_plans = referral.actionPlans?.map { SarsActionPlansDto.from(it) },
      contract = SarsContractDTO.from(dynamicFrameworkContract),
      end_of_service_report = referral.endOfServiceReport?.let {
        SarsEndOfServiceReportDTO(
          created_at = it.createdAt,
          submitted_at = it.submittedAt,
          further_information = it.furtherInformation,
          submitted_by = it.submittedBy?.userName,
          referral_id = it.referral.id,
          end_of_service_outcomes = it.outcomes.map { outcome ->
            SarsEndOfServiceReportOutcomesDTO(
              outcome.progressionComments ?: "",
              outcome.additionalTaskComments ?: "",
              outcome.achievementLevel.name,
            )
          },
        )
      },
      cancellation_reason = referral.endRequestedReason?.description,
      case_notes = caseNotes.map { SarsCaseNotesesDTO.from(it) },
      probation_practitioner_details = referral.probationPractitionerDetails?.let {
        SarsProbationPractitionerDetailsDTO(
          it.name,
          it.pdu,
          it.probationOffice,
          it.roleOrJobTitle,
          it.establishment,
        )
      },
      referral_details = SarsReferralDetailsDTO(
        created_at = referral.createdAt,
        reason_for_change = referral.referralDetails?.reasonForChange,
        completion_deadline = referral.referralDetails?.completionDeadline,
        further_information = referral.referralDetails?.furtherInformation,
        maximum_enforceable_days = referral.referralDetails?.maximumEnforceableDays,
        reason_for_referral = referral.referralDetails?.reasonForReferral,
        reason_for_referral_before_allocation = referral.referralDetails?.reasonForReferralCreationBeforeAllocation,
        reason_for_referral_further_information = referral.referralDetails?.reasonForReferralFurtherInformation,
      ),
      referal_location = SarsReferralLocationDTO(
        referal_id = referral.id,
        type = referral.referralLocation?.type?.name,
        prison_id = referral.referralLocation?.prisonId,
        nomis_prison_id = referral.referralLocation?.nomisPrisonId,
        referral_releasing_12_weeks = referral.referralLocation?.isReferralReleasingIn12Weeks,
        expected_probation_office = referral.referralLocation?.expectedProbationOffice,
        expected_probation_office_unknown_reason = referral.referralLocation?.expectedProbationOfficeUnknownReason,
      ),
      service_provider_name = if (dynamicFrameworkContract.subcontractorProviders.isNotEmpty()) dynamicFrameworkContract.primeProvider.name + " " + dynamicFrameworkContract.subcontractorProviders.joinToString(separator = ", ") { it.name } else dynamicFrameworkContract.primeProvider.name,
    )
  }
}

class SarsDraftReferralDTO(
  val created_at: OffsetDateTime?,
  val accessibility_needs: String,
  val additional_needs_information: String,
  val when_unavailable: String,
  val interpreting_language: String? = null,
  val needs_interpreter: Boolean = false,
  val has_additional_responsibilities: Boolean? = null,
  val draft_supplementary_risk: String? = null,
  val draft_supplementary_risk_updated_at: OffsetDateTime? = null,
  val person_current_location_type: String? = null,
  val person_expected_release_date: LocalDate? = null,
  val person_expected_release_date_missing_reason: String? = null,
  val referral_releasing_12_weeks: Boolean? = null,
  val role_job_title: String? = null,
  val service_user_crn: String? = null,
  val expected_probation_office: String? = null,
  val expected_probation_office_unknown_reason: String? = null,
  val contract: SarsContractDTO?,
) {
  companion object {
    fun from(
      referral: DraftReferral,
      dynamicFrameworkContract: DynamicFrameworkContract,
    ): SarsDraftReferralDTO = SarsDraftReferralDTO(
      accessibility_needs = referral.accessibilityNeeds ?: "",
      additional_needs_information = referral.additionalNeedsInformation ?: "",
      when_unavailable = referral.whenUnavailable ?: "",
      needs_interpreter = referral.needsInterpreter ?: false,
      interpreting_language = referral.interpreterLanguage,
      created_at = referral.createdAt,
      has_additional_responsibilities = referral.hasAdditionalResponsibilities,
      draft_supplementary_risk = referral.additionalRiskInformation,
      draft_supplementary_risk_updated_at = referral.additionalRiskInformationUpdatedAt,
      person_current_location_type = referral.personCurrentLocationType?.name,
      person_expected_release_date = referral.expectedReleaseDate,
      person_expected_release_date_missing_reason = referral.expectedReleaseDateMissingReason,
      referral_releasing_12_weeks = referral.isReferralReleasingIn12Weeks,
      role_job_title = referral.roleOrJobTitle,
      service_user_crn = referral.serviceUserCRN,
      expected_probation_office = referral.expectedProbationOffice,
      expected_probation_office_unknown_reason = referral.expectedProbationOfficeUnknownReason,
      contract = SarsContractDTO.from(dynamicFrameworkContract),
    )
  }
}

class SarsAppointmentDTO(
  val id: UUID,
  val duration_minutes: Int? = null,
  val created_at: OffsetDateTime? = null,
  val attendance_submitted_at: OffsetDateTime? = null,
  val notifyppof_attendance_behaviour: Boolean? = null,
  val attendance_behaviour_submitted_at: OffsetDateTime? = null,
  val appointment_feedback_submitted_at: OffsetDateTime? = null,
  val superseded: Boolean? = null,
  val superseded_by_appointment_id: UUID? = null,
  val session_feedback_submitted_at: OffsetDateTime? = null,
  val session_feedback_submitted_by: String? = null,
  val did_session_happen: Boolean? = null,
  val late: Boolean? = null,
  val future_session_plans: String? = null,
  val rescheduled_reasons: String? = null,
  val session_summary: String,
  val session_response: String,
  val session_concerns: String,
  val late_reason: String,
  val future_session_plan: String,
  val attended: Attended? = null,
  val additional_attendance_information: String? = null,
  val attendance_behaviour: String? = null,
  val attendance_failure_information: String? = null,
  val no_attendance_information: String? = null,
  val no_session_reason_type: String? = null,
  val no_session_reason_pop_acceptable: String? = null,
  val no_session_reason_pop_unacceptable: String? = null,
  val no_session_reason_logistics: String? = null,
  val session_behaviour: String? = null,
  val appointment_delivery_address: SarsAppointmentDeliveryAddress? = null,
  val appointment_delivery_type: SarsAppointmentDeliveryTypeDto? = null,
) {
  companion object {
    fun from(appointment: Appointment): SarsAppointmentDTO = SarsAppointmentDTO(
      id = appointment.id,
      duration_minutes = appointment.durationInMinutes,
      created_at = appointment.createdAt,
      attendance_submitted_at = appointment.attendanceSubmittedAt,
      notifyppof_attendance_behaviour = appointment.notifyPPOfAttendanceBehaviour,
      attendance_behaviour_submitted_at = appointment.attendanceBehaviourSubmittedAt,
      appointment_feedback_submitted_at = appointment.appointmentFeedbackSubmittedAt,
      superseded = appointment.superseded,
      superseded_by_appointment_id = appointment.supersededByAppointmentId,
      session_feedback_submitted_at = appointment.sessionFeedbackSubmittedAt,
      session_feedback_submitted_by = appointment.sessionFeedbackSubmittedBy?.userName,
      did_session_happen = appointment.didSessionHappen,
      late = appointment.late,
      rescheduled_reasons = appointment.rescheduledReason,
      session_summary = appointment.sessionSummary ?: "",
      session_response = appointment.sessionResponse ?: "",
      session_concerns = appointment.sessionConcerns ?: "",
      late_reason = appointment.lateReason ?: "",
      future_session_plan = appointment.futureSessionPlans ?: "",
      attended = appointment.attended,
      additional_attendance_information = appointment.additionalAttendanceInformation,
      attendance_behaviour = appointment.attendanceBehaviour,
      attendance_failure_information = appointment.attendanceFailureInformation,
      no_attendance_information = appointment.noAttendanceInformation,
      no_session_reason_type = appointment.noSessionReasonType?.name,
      no_session_reason_pop_acceptable = appointment.noSessionReasonPopAcceptable,
      no_session_reason_pop_unacceptable = appointment.noSessionReasonPopUnacceptable,
      no_session_reason_logistics = appointment.noSessionReasonLogistics,
      session_behaviour = appointment.sessionBehaviour,
      appointment_delivery_address = SarsAppointmentDeliveryAddress.from(appointment),
      appointment_delivery_type = SarsAppointmentDeliveryTypeDto.from(appointment),
    )
  }
}

class SarsAppointmentDeliveryAddress(
  val first_address_line: String,
  val second_address_line: String?,
  val town_or_city: String,
  val postcode: String,
) {
  companion object {

    fun from(appointment: Appointment): SarsAppointmentDeliveryAddress = SarsAppointmentDeliveryAddress(
      first_address_line = appointment.appointmentDelivery?.appointmentDeliveryAddress?.firstAddressLine ?: "",
      second_address_line = appointment.appointmentDelivery?.appointmentDeliveryAddress?.secondAddressLine,
      town_or_city = appointment.appointmentDelivery?.appointmentDeliveryAddress?.townCity ?: "",
      postcode = appointment.appointmentDelivery?.appointmentDeliveryAddress?.postCode ?: "",
    )
  }
}

class SarsAppointmentDeliveryTypeDto(
  val appointment_delivery_type: String,
  val nps_office_code: String? = null,
  val appointment_session_type: String? = null,
) {
  companion object {
    fun from(appointment: Appointment): SarsAppointmentDeliveryTypeDto = SarsAppointmentDeliveryTypeDto(
      appointment_delivery_type = appointment.appointmentDelivery?.appointmentDeliveryType?.name ?: "",
      nps_office_code = appointment.appointmentDelivery?.npsOfficeCode,
      appointment_session_type = appointment.appointmentDelivery?.appointmentSessionType?.name,
    )
  }
}

class SarsActionPlanActivityDTO(
  val description: String?,
  val createdAt: OffsetDateTime?,
)

class SarsActionPlansDto(
  val number_of_sessions: Int?,
  val created_at: OffsetDateTime?,
  val approved_by: String?,
  val action_plan_activities: List<SarsActionPlanActivityDTO>,
) {
  companion object {

    fun from(actionPlan: ActionPlan) = SarsActionPlansDto(
      number_of_sessions = actionPlan.numberOfSessions,
      created_at = actionPlan.createdAt,
      approved_by = actionPlan.approvedBy?.userName,
      action_plan_activities = actionPlan.activities.map { SarsActionPlanActivityDTO(it.description, it.createdAt) },
    )
  }
}

class SarsEndOfServiceReportOutcomesDTO(
  val progression_comments: String,
  val additional_task_comments: String,
  val acheivement_level: String? = null,
)

class SarsContractDTO(
  val nps_region_id: String? = null,
  val pcc_region_id: String? = null,
  val referral_start_date: LocalDate? = null,
  val referral_end_date: LocalDate? = null,
) {
  companion object {
    fun from(dynamicFrameworkContract: DynamicFrameworkContract): SarsContractDTO = SarsContractDTO(
      nps_region_id = dynamicFrameworkContract.npsRegion?.name,
      pcc_region_id = dynamicFrameworkContract.pccRegion?.name,
      referral_start_date = dynamicFrameworkContract.referralStartDate,
      referral_end_date = dynamicFrameworkContract.referralEndAt?.toLocalDate(),
    )
  }
}

class SarsCaseNotesesDTO(
  val subject: String? = null,
  val body: String? = null,
  val sent_at: OffsetDateTime? = null,
) {
  companion object {
    fun from(caseNote: CaseNote) = SarsCaseNotesesDTO(
      subject = caseNote.subject,
      body = caseNote.body,
      sent_at = caseNote.sentAt,
    )
  }
}

class SarsEndOfServiceReportDTO(
  val end_of_service_outcomes: List<SarsEndOfServiceReportOutcomesDTO>,
  val created_at: OffsetDateTime? = null,
  val submitted_at: OffsetDateTime? = null,
  val further_information: String? = null,
  val submitted_by: String? = null,
  val referral_id: UUID? = null,
)

class SarsProbationPractitionerDetailsDTO(
  val name: String? = null,
  val pdu: String? = null,
  val probation_office: String? = null,
  val role_job_title: String? = null,
  val establishment: String? = null,
)

class SarsReferralDetailsDTO(
  val created_at: OffsetDateTime?,
  val reason_for_change: String?,
  val completion_deadline: LocalDate?,
  val further_information: String?,
  val maximum_enforceable_days: Int?,
  val reason_for_referral: String?,
  val reason_for_referral_before_allocation: String?,
  val reason_for_referral_further_information: String?,
)

class SarsReferralLocationDTO(
  val referal_id: UUID,
  val type: String? = null,
  val prison_id: String? = null,
  val nomis_prison_id: String? = null,
  val referral_releasing_12_weeks: Boolean? = null,
  val expected_probation_office: String? = null,
  val expected_probation_office_unknown_reason: String? = null,
)
