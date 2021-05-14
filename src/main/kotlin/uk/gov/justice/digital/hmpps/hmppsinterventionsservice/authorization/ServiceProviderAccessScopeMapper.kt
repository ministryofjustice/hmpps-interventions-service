package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.authorization

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.config.AccessError
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AuthUser
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.DynamicFrameworkContract
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ServiceProvider
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.DynamicFrameworkContractRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ServiceProviderRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.HMPPSAuthService

data class ServiceProviderAccessScope(
  val serviceProvider: ServiceProvider?,
  val contracts: List<DynamicFrameworkContract>,
)

@Component
class ServiceProviderAccessScopeMapper(
  private val hmppsAuthService: HMPPSAuthService,
  private val serviceProviderRepository: ServiceProviderRepository,
  private val dynamicFrameworkContractRepository: DynamicFrameworkContractRepository,
  private val userTypeChecker: UserTypeChecker,
) {
  private val serviceProviderGroupPrefix = "INT_SP_"
  private val contractGroupPrefix = "INT_CG_"
  private val errorMessage = "could not map service provider user to access scope"

  fun fromUser(user: AuthUser): ServiceProviderAccessScope {
    if (!userTypeChecker.isServiceProviderUser(user)) {
      throw AccessError(errorMessage, listOf("user is not a service provider"))
    }

    val groups = hmppsAuthService.getUserGroups(user)
      ?: throw AccessError(errorMessage, listOf("cannot find user in hmpps auth"))

    val configErrors = mutableListOf<String>()

    val serviceProviderGroups = groups
      .filter { it.startsWith(serviceProviderGroupPrefix) }
      .map { it.removePrefix(serviceProviderGroupPrefix) }

    if (serviceProviderGroups.isEmpty()) {
      configErrors.add("no service provider groups associated with user")
    } else if (serviceProviderGroups.size > 1) {
      configErrors.add("more than one service provider groups associated with user")
    }

    val serviceProvider = serviceProviderRepository.findByIdOrNull(serviceProviderGroups[0])

    if (serviceProvider == null) {
      configErrors.add("service provider id does not exist in the interventions database")
    }

    val contractGroups = groups
      .filter { it.startsWith(contractGroupPrefix) }
      .map { it.removePrefix(contractGroupPrefix) }

    if (contractGroups.isEmpty()) {
      configErrors.add("no contract groups associated with user")
    }

    val contracts = dynamicFrameworkContractRepository.findAllByContractReferenceIn(contractGroups)

    if (contracts.size != contractGroups.size) {
      configErrors.add("at least one contract group does not exist in the interventions database")
    }

    if (configErrors.isNotEmpty()) {
      throw AccessError(errorMessage, configErrors)
    }

    return ServiceProviderAccessScope(serviceProvider = serviceProvider, contracts = contracts)
  }
}
