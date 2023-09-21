package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto

import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.DraftReferral
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Referral
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.ResponsibleProbationPractitioner
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.UserDetail
import java.time.OffsetDateTime

data class ProbationCaseReferralDTO(
  val serviceCategories: List<String>?,
  val contractType: String,
  val referralCreatedAt: OffsetDateTime?,
  val referralSentAt: OffsetDateTime?,
  val interventionTitle: String,
  val referringOfficer: String,
  val responsibleOfficer: String?,
  val serviceProviderUser: String?,
  val serviceProviderLocation: String?,
  val serviceProviderName: String?,
  val isDraft: Boolean,
) {
  companion object {
    fun from(referral: Referral, responsibleOfficer: ResponsibleProbationPractitioner, serviceProviderUser: UserDetail?, referringOfficer: UserDetail): ProbationCaseReferralDTO {
      return ProbationCaseReferralDTO(
        serviceCategories = referral.selectedServiceCategories?.map { it.name },
        contractType = referral.intervention.dynamicFrameworkContract.contractType.name,
        referralCreatedAt = referral.createdAt,
        referralSentAt = referral.sentAt,
        interventionTitle = referral.intervention.title,
        referringOfficer = "${referringOfficer.firstName} ${referringOfficer.lastName}",
        responsibleOfficer = "${responsibleOfficer.firstName} ${responsibleOfficer.lastName}",
        serviceProviderUser = serviceProviderUser?.let { "${serviceProviderUser.firstName} ${serviceProviderUser.lastName}" },
        serviceProviderLocation = if (referral.intervention.dynamicFrameworkContract.pccRegion != null) {
          referral.intervention.dynamicFrameworkContract.pccRegion?.name
        } else {
          referral.intervention.dynamicFrameworkContract.npsRegion!!.name
        },
        serviceProviderName = referral.intervention.dynamicFrameworkContract.primeProvider.name,
        isDraft = false,
      )
    }

    fun from(referral: DraftReferral, referringOfficer: UserDetail): ProbationCaseReferralDTO {
      return ProbationCaseReferralDTO(
        serviceCategories = referral.selectedServiceCategories?.map { it.name },
        contractType = referral.intervention.dynamicFrameworkContract.contractType.name,
        referralCreatedAt = referral.createdAt,
        referralSentAt = null,
        interventionTitle = referral.intervention.title,
        referringOfficer = "${referringOfficer.firstName} ${referringOfficer.lastName}",
        responsibleOfficer = null,
        serviceProviderUser = null,
        serviceProviderLocation = null,
        serviceProviderName = null,
        isDraft = true,
      )
    }
  }
}
