package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.ProbationCaseReferralDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Appointment
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Referral
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.DraftReferralRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ReferralRepository

@Service
@Transactional
class ProbationCaseService(
  val referralRepository: ReferralRepository,
  val draftReferralRepository: DraftReferralRepository,
  val referralService: ReferralService,
  val hmppsAuthService: HMPPSAuthService,
  val deliverySessionService: DeliverySessionService,
) {

  fun getProbationCaseDetails(
    crn: String,
  ): List<ProbationCaseReferralDTO> {
    val sentReferrals = referralRepository.findByServiceUserCRN(crn)
    val draftReferrals = draftReferralRepository.findByServiceUserCRN(crn)
    val probationCaseDetails: MutableList<ProbationCaseReferralDTO> = mutableListOf()

    sentReferrals.forEach { referral ->
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

    draftReferrals.forEach { referral ->
      val referringOfficerDetails = hmppsAuthService.getUserDetail(referral.createdBy)
      probationCaseDetails.add(
        ProbationCaseReferralDTO.from(
          referral,
          referringOfficerDetails,
        ),
      )
    }
    return probationCaseDetails
  }

  fun getAppointmentLocationDetails(crn: String): List<ReferralAppointmentLocationDetails> {
    val sentReferrals = referralRepository.findByServiceUserCRN(crn)
    return sentReferrals.map {
      val deliverySessions = deliverySessionService.getSessions(it.id)
      val appointments = deliverySessions.flatMap { ds -> ds.appointments }
      ReferralAppointmentLocationDetails(it, appointments)
    }
  }
}

data class ReferralAppointmentLocationDetails(
  val referral: Referral,
  val appointments: List<Appointment>,
)
