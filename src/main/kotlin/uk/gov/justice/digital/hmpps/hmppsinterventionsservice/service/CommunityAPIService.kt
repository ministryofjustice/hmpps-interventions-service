package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service

import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Referral

interface CommunityAPIService {
  fun getNotes(referral: Referral, url: String, description: String): String {
    val contractTypeName = referral.intervention.dynamicFrameworkContract.contractType.name
    val primeProviderName = referral.intervention.dynamicFrameworkContract.primeProvider.name
    return "$description for $contractTypeName Referral ${referral.referenceNumber} with Prime Provider $primeProviderName\n$url"
  }
}
