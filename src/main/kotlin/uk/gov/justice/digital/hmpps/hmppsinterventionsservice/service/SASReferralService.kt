package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.SASReferralDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.SASReferralStatus
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.DraftReferral
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Referral
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.DraftReferralRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ReferralRepository

@Service
class SASReferralService(
  val referralRepository: ReferralRepository,
  val draftReferralRepository: DraftReferralRepository,
) {

  companion object {
    const val ACCOMMODATION_CATEGORY_NAME = "Accommodation"
  }

  fun getReferralsByCrn(crn: String): List<SASReferralDTO> {
    val results = mutableListOf<SASReferralDTO>()

    referralRepository.findByServiceUserCRNForSAS(crn)
      .filter { it.hasAccommodationServiceCategory() }
      .forEach { referral ->
        results.add(SASReferralDTO.from(referral, determineStatus(referral)))
      }

    draftReferralRepository.findDraftOnlyByServiceUserCRNForSAS(crn)
      .filter { it.hasAccommodationServiceCategory() }
      .forEach { draftReferral ->
        results.add(SASReferralDTO.fromDraft(draftReferral))
      }

    return results
  }

  private fun determineStatus(referral: Referral): SASReferralStatus = when {
    referral.withdrawalReasonCode != null && referral.withdrawalComments != null -> SASReferralStatus.WITHDRAWN
    referral.concludedAt != null && referral.endOfServiceReport != null -> SASReferralStatus.COMPLETED
    else -> SASReferralStatus.LIVE
  }

  private fun Referral.hasAccommodationServiceCategory(): Boolean = selectedServiceCategories?.any { it.name == ACCOMMODATION_CATEGORY_NAME } == true

  private fun DraftReferral.hasAccommodationServiceCategory(): Boolean = selectedServiceCategories?.any { it.name == ACCOMMODATION_CATEGORY_NAME } == true
}
