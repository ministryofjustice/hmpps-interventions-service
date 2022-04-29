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
  private val noAccessToPageHeader = "You do not have permission to view this page"
  private val noAccessToServiceHeader = "You do not have permission to view this service"

  fun fromUser(user: AuthUser): ServiceProviderAccessScope {
    if (!userTypeChecker.isServiceProviderUser(user)) {
      throw AccessError(
        user,
        noAccessToPageHeader,
        listOf("Your account is not set up correctly. Ask an admin user in your organisation to add the ‘CRS provider’ role in HMPPS Digital Services.")
      )
    }

    val groups = hmppsAuthService.getUserGroups(user) ?: throw AccessError(
      user,
      noAccessToPageHeader,
      listOf("Your email address is not recognised. If it has changed recently, try signing out and signing in with the correct one. Ask an admin user in your organisation to check what the right email is in HMPPS Digital Services. If that does not work, <a target=\"_blank\" href=\"https://hmpps-interventions-ui-dev.apps.live-1.cloud-platform.service.justice.gov.uk/report-a-problem\">report it as a problem.</a>")
    )

    // order is important as each step can mutate WorkingScope
    val workingScope = WorkingScope(authGroups = groups)

    resolveProviders(workingScope)
    if (workingScope.errors.isNotEmpty()) {
      throw AccessError(
        user,
        noAccessToServiceHeader,
        listOf("Your provider group is not recognised. Ask an admin in your organisation to check it has been set up correctly in HMPPS Digital Services. <a target=\"_blank\" href=\"https://hmpps-interventions-ui-dev.apps.live-1.cloud-platform.service.justice.gov.uk/report-a-problem\">They may need to report it as a problem.</a>")
      )
    }

    resolveContracts(workingScope)
    if (workingScope.errors.isNotEmpty()) {
      throw AccessError(
        user,
        noAccessToServiceHeader,
        listOf("Your contract group is not recognised. Ask an admin in your organisation to check it has been set up correctly in HMPPS Digital Services. <a target=\"_blank\" href=\"https://hmpps-interventions-ui-dev.apps.live-1.cloud-platform.service.justice.gov.uk/report-a-problem\">They may need to report it as a problem.</a>")
      )
    }

    removeInaccessibleContracts(workingScope)
    if (workingScope.errors.isNotEmpty()) {
      throw AccessError(
        user,
        noAccessToServiceHeader,
        listOf("The contract and supplier groups on your account do not match. Ask an admin user in your organisation to fix this in HMPPS Digital Services.")
      )
    }

    blockUsersWithoutProviders(workingScope)
    if (workingScope.errors.isNotEmpty()) {
      throw AccessError(
        user,
        noAccessToServiceHeader,
        listOf("You do not have any supplier groups on your account. Ask an admin in your organisation to set this up in HMPPS Digital Services.")
      )
    }

    blockUsersWithoutContracts(workingScope)
    if (workingScope.errors.isNotEmpty()) {
      throw AccessError(
        user,
        noAccessToServiceHeader,
        listOf("You do not have any contract groups on your account. Ask an admin in your organisation to set this up in HMPPS Digital Services.")
      )
    }

    return ServiceProviderAccessScope(
      serviceProviders = workingScope.providers,
      contracts = workingScope.contracts,
    ).also {
      telemetryClient.trackEvent(
        "InterventionsAuthorizedProvider",
        mapOf(
          "userId" to user.id,
          "userName" to user.userName,
          "userAuthSource" to user.authSource,
          "contracts" to it.contracts.joinToString(",") { c -> c.contractReference },
          "providers" to it.serviceProviders.joinToString(",") { p -> p.id },
        ),
        null
      )
    }
  }

  private fun resolveProviders(scope: WorkingScope) {
    val serviceProviderGroups = scope.authGroups.filter { it.startsWith(serviceProviderGroupPrefix) }
      .map { it.removePrefix(serviceProviderGroupPrefix) }

    val providers = getProviders(serviceProviderGroups, scope.errors)
    scope.providers.addAll(providers)
  }

  private fun resolveContracts(scope: WorkingScope) {
    val contractGroups =
      scope.authGroups.filter { it.startsWith(contractGroupPrefix) }.map { it.removePrefix(contractGroupPrefix) }

    val contracts = getContracts(contractGroups, scope.errors)
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

  private fun getProviders(providerGroups: List<String>, configErrors: MutableList<String>): List<ServiceProvider> {
    val providers = serviceProviderRepository.findAllById(providerGroups)
    val unidentifiedProviders = providerGroups.subtract(providers.map { it.id })
    unidentifiedProviders.forEach { undefinedProvider ->
      configErrors.add("unidentified provider '$undefinedProvider': group does not exist in the reference data")
    }
    return providers.sortedBy { it.id }
  }

  private fun getContracts(
    contractGroups: List<String>,
    configErrors: MutableList<String>
  ): List<DynamicFrameworkContract> {
    val contracts = dynamicFrameworkContractRepository.findAllByContractReferenceIn(contractGroups)
    val unidentifiedContracts = contractGroups.subtract(contracts.map { it.contractReference })
    unidentifiedContracts.forEach { undefinedContract ->
      configErrors.add("unidentified contract '$undefinedContract': group does not exist in the reference data")
    }
    return contracts.sortedBy { it.contractReference }
  }
}
