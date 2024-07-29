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

internal class RamDeliusAPIConvictionServiceTest {
  private val convictionLocation = "convictions"
  private val telemetryService = mock<TelemetryService>()

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

  private fun convictionServiceFactory(restClient: RestClient): RamDeliusAPIConvictionService {
    return RamDeliusAPIConvictionService(convictionLocation, restClient, telemetryService)
  }

  @Test
  fun `getConvictionDetails returns when there are is a matching crn and conviction`() {
    val convictionService = convictionServiceFactory(
      createMockedRestClient(
        MockedResponse(
          convictionLocation,
          HttpStatus.OK,
          "{\n" +
            " \"caseDetail\": {\n" +
            " \"crn\": \"string\",\n" +
            " \"name\": {\n" +
            " \"forename\": \"string\",\n" +
            " \"surname\": \"string\"\n" +
            "},\n" +
            "\"dateOfBirth\": \"2024-07-24\",\n" +
            "\"gender\": \"string\",\n" +
            "\"profile\": {\n" +
            "\"primaryLanguage\": \"string\",\n" +
            "\"ethnicity\": \"string\",\n" +
            "\"religion\": \"string\",\n" +
            "\"disabilities\": [\n" +
            "{\n" +
            " \"type\": \"string\",\n" +
            " \"startDate\": \"2024-07-24\",\n" +
            " \"notes\": \"string\"\n" +
            "}\n" +
            "]\n" +
            "},\n" +
            "\"contactDetails\": {\n" +
            " \"noFixedAbode\": true,\n" +
            " \"mainAddress\": {\n" +
            " \"buildingName\": \"string\",\n" +
            " \"buildingNumber\": \"string\",\n" +
            " \"streetName\": \"string\",\n" +
            " \"district\": \"string\",\n" +
            " \"town\": \"string\",\n" +
            " \"county\": \"string\",\n" +
            " \"postcode\": \"string\"\n" +
            "}," +
            "\"emailAddress\": \"string\",\n" +
            "\"telephoneNumber\": \"string\",\n" +
            "\"mobileNumber\": \"string\"\n" +
            "}\n" +
            "},\n" +
            "\"conviction\": {\n" +
            " \"id\": 123456,\n" +
            " \"date\": \"2024-07-24\",\n" +
            " \"sentence\": {\n" +
            "   \"description\": \"string\",\n" +
            "   \"expectedEndDate\": \"2024-01-01\"\n" +
            " },\n" +
            " \"mainOffence\": {\n" +
            " \"category\": \"string\",\n" +
            " \"subCategory\": \"string\"\n" +
            "},\n" +
            "\"active\": true\n" +
            "}\n" +
            "}",
        ),
      ),
    )

    val convictionDetails = convictionService.getConvictionDetails("X123456", 123456L)
    assertThat(convictionDetails?.conviction?.id).isEqualTo(123456L)
    assertThat(convictionDetails?.conviction?.sentence?.expectedEndDate).isEqualTo("2024-01-01")
  }

  @Test
  fun `getConvictionDetails throws unhandled error`() {
    val convictionService = convictionServiceFactory(createMockedRestClient(MockedResponse(convictionLocation, HttpStatus.INTERNAL_SERVER_ERROR, "")))
    assertThrows<WebClientResponseException> {
      convictionService.getConvictionDetails("X123456", 123456L)
    }
  }

  @Test
  fun `getConvictionDetails throws error when not found`() {
    val convictionService = convictionServiceFactory(createMockedRestClient(MockedResponse(convictionLocation, HttpStatus.NOT_FOUND, "")))
    assertThrows<WebClientResponseException> {
      convictionService.getConvictionDetails("X123456", 654321L)
    }
  }
}
