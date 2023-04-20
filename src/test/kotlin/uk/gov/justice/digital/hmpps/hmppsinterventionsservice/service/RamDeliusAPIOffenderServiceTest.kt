package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.mock
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.component.RestClient
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.config.InvalidAssumptionError

internal class RamDeliusAPIOffenderServiceTest {
  private val responsibleOfficerLocation = "responsible-officer"
  private val telemetryService = TelemetryService(mock())

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

  private fun offenderServiceFactory(restClient: RestClient): RamDeliusAPIOffenderService {
    return RamDeliusAPIOffenderService(responsibleOfficerLocation, restClient, telemetryService)
  }

  @Test
  fun `getResponsibleOfficerDetails returns when there are is a responsible officer`() {
    val offenderService = offenderServiceFactory(
      createMockedRestClient(
        MockedResponse(
          responsibleOfficerLocation,
          HttpStatus.OK,
          "{\n" +
            "  \"communityManager\": {\n" +
            "    \"code\": \"123\",\n" +
            "    \"name\": {\n" +
            "      \"forename\": \"Dan\",\n" +
            "      \"surname\": \"smith\"\n" +
            "    },\n" +
            "    \"username\": \"abcdef\",\n" +
            "    \"email\": \"dan.smith@gmail.com\",\n" +
            "    \"responsibleOfficer\": true\n" +
            "  }\n" +
            "}",
        ),
      ),
    )

    val responsibleOfficerDetails = offenderService.getResponsibleOfficerDetails("X123456")
    assertThat(responsibleOfficerDetails?.communityManager?.name?.forename).isEqualTo("Dan")
    assertThat(responsibleOfficerDetails?.communityManager?.code).isEqualTo("123")
    assertThat(responsibleOfficerDetails?.communityManager?.responsibleOfficer).isTrue
  }

  @Test
  fun `getResponsibleOfficerDetails fails when there are no responsible officers`() {
    val offenderService = offenderServiceFactory(
      createMockedRestClient(
        MockedResponse(
          responsibleOfficerLocation,
          HttpStatus.OK,
          "{\n" +
            "  \"communityManager\": {\n" +
            "    \"code\": \"123\",\n" +
            "    \"name\": {\n" +
            "      \"forename\": \"Dan\",\n" +
            "      \"surname\": \"smith\"\n" +
            "    },\n" +
            "    \"username\": \"abcdef\",\n" +
            "    \"email\": \"dan.smith@gmail.com\",\n" +
            "    \"responsibleOfficer\": false\n" +
            "  }\n" +
            "}",
        ),
      ),
    )

    assertThrows<InvalidAssumptionError> {
      offenderService.getResponsibleOfficerDetails("X123456")
    }
  }

  @Test
  fun `getResponsibleOfficerDetails throws not found`() {
    val offenderService = offenderServiceFactory(createMockedRestClient(MockedResponse(responsibleOfficerLocation, HttpStatus.NOT_FOUND, "")))
    assertThrows<InvalidAssumptionError> {
      offenderService.getResponsibleOfficerDetails("X123456")
    }
  }

  @Test
  fun `getResponsibleOfficerDetails throws unhandled error`() {
    val offenderService = offenderServiceFactory(createMockedRestClient(MockedResponse(responsibleOfficerLocation, HttpStatus.INTERNAL_SERVER_ERROR, "")))
    assertThrows<WebClientResponseException> {
      offenderService.getResponsibleOfficerDetails("X123456")
    }
  }
}
