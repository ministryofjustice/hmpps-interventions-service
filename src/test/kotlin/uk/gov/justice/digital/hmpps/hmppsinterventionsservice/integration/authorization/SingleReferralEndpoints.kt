package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.integration.authorization

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.boot.test.mock.mockito.MockBean
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.CreateReferralRequestDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AuthUser
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.DynamicFrameworkContract
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Referral
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.CommunityAPIOffenderService
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.CommunityAPIReferralService
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.HMPPSAuthService
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.RisksAndNeedsService
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.ServiceUserAccessResult
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.JwtTokenFactory
import java.util.UUID

// @Suppress("UNUSED") is used for things that are only used through parameterised tests, so appear unused
class SingleReferralEndpoints : IntegrationTestBase() {
  @MockBean lateinit var mockHmppsAuthService: HMPPSAuthService
  @MockBean lateinit var mockCommunityAPIOffenderService: CommunityAPIOffenderService
  @MockBean @Suppress("UNUSED") lateinit var mockCommunityApiReferralService: CommunityAPIReferralService
  @MockBean lateinit var mockRisksAndNeedsService: RisksAndNeedsService

  private lateinit var requestFactory: RequestFactory

  private val tokenFactory = JwtTokenFactory()

  @BeforeEach
  fun initRequestBuilder() {
    requestFactory = RequestFactory(webTestClient, setupAssistant)
  }

  @BeforeEach
  fun setupMocks() {
    // required for 'sendDraftReferral' to return a valid DTO
    whenever(mockRisksAndNeedsService.createSupplementaryRisk(any(), any(), any(), anyOrNull(), any(), anyOrNull())).thenReturn(UUID.randomUUID())
  }

  companion object {
    @Suppress("UNUSED")
    @JvmStatic fun draftReferralRequests(): List<Request> {
      return listOf(Request.GetDraftReferral, Request.UpdateDraftReferral, Request.SendDraftReferral)
    }

    @Suppress("UNUSED")
    @JvmStatic fun sentReferralRequests(): List<Request> {
      return listOf(Request.GetSentReferral, Request.AssignSentReferral, Request.EndSentReferral)
    }

    @Suppress("UNUSED")
    @JvmStatic fun allReferralRequests(): List<Request> {
      return draftReferralRequests() + sentReferralRequests()
    }
  }

  private fun setUserGroups(user: AuthUser, groups: List<String>?) {
    whenever(mockHmppsAuthService.getUserGroups(user)).thenReturn(groups)
  }

  private fun setLimitedAccessCRNs(vararg crns: String) {
    // order is important here! it's LIFO so generic matchers first, followed by specific CRNs
    whenever(mockCommunityAPIOffenderService.checkIfAuthenticatedDeliusUserHasAccessToServiceUser(any(), any()))
      .thenReturn(ServiceUserAccessResult(true, listOf()))

    crns.forEach {
      whenever(mockCommunityAPIOffenderService.checkIfAuthenticatedDeliusUserHasAccessToServiceUser(any(), eq(it)))
        .thenReturn(ServiceUserAccessResult(false, listOf("exclusion message", "restriction message")))
    }
  }

  private fun createSentReferral(contract: DynamicFrameworkContract): Referral {
    return setupAssistant.createSentReferral(intervention = setupAssistant.createIntervention(dynamicFrameworkContract = contract))
  }

  private fun createEncodedTokenForUser(user: AuthUser): String {
    return tokenFactory.createEncodedToken(userID = user.id, userName = user.userName, authSource = user.authSource)
  }

  @ParameterizedTest(name = "{displayName} ({argumentsWithNames})")
  @MethodSource("draftReferralRequests")
  fun `pp user cannot access limited access offender referrals`(request: Request) {
    val user = setupAssistant.createPPUser()
    val token = createEncodedTokenForUser(user)

    setLimitedAccessCRNs("X999999")
    val referral1 = setupAssistant.createDraftReferral(serviceUserCRN = "X000000")
    val referral2 = setupAssistant.createDraftReferral(serviceUserCRN = "X999999")
    setupAssistant.fillDraftReferralFields(referral1)
    setupAssistant.fillDraftReferralFields(referral2)

    requestFactory.create(request, token, referral1.id.toString())
      .exchange()
      .expectStatus()
      .is2xxSuccessful

    requestFactory.create(request, token, referral2.id.toString())
      .exchange()
      .expectStatus().isForbidden
      .expectBody().json(
        """
        {"accessErrors": [
        "exclusion message",
        "restriction message"
        ]}
        """.trimIndent()
      )
  }

  @Test
  fun `pp user cannot create referrals for limited access offenders`() {
    val user = setupAssistant.createPPUser()
    val token = createEncodedTokenForUser(user)
    val intervention = setupAssistant.createIntervention()

    setLimitedAccessCRNs("X5555555")

    requestFactory.create(Request.CreateDraftReferral, token, body = CreateReferralRequestDTO("X1233456", intervention.id))
      .exchange()
      .expectStatus()
      .is2xxSuccessful

    requestFactory.create(Request.CreateDraftReferral, token, body = CreateReferralRequestDTO("X5555555", intervention.id))
      .exchange()
      .expectStatus().isForbidden
      .expectBody().json(
        """
        {"accessErrors": [
        "exclusion message",
        "restriction message"
        ]}
        """.trimIndent()
      )
  }

  @ParameterizedTest(name = "{displayName} ({argumentsWithNames})")
  @MethodSource("sentReferralRequests")
  fun `sp user works for prime provider and has required contract group`(request: Request) {
    val user = setupAssistant.createSPUser()

    setUserGroups(
      user,
      listOf(
        "INT_SP_HARMONY_LIVING",
        "INT_CR_0001",
      )
    )

    val contract = setupAssistant.createDynamicFrameworkContract(
      contractReference = "0001",
      primeProviderId = "HARMONY_LIVING",
      subContractorServiceProviderIds = emptySet()
    )
    val referral = createSentReferral(contract)
    val token = createEncodedTokenForUser(user)

    requestFactory.create(request, token, referral.id.toString())
      .exchange()
      .expectStatus()
      .is2xxSuccessful
  }

  @ParameterizedTest(name = "{displayName} ({argumentsWithNames})")
  @MethodSource("sentReferralRequests")
  fun `sp user works for subcontractor provider and has required contract group`(request: Request) {
    val user = setupAssistant.createSPUser()

    setUserGroups(
      user,
      listOf(
        "INT_SP_BETTER_LTD",
        "INT_CR_0001",
      )
    )

    val contract = setupAssistant.createDynamicFrameworkContract(
      contractReference = "0001",
      primeProviderId = "HARMONY_LIVING",
      subContractorServiceProviderIds = setOf("BETTER_LTD")
    )
    val referral = createSentReferral(contract)
    val token = createEncodedTokenForUser(user)

    requestFactory.create(request, token, referral.id.toString())
      .exchange()
      .expectStatus()
      .is2xxSuccessful
  }

  @ParameterizedTest(name = "{displayName} ({argumentsWithNames})")
  @MethodSource("sentReferralRequests")
  fun `sp user works for a provider that does not exist and has a non existent contract group`(request: Request) {
    val user = setupAssistant.createSPUser()

    setUserGroups(
      user,
      listOf(
        "INT_SP_BETTER_LTD",
        "INT_CR_0002",
      )
    )

    val contract = setupAssistant.createDynamicFrameworkContract(
      contractReference = "0001",
      primeProviderId = "HARMONY_LIVING",
      subContractorServiceProviderIds = setOf()
    )
    val referral = createSentReferral(contract)
    val token = createEncodedTokenForUser(user)

    val response = requestFactory.create(request, token, referral.id.toString()).exchange()
    response.expectStatus().isForbidden
    response.expectBody().json(
      """
      {"accessErrors": [
      "unidentified provider 'BETTER_LTD': group does not exist in the reference data",
      "unidentified contract '0002': group does not exist in the reference data",
      "no valid service provider groups associated with user",
      "no valid contract groups associated with user"
      ]}
      """.trimIndent()
    )
  }

  @ParameterizedTest(name = "{displayName} ({argumentsWithNames})")
  @MethodSource("sentReferralRequests")
  fun `sp user works for the wrong provider and has wrong contract group`(request: Request) {
    val user = setupAssistant.createSPUser()

    setUserGroups(
      user,
      listOf(
        "INT_SP_BETTER_LTD",
        "INT_CR_0002",
      )
    )

    setupAssistant.createDynamicFrameworkContract(
      contractReference = "0002",
      primeProviderId = "BETTER_LTD",
      subContractorServiceProviderIds = setOf()
    )

    val contract = setupAssistant.createDynamicFrameworkContract(
      contractReference = "0001",
      primeProviderId = "HARMONY_LIVING",
      subContractorServiceProviderIds = setOf()
    )

    val referral = createSentReferral(contract)
    val token = createEncodedTokenForUser(user)

    val response = requestFactory.create(request, token, referral.id.toString()).exchange()
    response.expectStatus().isForbidden
    response.expectBody().json(
      """
      {"accessErrors": [
      "user does not have the required provider group to access this referral",
      "user does not have the required contract group to access this referral"
      ]}
      """.trimIndent()
    )
  }

  @ParameterizedTest(name = "{displayName} ({argumentsWithNames})")
  @MethodSource("sentReferralRequests")
  fun `sp user has one of the providers required to access the referral but not the contract`(request: Request) {
    val user = setupAssistant.createSPUser()

    setUserGroups(
      user,
      listOf(
        "INT_SP_BETTER_LTD",
        "INT_SP_HARMONY_LIVING",
        "INT_CR_0002",
      )
    )

    setupAssistant.createDynamicFrameworkContract(
      contractReference = "0002",
      primeProviderId = "BETTER_LTD",
      subContractorServiceProviderIds = setOf()
    )

    val contract = setupAssistant.createDynamicFrameworkContract(
      contractReference = "0001",
      primeProviderId = "HARMONY_LIVING",
      subContractorServiceProviderIds = setOf()
    )

    val referral = createSentReferral(contract)
    val token = createEncodedTokenForUser(user)

    val response = requestFactory.create(request, token, referral.id.toString()).exchange()
    response.expectStatus().isForbidden
    response.expectBody().json(
      """
      {"accessErrors": [
      "user does not have the required contract group to access this referral"
      ]}
      """.trimIndent()
    )
  }
  @ParameterizedTest(name = "{displayName} ({argumentsWithNames})")
  @MethodSource("sentReferralRequests")
  fun `sp user works for subcontractor provider but is missing required contract group`(request: Request) {
    val user = setupAssistant.createSPUser()

    setUserGroups(
      user,
      listOf(
        "INT_SP_BETTER_LTD",
        "INT_CR_0002",
      )
    )

    val contract = setupAssistant.createDynamicFrameworkContract(
      contractReference = "0001",
      primeProviderId = "HARMONY_LIVING",
      subContractorServiceProviderIds = setOf("BETTER_LTD")
    )
    val referral = createSentReferral(contract)
    val token = createEncodedTokenForUser(user)

    val response = requestFactory.create(request, token, referral.id.toString()).exchange()
    response.expectStatus().isForbidden
    response.expectBody().json(
      """
      {"accessErrors": [
      "unidentified contract '0002': group does not exist in the reference data",
      "no valid contract groups associated with user"
      ]}
      """.trimIndent()
    )
  }

  @ParameterizedTest(name = "{displayName} ({argumentsWithNames})")
  @MethodSource("sentReferralRequests")
  fun `sp user does not have any groups in hmpps auth`(request: Request) {
    val user = setupAssistant.createSPUser()
    setUserGroups(user, emptyList())

    val contract = setupAssistant.createDynamicFrameworkContract(
      contractReference = "0001",
      primeProviderId = "HARMONY_LIVING",
      subContractorServiceProviderIds = setOf("BETTER_LTD")
    )
    val referral = createSentReferral(contract)
    val token = createEncodedTokenForUser(user)

    val response = requestFactory.create(request, token, referral.id.toString()).exchange()
    response.expectStatus().isForbidden
    response.expectBody().json(
      """
      {"accessErrors": [
      "no valid service provider groups associated with user",
      "no valid contract groups associated with user"
      ]}
      """.trimIndent()
    )
  }

  @ParameterizedTest(name = "{displayName} ({argumentsWithNames})")
  @MethodSource("sentReferralRequests")
  fun `sp users with multiple providers can access all referrals with those providers and contracts`(request: Request) {
    val user = setupAssistant.createSPUser()
    setUserGroups(
      user,
      listOf(
        "INT_SP_HARMONY_LIVING",
        "INT_SP_BETTER_LTD",
        "INT_CR_0001",
        "INT_CR_0002",
        "INT_CR_0003",
      )
    )

    val contract1 = setupAssistant.createDynamicFrameworkContract(
      contractReference = "0001",
      primeProviderId = "HARMONY_LIVING",
      subContractorServiceProviderIds = setOf("BETTER_LTD")
    )
    val contract2 = setupAssistant.createDynamicFrameworkContract(
      contractReference = "0002",
      primeProviderId = "BETTER_LTD",
    )
    val contract3 = setupAssistant.createDynamicFrameworkContract(
      contractReference = "0003",
      primeProviderId = "DONT_HAVE_THIS_GROUP",
      subContractorServiceProviderIds = setOf("BETTER_LTD")
    )
    val referral1 = createSentReferral(contract1)
    val referral2 = createSentReferral(contract2)
    val referral3 = createSentReferral(contract3)
    val token = createEncodedTokenForUser(user)

    val checkReferral1 = requestFactory.create(request, token, referral1.id.toString()).exchange()
    checkReferral1.expectStatus().is2xxSuccessful

    val checkReferral2 = requestFactory.create(request, token, referral2.id.toString()).exchange()
    checkReferral2.expectStatus().is2xxSuccessful

    val checkReferral3 = requestFactory.create(request, token, referral3.id.toString()).exchange()
    checkReferral3.expectStatus().is2xxSuccessful
  }

  @ParameterizedTest(name = "{displayName} ({argumentsWithNames})")
  @MethodSource("draftReferralRequests")
  fun `sp users can never access any draft referrals`(request: Request) {
    val user = setupAssistant.createSPUser()

    val referral = setupAssistant.createDraftReferral()
    val token = createEncodedTokenForUser(user)

    val response = requestFactory.create(request, token, referral.id.toString()).exchange()
    response.expectStatus().isForbidden
    response.expectBody().json(
      """
      {"accessErrors": [
      "only probation practitioners can access draft referrals"
      ]}
      """.trimIndent()
    )
  }

  @ParameterizedTest(name = "{displayName} ({argumentsWithNames})")
  @MethodSource("sentReferralRequests")
  fun `nomis users can never access anything`(request: Request) {
    val referral = setupAssistant.createSentReferral()
    val token = tokenFactory.createEncodedToken("123456", "nomis", "tom")

    val response = requestFactory.create(request, token, referral.id.toString()).exchange()
    response.expectStatus().isForbidden
    response.expectBody().json(
      """
      {"accessErrors": ["logins from nomis are not supported"]}
      """.trimIndent()
    )
  }

  @ParameterizedTest(name = "{displayName} ({argumentsWithNames})")
  @MethodSource("sentReferralRequests")
  fun `users not found in hmpps auth can never access anything`(request: Request) {
    val user = setupAssistant.createSPUser()
    setUserGroups(user, null)

    val referral = setupAssistant.createSentReferral()
    val token = createEncodedTokenForUser(user)

    val response = requestFactory.create(request, token, referral.id.toString()).exchange()
    response.expectStatus().isForbidden
    response.expectBody().json(
      """
      {"accessErrors": [
      "cannot find user in hmpps auth"
      ]}
      """.trimIndent()
    )
  }

  @ParameterizedTest(name = "{displayName} ({argumentsWithNames})")
  @MethodSource("allReferralRequests")
  fun `auth tokens with missing claims can never access anything`(request: Request) {
    val token = tokenFactory.createEncodedToken("123456", null, null)
    val response = requestFactory.create(request, token, UUID.randomUUID().toString()).exchange()
    response.expectStatus().isForbidden
    response.expectBody().json(
      """
      {"status":403,"error":"access denied",
       "message":"could not map auth token to user: [no 'user_name' claim in token, no 'auth_source' claim in token]"}
      """.trimIndent()
    )
  }

  @ParameterizedTest(name = "{displayName} ({argumentsWithNames})")
  @MethodSource("allReferralRequests")
  fun `requests with no auth token can never access anything`(request: Request) {
    val response = requestFactory.create(request, null, UUID.randomUUID().toString()).exchange()
    response.expectStatus().isUnauthorized
  }
}
