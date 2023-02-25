package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.component.RestClient
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.AuthUserFactory

data class MockedResponse(
  val path: String,
  val status: HttpStatusCode,
  val responseBody: String,
)

internal class CommunityAPIOffenderServiceTest {
  private val user = AuthUserFactory().create()
  private val offenderAccessLocation = "case-access"
  private val managedOffendersLocation = "managed-cases"
  private val offenderIdentifiersLocation = "case-identifiers"

  private fun createMockedRestClient(vararg responses: MockedResponse): RestClient {
    return RestClient(
      WebClient.builder()
        .exchangeFunction exchange@{ req ->
          responses.forEach {
            if (req.url().path == it.path) {
              return@exchange Mono.just(
                ClientResponse.create(it.status)
                  .header("content-type", "application/json")
                  .body(it.responseBody)
                  .build(),
              )
            }
          }
          Mono.empty()
        }
        .build(),
      "client-registration-id",
    )
  }

  private fun offenderServiceFactory(restClient: RestClient): CommunityAPIOffenderService {
    return CommunityAPIOffenderService(offenderAccessLocation, managedOffendersLocation, offenderIdentifiersLocation, restClient)
  }

  @Test
  fun `checkIfAuthenticatedDeliusUserHasAccessToServiceUser success`() {
    val response = MockedResponse(offenderAccessLocation, HttpStatus.OK, "{\"access\":[{\"crn\":\"X123456\", \"userExcluded\":false, \"userRestricted\":false, \"exclusionMessage\": null, \"restrictionMessage\": null}]}")
    val offenderService = offenderServiceFactory(createMockedRestClient(response))
    val result = offenderService.checkIfAuthenticatedDeliusUserHasAccessToServiceUser(user, "X123456")
    assertThat(result.canAccess).isTrue
    assertThat(result.messages).isEmpty()
  }

  @Test
  fun `checkIfAuthenticatedDeliusUserHasAccessToServiceUser limited access`() {
    val response = MockedResponse(offenderAccessLocation, HttpStatus.OK, "{\"access\":[{\"crn\":\"X123456\", \"userExcluded\":true, \"userRestricted\":false, \"exclusionMessage\": \"family exclusion\", \"restrictionMessage\": null}]}")
    val offenderService = offenderServiceFactory(createMockedRestClient(response))
    val result = offenderService.checkIfAuthenticatedDeliusUserHasAccessToServiceUser(user, "X123456")
    assertThat(result.canAccess).isFalse
    assertThat(result.messages).isEqualTo(listOf("family exclusion"))
  }

  @Test
  fun `getManagedOffendersForDeliusUser returns list of offenders`() {
    val offendersResponse = MockedResponse(managedOffendersLocation, HttpStatus.OK, "{\"managedCases\": [{\"crn\": \"CRN123\"}, {\"crn\": \"CRN456\"}]}")
    val offenderService = offenderServiceFactory(createMockedRestClient(offendersResponse))
    val result = offenderService.getManagedOffendersForDeliusUser(user)
    assertThat(result).isEqualTo(listOf(Offender("CRN123"), Offender("CRN456")))
  }
}
