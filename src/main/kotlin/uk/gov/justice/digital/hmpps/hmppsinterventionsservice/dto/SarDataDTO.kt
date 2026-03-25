package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto

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
