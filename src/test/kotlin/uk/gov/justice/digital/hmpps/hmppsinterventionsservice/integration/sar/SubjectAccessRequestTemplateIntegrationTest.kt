package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.integration.sar

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.JwtTokenFactory

class SubjectAccessRequestTemplateIntegrationTest : IntegrationTestBase() {

  private val tokenFactory = JwtTokenFactory()

  @Test
  fun `GET subject-access-request template returns mustache template when authenticated with SAR role`() {
    val token = tokenFactory.createEncodedToken(roles = listOf("ROLE_SAR_DATA_ACCESS"))

    val response = webTestClient.get()
      .uri("/subject-access-request/template")
      .header("Authorization", "Bearer $token")
      .exchange()

    response.expectStatus().isOk
    val body = response.expectBody(String::class.java).returnResult().responseBody
    assertThat(body).isNotBlank
    // structural mustache markers
    assertThat(body).contains("{{#referral}}")
    assertThat(body).contains("{{#draft_referral}}")
    // template helper functions
    assertThat(body).contains("{{ formatDate")
    assertThat(body).contains("{{ optionalString")
    assertThat(body).contains("{{ optionalValue")
    assertThat(body).contains("{{ convertBoolean")
  }

  @Test
  fun `GET subject-access-request template returns 401 without auth token`() {
    webTestClient.get()
      .uri("/subject-access-request/template")
      .exchange()
      .expectStatus().isEqualTo(HttpStatus.UNAUTHORIZED)
  }

  @Test
  fun `GET subject-access-request template returns 403 without required role`() {
    val token = tokenFactory.createEncodedToken(roles = listOf("ROLE_SOME_OTHER_ROLE"))

    webTestClient.get()
      .uri("/subject-access-request/template")
      .header("Authorization", "Bearer $token")
      .exchange()
      .expectStatus().isForbidden
  }
}
