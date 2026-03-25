package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto

import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.DraftReferral
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.DynamicFrameworkContract
import java.time.LocalDate
import java.time.OffsetDateTime

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
