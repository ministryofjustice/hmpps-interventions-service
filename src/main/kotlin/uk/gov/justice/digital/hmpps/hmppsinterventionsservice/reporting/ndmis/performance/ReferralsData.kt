package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.reporting.ndmis.performance

import java.util.UUID

data class ReferralsData(
  val referralReference: String,
  val referralId: UUID,
  val contractReference: String,
  val contractType: String,
  val primeProvider: String,
  val referringOfficerId: String,
  val relevantSentanceId: Long,
  val serviceUserCRN: String,
  val dateReferralReceived: NdmisDateTime,
  val firstActionPlanSubmittedAt: NdmisDateTime?,
  val firstActionPlanApprovedAt: NdmisDateTime?,
  val numberOfOutcomes: Int?,
  val achievementScore: Float?,
  val numberOfSessions: Int?,
  val numberOfSessionsAttended: Int?,
  val endRequestedAt: NdmisDateTime?,
  val interventionEndReason: String?,
  val eosrSubmittedAt: NdmisDateTime?,
  val endReasonCode: String?,
  val endReasonDescription: String?,
  val concludedAt: NdmisDateTime?,
) {
  companion object {
    val fields = listOf(
      "referralReference",
      "referralId",
      "contractReference",
      "contractType",
      "primeProvider",
      "referringOfficerId",
      "relevantSentanceId",
      "serviceUserCRN",
      "dateReferralReceived",
      "firstActionPlanSubmittedAt",
      "firstActionPlanApprovedAt",
      "numberOfOutcomes",
      "achievementScore",
      "numberOfSessions",
      "numberOfSessionsAttended",
      "endRequestedAt",
      "interventionEndReason",
      "eosrSubmittedAt",
      "endReasonCode",
      "endReasonDescription",
      "concludedAt"
    )
    val headers = listOf(
      "referral_ref",
      "referral_id",
      "crs_contract_reference",
      "crs_contract_type",
      "crs_provider_id",
      "referring_officer_id",
      "relevant_sentence_id",
      "service_user_crn",
      "date_referral_received",
      "date_first_action_plan_submitted",
      "date_of_first_action_plan_approval",
      "outcomes_to_be_achieved_count",
      "outcomes_progress",
      "count_of_sessions_expected",
      "count_of_sessions_attended",
      "date_intervention_ended",
      "intervention_end_reason",
      "date_end_of_service_report_submitted",
      "intervention_end_reason_code",
      "intervention_end_reason_description",
      "intervention_concluded_at"
    )
  }
}
