package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto

import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Referral
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.ResponsibleProbationPractitioner
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.UserDetail
import java.time.OffsetDateTime

data class ProbationCaseReferralDTO(
  val serviceCategories: List<String>?, //The type of referral sent ( I think type is how you differentiate accomodation/wellbeing etc)
  val contractType: String,
  val referralSentAt: OffsetDateTime?, //the date/time the referral was sent
  val interventionTitle: String, //the intervention Title
  val referringOfficer: String, //The name of the user who created the referral
  val responsibleOfficer: String, //Who the referral is assigned to (or if it is currently unassigned)
  val serviceProviderUser: String?, //The name of the service provider the referral was sent to
  val serviceProviderLocation: String?, //The location of the service provider
  val serviceProviderName: String
) {
  companion object {
    fun from(referral: Referral, responsibleOfficer: ResponsibleProbationPractitioner, serviceProviderUser: UserDetail?, referringOfficer: UserDetail): ProbationCaseReferralDTO {
      return ProbationCaseReferralDTO(
        serviceCategories = referral.selectedServiceCategories?.map { it.name },
        contractType = referral.intervention.dynamicFrameworkContract.contractType.name,
        referralSentAt = referral.sentAt,
        interventionTitle = referral.intervention.title,
        referringOfficer = "${referringOfficer.firstName} ${referringOfficer.lastName}",
        responsibleOfficer = "${responsibleOfficer.firstName} ${responsibleOfficer.lastName}",
        serviceProviderUser = serviceProviderUser?.let { "${serviceProviderUser.firstName} ${serviceProviderUser.lastName}" },
        serviceProviderLocation = if(referral.intervention.dynamicFrameworkContract.pccRegion != null) {
          referral.intervention.dynamicFrameworkContract.pccRegion?.name
        }  else{
          referral.intervention.dynamicFrameworkContract.npsRegion!!.name
        },
        serviceProviderName = referral.intervention.dynamicFrameworkContract.primeProvider.name
      )
    }
  }
}







