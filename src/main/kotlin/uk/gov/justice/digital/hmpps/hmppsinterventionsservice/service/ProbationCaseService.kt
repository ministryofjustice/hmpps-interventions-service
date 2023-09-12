package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.component.ActionPlanValidator
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.config.ValidationError
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.ProbationCaseReferralDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.ActionPlanEventPublisher
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ActionPlan
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ActionPlanActivity
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Appointment
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Attended
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AuthUser
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Referral
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ActionPlanRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.AuthUserRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.DeliverySessionRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ReferralRepository
import java.time.OffsetDateTime
import java.util.UUID
import java.util.UUID.randomUUID
import javax.persistence.EntityNotFoundException

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
          referringOfficerDetails
        )
      )
    }
    return probationCaseDetails
  }
}
