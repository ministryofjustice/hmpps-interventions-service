package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.ProbationCaseReferralDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ReferralRepository

@Service
@Transactional
class ProbationCaseService(
  val referralRepository: ReferralRepository,
  val referralService: ReferralService,
  val hmppsAuthService: HMPPSAuthService,
) {

  fun getProbationCaseDetails(
    crn: String,
  ): List<ProbationCaseReferralDTO> {
    val referrals = referralRepository.findByServiceUserCRN(crn)
    val probationCaseDetails: MutableList<ProbationCaseReferralDTO> = mutableListOf()

    referrals.forEach { referral ->
      val responsibleOfficer = referralService.getResponsibleProbationPractitioner(referral)
      val referringOfficerDetails = hmppsAuthService.getUserDetail(referral.createdBy)
      val serviceProviderUser = referral.currentAssignee?.let { hmppsAuthService.getUserDetail(it) }
      probationCaseDetails.add(
        ProbationCaseReferralDTO.from(
          referral,
          responsibleOfficer,
          serviceProviderUser,
          referringOfficerDetails,
        ),
      )
    }
    return probationCaseDetails
  }
}
