package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.integration

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Referral
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.JwtTokenFactory
import java.time.OffsetDateTime

class SASReferralControllerIntegrationTest : IntegrationTestBase() {

  private val tokenFactory = JwtTokenFactory()

  private fun createTokenWithRole(vararg roles: String): String = tokenFactory.createEncodedToken(
    userID = "sas-user-001",
    authSource = "delius",
    userName = "sas.testuser",
    roles = roles.toList(),
  )

  /** Sets the Accommodation service category on a referral and persists it. */
  private fun withAccommodationCategory(referral: Referral): Referral {
    val accommodationCategory = setupAssistant.serviceCategories["Accommodation"]
      ?: error("Accommodation service category not found in test DB; check migration data")
    referral.selectedServiceCategories = mutableSetOf(accommodationCategory)
    return referralRepository.save(referral)
  }

  @Test
  fun `returns 200 with LIVE referral for a valid CRN when sent referral has Accommodation category`() {
    val token = createTokenWithRole("ROLE_SAS_RM__REFERRAL_RO")
    withAccommodationCategory(setupAssistant.createSentReferral(serviceUserCRN = "X100001"))

    webTestClient.get()
      .uri("/sas-referral-details/X100001")
      .headers { it.setBearerAuth(token) }
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$[0].status").isEqualTo("LIVE")
      .jsonPath("$[0].referral.createdAt").isNotEmpty
      .jsonPath("$[0].referral.serviceProviders").isNotEmpty
  }

  @Test
  fun `returns 404 when sent referral exists but has no Accommodation service category`() {
    val token = createTokenWithRole("ROLE_SAS_RM__REFERRAL_RO")
    // CRN "X100001B" – referral created but selectedServiceCategories is null (not set)
    setupAssistant.createSentReferral(serviceUserCRN = "X100001B")

    webTestClient.get()
      .uri("/sas-referral-details/X100001B")
      .headers { it.setBearerAuth(token) }
      .exchange()
      .expectStatus().isNotFound
  }

  @Test
  fun `returns 200 with DRAFT status for a draft-only referral with Accommodation category`() {
    val token = createTokenWithRole("ROLE_SAS_RM__REFERRAL_RO")
    val accommodationCategory = setupAssistant.serviceCategories["Accommodation"]!!
    setupAssistant.createDraftReferral(
      serviceUserCRN = "X100002",
      selectedServiceCategories = mutableSetOf(accommodationCategory),
    )

    webTestClient.get()
      .uri("/sas-referral-details/X100002")
      .headers { it.setBearerAuth(token) }
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$[0].status").isEqualTo("DRAFT")
      .jsonPath("$[0].sentAt").doesNotExist()
      .jsonPath("$[0].sentBy").doesNotExist()
      .jsonPath("$[0].referral.createdAt").isNotEmpty
  }

  @Test
  fun `returns 404 when draft referral exists but has no Accommodation service category`() {
    val token = createTokenWithRole("ROLE_SAS_RM__REFERRAL_RO")
    // No selectedServiceCategories set → should be filtered out
    setupAssistant.createDraftReferral(serviceUserCRN = "X100002B")

    webTestClient.get()
      .uri("/sas-referral-details/X100002B")
      .headers { it.setBearerAuth(token) }
      .exchange()
      .expectStatus().isNotFound
  }

  @Test
  fun `returns 200 with COMPLETED status for a referral with concludedAt and end-of-service report`() {
    val token = createTokenWithRole("ROLE_SAS_RM__REFERRAL_RO")
    // createAssignedReferral defaults to CRN "X123456"
    val referral = setupAssistant.createAssignedReferral()
    referral.concludedAt = OffsetDateTime.now()
    referralRepository.save(referral)
    setupAssistant.createEndOfServiceReport(referral = referral)
    withAccommodationCategory(referral)

    webTestClient.get()
      .uri("/sas-referral-details/X123456")
      .headers { it.setBearerAuth(token) }
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$[0].status").isEqualTo("COMPLETED")
  }

  @Test
  fun `returns 200 with WITHDRAWN status for a referral with withdrawal info and Accommodation category`() {
    val token = createTokenWithRole("ROLE_SAS_RM__REFERRAL_RO")
    val referral = setupAssistant.createSentReferral(serviceUserCRN = "X100004")
    referral.withdrawalReasonCode = "MIS"
    referral.withdrawalComments = "Service user no longer needs this intervention"
    referralRepository.save(referral)
    withAccommodationCategory(referral)

    webTestClient.get()
      .uri("/sas-referral-details/X100004")
      .headers { it.setBearerAuth(token) }
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$[0].status").isEqualTo("WITHDRAWN")
  }

  @Test
  fun `returns multiple Accommodation referrals for the same CRN`() {
    val token = createTokenWithRole("ROLE_SAS_RM__REFERRAL_RO")
    val accommodationCategory = setupAssistant.serviceCategories["Accommodation"]!!
    // Sent referral – set Accommodation category
    withAccommodationCategory(setupAssistant.createSentReferral(serviceUserCRN = "X100005"))
    // Draft-only referral (different UUID) – pass Accommodation category directly
    setupAssistant.createDraftReferral(
      serviceUserCRN = "X100005",
      selectedServiceCategories = mutableSetOf(accommodationCategory),
    )

    webTestClient.get()
      .uri("/sas-referral-details/X100005")
      .headers { it.setBearerAuth(token) }
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.length()").isEqualTo(2)
  }

  @Test
  fun `returns 404 when no referrals found for CRN`() {
    val token = createTokenWithRole("ROLE_SAS_RM__REFERRAL_RO")

    webTestClient.get()
      .uri("/sas-referral-details/NOTEXIST99")
      .headers { it.setBearerAuth(token) }
      .exchange()
      .expectStatus().isNotFound
  }

  @Test
  fun `returns 401 when no token is provided`() {
    webTestClient.get()
      .uri("/sas-referral-details/X123456")
      .exchange()
      .expectStatus().isUnauthorized
  }

  @Test
  fun `returns 403 when token has wrong role`() {
    val token = createTokenWithRole("ROLE_PROBATION")

    webTestClient.get()
      .uri("/sas-referral-details/X123456")
      .headers { it.setBearerAuth(token) }
      .exchange()
      .expectStatus().isForbidden
  }

  @Test
  fun `returns 403 when token has CRS provider role but not SAS role`() {
    val token = createTokenWithRole("ROLE_CRS_PROVIDER")

    webTestClient.get()
      .uri("/sas-referral-details/X123456")
      .headers { it.setBearerAuth(token) }
      .exchange()
      .expectStatus().isForbidden
  }

  @Test
  fun `response includes sentAt and sentBy for sent referrals with Accommodation category`() {
    val token = createTokenWithRole("ROLE_SAS_RM__REFERRAL_RO")
    withAccommodationCategory(setupAssistant.createSentReferral(serviceUserCRN = "X100006"))

    webTestClient.get()
      .uri("/sas-referral-details/X100006")
      .headers { it.setBearerAuth(token) }
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$[0].sentAt").isNotEmpty
      .jsonPath("$[0].sentBy.username").isNotEmpty
      .jsonPath("$[0].sentBy.authSource").isNotEmpty
      .jsonPath("$[0].sentBy.userId").isNotEmpty
  }

  @Test
  fun `response includes service providers from prime provider`() {
    val token = createTokenWithRole("ROLE_SAS_RM__REFERRAL_RO")
    val intervention = setupAssistant.createIntervention(
      interventionTitle = "Test Intervention",
      serviceProviderId = "SP_TEST_001",
    )
    withAccommodationCategory(setupAssistant.createSentReferral(serviceUserCRN = "X100007", intervention = intervention))

    webTestClient.get()
      .uri("/sas-referral-details/X100007")
      .headers { it.setBearerAuth(token) }
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$[0].referral.serviceProviders[0].id").isEqualTo("SP_TEST_001")
      .jsonPath("$[0].referral.serviceProviders[0].name").isEqualTo("SP_TEST_001")
  }

  @Test
  fun `response includes subcontractor providers alongside prime provider`() {
    val token = createTokenWithRole("ROLE_SAS_RM__REFERRAL_RO")
    val contract = setupAssistant.createDynamicFrameworkContract(
      primeProviderId = "PRIME_001",
      subContractorServiceProviderIds = setOf("SUB_001"),
    )
    val intervention = setupAssistant.createIntervention(dynamicFrameworkContract = contract)
    withAccommodationCategory(setupAssistant.createSentReferral(serviceUserCRN = "X100008", intervention = intervention))

    webTestClient.get()
      .uri("/sas-referral-details/X100008")
      .headers { it.setBearerAuth(token) }
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$[0].referral.serviceProviders.length()").isEqualTo(2)
  }

  @Test
  fun `LIVE status returned when concludedAt is set but no end-of-service report`() {
    val token = createTokenWithRole("ROLE_SAS_RM__REFERRAL_RO")
    val referral = setupAssistant.createSentReferral(serviceUserCRN = "X100009")
    referral.concludedAt = OffsetDateTime.now()
    referralRepository.save(referral)
    withAccommodationCategory(referral)

    webTestClient.get()
      .uri("/sas-referral-details/X100009")
      .headers { it.setBearerAuth(token) }
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$[0].status").isEqualTo("LIVE")
  }
}
