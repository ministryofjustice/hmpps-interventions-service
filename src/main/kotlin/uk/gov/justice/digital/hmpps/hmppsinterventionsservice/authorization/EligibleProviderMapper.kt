package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.authorization

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.DynamicFrameworkContract
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ServiceProvider

@Component
class EligibleProviderMapper {
  // get the service providers who are eligible to access this referral
  fun fromReferral(dynamicFrameworkContract: DynamicFrameworkContract): List<ServiceProvider> {
    return listOf(dynamicFrameworkContract.primeProvider) + dynamicFrameworkContract.subcontractorProviders.toList()
  }
}
