package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.authorization

import com.microsoft.applicationinsights.TelemetryClient
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.config.AccessError
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AuthUser
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.DynamicFrameworkContract
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ServiceProvider
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.DynamicFrameworkContractRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ServiceProviderRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.HMPPSAuthService

data class ServiceProviderAccessScope(
  val serviceProviders: Set<ServiceProvider>,
  val contracts: Set<DynamicFrameworkContract>,
)

private data class WorkingScope(
  val authGroups: List<String>,
  val providers: MutableSet<ServiceProvider> = mutableSetOf(),
  val contracts: MutableSet<DynamicFrameworkContract> = mutableSetOf(),
  val errors: MutableList<String> = mutableListOf(),
  val warnings: MutableList<String> = mutableListOf(),
)

@Component
@Transactional
class ServiceProviderAccessScopeMapper(
  private val hmppsAuthService: HMPPSAuthService,
  private val serviceProviderRepository: ServiceProviderRepository,
  private val dynamicFrameworkContractRepository: DynamicFrameworkContractRepository,
  private val userTypeChecker: UserTypeChecker,
  private val telemetryClient: TelemetryClient,
) {
  private val serviceProviderGroupPrefix = "INT_SP_"
  private val contractGroupPrefix = "INT_CR_"
  private val errorMessage = "could not map service provider user to access scope"

  fun fromUser(user: AuthUser): ServiceProviderAccessScope {
    if (!userTypeChecker.isServiceProviderUser(user)) {
      throw AccessError(user, errorMessage, listOf("user is not a service provider"))
    }

    val groups = hmppsAuthService.getUserGroups(user)
      ?: throw AccessError(user, errorMessage, listOf("cannot find user in hmpps auth"))

    // order is important as each step can mutate WorkingScope
    val workingScope = WorkingScope(authGroups = groups)

    resolveProviders(workingScope)
    resolveContracts(workingScope)
    removeInaccessibleContracts(workingScope)

    blockUsersWithoutProviders(workingScope)
    blockUsersWithoutContracts(workingScope)

    if (workingScope.warnings.isNotEmpty()) {
      trackWarnings(user, workingScope.warnings)
    }
    if (workingScope.errors.isNotEmpty()) {
      throw AccessError(user, errorMessage, workingScope.errors)
    }

    return ServiceProviderAccessScope(
      serviceProviders = workingScope.providers,
      contracts = workingScope.contracts,
    ).also {
      trackAuthorization(user, it)
    }
  }

  private fun trackAuthorization(user: AuthUser, scope: ServiceProviderAccessScope) {
    telemetryClient.trackEvent(
      "InterventionsAuthorizedProvider",
      mapOf(
        "userId" to user.id,
        "userName" to user.userName,
        "userAuthSource" to user.authSource,
        "contracts" to scope.contracts.joinToString(",") { c -> c.contractReference },
        "providers" to scope.serviceProviders.joinToString(",") { p -> p.id },
      ),
      null,
    )
  }

  private fun trackWarnings(user: AuthUser, scope: MutableList<String>) {
    telemetryClient.trackEvent(
      "InterventionsAuthorizationWarning",
      mapOf(
        "userId" to user.id,
        "userName" to user.userName,
        "userAuthSource" to user.authSource,
        "issues" to scope.toString(),
      ),
      null,
    )
  }

  private fun resolveProviders(scope: WorkingScope) {
    val serviceProviderGroups = scope.authGroups
      .filter { it.startsWith(serviceProviderGroupPrefix) }
      .map { it.removePrefix(serviceProviderGroupPrefix) }

    val providers = getProviders(serviceProviderGroups, scope.warnings)
    scope.providers.addAll(providers)
  }

  private fun resolveContracts(scope: WorkingScope) {
    val contractGroups = scope.authGroups
      .filter { it.startsWith(contractGroupPrefix) }
      .map { it.removePrefix(contractGroupPrefix) }

    val contracts = getContracts(contractGroups, scope.warnings)
    scope.contracts.addAll(contracts)
  }

  private fun removeInaccessibleContracts(userScope: WorkingScope) {
    val contractsNotAssociatedWithUserProviders = userScope.contracts.filter { contract ->
      val providersOnContract = setOf(contract.primeProvider).union(contract.subcontractorProviders)
      userScope.providers.intersect(providersOnContract).isEmpty()
    }
    contractsNotAssociatedWithUserProviders.forEach {
      userScope.errors.add("contract '${it.contractReference}' is not accessible to providers ${userScope.providers.map { p -> p.id }}")
    }
    userScope.contracts.subtract(contractsNotAssociatedWithUserProviders)
  }

  private fun blockUsersWithoutContracts(scope: WorkingScope) {
    if (scope.contracts.isEmpty()) {
      scope.errors.add("no valid contract groups associated with user")
    }
  }

  private fun blockUsersWithoutProviders(scope: WorkingScope) {
    if (scope.providers.isEmpty()) {
      scope.errors.add("no valid service provider groups associated with user")
    }
  }

  private fun getProviders(providerGroups: List<String>, warnings: MutableList<String>): List<ServiceProvider> {
    val providers = serviceProviderRepository.findAllById(providerGroups)
    val unidentifiedProviders = providerGroups.subtract(providers.map { it.id })
    unidentifiedProviders.forEach { undefinedProvider ->
      warnings.add("unidentified provider '$undefinedProvider': group does not exist in the reference data")
    }
    return providers.sortedBy { it.id }
  }

  private fun getContracts(contractGroups: List<String>, warnings: MutableList<String>): List<DynamicFrameworkContract> {
    val contracts = dynamicFrameworkContractRepository.findAllByContractReferenceIn(contractGroups)
    val unidentifiedContracts = contractGroups.subtract(contracts.map { it.contractReference }.toSet())
    unidentifiedContracts.forEach { undefinedContract ->
      warnings.add("unidentified contract '$undefinedContract': group does not exist in the reference data")
    }
    return contracts.sortedBy { it.contractReference }
  }
}
