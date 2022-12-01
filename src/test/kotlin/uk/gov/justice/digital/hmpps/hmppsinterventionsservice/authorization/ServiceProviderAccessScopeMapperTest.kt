package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.authorization

import com.microsoft.applicationinsights.TelemetryClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.config.AccessError
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AuthUser
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.DynamicFrameworkContract
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.SampleData
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ServiceProvider
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.DynamicFrameworkContractRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ServiceProviderRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.HMPPSAuthService

class ServiceProviderAccessScopeMapperTest {
  private lateinit var mapper: ServiceProviderAccessScopeMapper
  private val userTypeChecker = UserTypeChecker()

  private val hmppsAuthService = mock<HMPPSAuthService>()
  private val serviceProviderRepository = mock<ServiceProviderRepository>()
  private val contractRepository = mock<DynamicFrameworkContractRepository>()
  private val telemetryClient = mock<TelemetryClient>()

  private val spUser =
    AuthUser(id = "b40ac52d-037d-4732-a3cd-bf5484c6ab6a", userName = "test@test.example.org", authSource = "auth")
  private val ppUser = AuthUser(id = "123456789", userName = "TEST_USER", authSource = "delius")

  @BeforeEach
  fun setup() {
    mapper = ServiceProviderAccessScopeMapper(
      hmppsAuthService = hmppsAuthService,
      serviceProviderRepository = serviceProviderRepository,
      dynamicFrameworkContractRepository = contractRepository,
      userTypeChecker = userTypeChecker,
      telemetryClient = telemetryClient
    )
  }

  @Test
  fun `throws AccessError if the user is not a service provider`() {
    val error = assertThrows<AccessError> { mapper.fromUser(ppUser) }
    assertThat(error.user).isEqualTo(ppUser)
    assertThat(error.message).isEqualTo("could not map service provider user to access scope")
    assertThat(error.errors).containsExactly("user is not a service provider")
  }

  @Test
  fun `throws AccessError if the user's auth groups cannot be retrieved`() {
    whenever(hmppsAuthService.getUserGroups(spUser)).thenReturn(null)

    val error = assertThrows<AccessError> { mapper.fromUser(spUser) }
    assertThat(error.user).isEqualTo(spUser)
    assertThat(error.message).isEqualTo("could not map service provider user to access scope")
    assertThat(error.errors).containsExactly("cannot find user in hmpps auth")
  }

  // the business behaviour is tested through integration tests at
  // src/test/kotlin/uk/gov/justice/digital/hmpps/hmppsinterventionsservice/integration/authorization

  @Nested
  @DisplayName("for valid provider scopes")
  inner class ForValidScope {
    private lateinit var providers: List<ServiceProvider>
    private lateinit var contracts: List<DynamicFrameworkContract>

    @BeforeEach
    fun setup() {
      whenever(hmppsAuthService.getUserGroups(spUser)).thenReturn(
        listOf(
          "INT_SP_TEST_P1",
          "INT_SP_TEST_A2",
          "INT_SP_UNKNOWN",
          "INT_CR_TEST_C0001",
          "INT_CR_TEST_A0002",
          "INT_CR_UNDEFINED"
        )
      )

      val p1 = ServiceProvider(id = "TEST_P1", name = "Test 1 Ltd")
      val p2 = ServiceProvider(id = "TEST_A2", name = "Test 2 Ltd")
      providers = listOf(p1, p2)
      whenever(serviceProviderRepository.findAllById(listOf("TEST_P1", "TEST_A2", "UNKNOWN")))
        .thenReturn(providers)

      val c1 = SampleData.sampleContract(contractReference = "TEST_C0001", primeProvider = p1)
      val c2 = SampleData.sampleContract(contractReference = "TEST_A0002", primeProvider = p2)
      contracts = listOf(c1, c2)
      whenever(contractRepository.findAllByContractReferenceIn(listOf("TEST_C0001", "TEST_A0002", "UNDEFINED")))
        .thenReturn(contracts)
    }

    @Test
    fun `returns the access scope with providers sorted by their ID`() {
      val scope = mapper.fromUser(spUser)
      assertThat(scope.serviceProviders.map { it.id }).containsExactly("TEST_A2", "TEST_P1")
    }

    @Test
    fun `returns the access scope with contracts sorted by their contract reference`() {
      val scope = mapper.fromUser(spUser)
      assertThat(scope.contracts.map { it.contractReference }).containsExactly("TEST_A0002", "TEST_C0001")
    }

    @Test
    fun `tracks the successful authorisation event in AppInsights telemetry`() {
      mapper.fromUser(spUser)
      verify(telemetryClient).trackEvent(
        "InterventionsAuthorizedProvider",
        mapOf(
          "userId" to "b40ac52d-037d-4732-a3cd-bf5484c6ab6a",
          "userName" to "test@test.example.org",
          "userAuthSource" to "auth",
          "contracts" to "TEST_A0002,TEST_C0001",
          "providers" to "TEST_A2,TEST_P1",
        ),
        null
      )
    }

    @Test
    fun `emits warning AppInsights telemetry about the undefined provider and contract`() {
      mapper.fromUser(spUser)
      verify(telemetryClient).trackEvent(
        "InterventionsAuthorizationWarning",
        mapOf(
          "userId" to "b40ac52d-037d-4732-a3cd-bf5484c6ab6a",
          "userName" to "test@test.example.org",
          "userAuthSource" to "auth",
          "issues" to "[unidentified provider 'UNKNOWN': group does not exist in the reference data, unidentified contract 'UNDEFINED': group does not exist in the reference data]",
        ),
        null
      )
    }
  }
}
