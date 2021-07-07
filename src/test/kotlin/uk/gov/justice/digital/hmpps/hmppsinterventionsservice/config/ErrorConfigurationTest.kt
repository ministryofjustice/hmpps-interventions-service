package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.config

import com.microsoft.applicationinsights.TelemetryClient
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod.POST
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.CONFLICT
import org.springframework.http.HttpStatus.MOVED_PERMANENTLY
import org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE
import org.springframework.web.reactive.function.client.WebClientRequestException
import org.springframework.web.reactive.function.client.WebClientResponseException
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.exception.DownstreamApiCallError
import java.lang.RuntimeException
import java.net.URI
import java.nio.charset.Charset

internal class ErrorConfigurationTest {

  private val telemetryClient = mock<TelemetryClient>()
  private val errorConfiguration = ErrorConfiguration(telemetryClient)

  @Test
  fun `augments web client request exception`() {

    val exception = WebClientRequestException(IllegalStateException("An Error"), POST, URI.create("uri"), HttpHeaders.EMPTY)

    val response = errorConfiguration.handleWebClientRequestException(exception)

    val responseBody = response.body
    assertThat(responseBody.status).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value())
    assertThat(responseBody.error).isEqualTo("Call to dependency request exception")
    assertThat(responseBody.message).isEqualTo("An Error; nested exception is java.lang.IllegalStateException: An Error")
    assertThat(responseBody.userMessage).isEqualTo("System is experiencing issues. Please try again later and if the issue persists contact Support")
  }

  @Test
  fun `augments web client response exception`() {

    val exception = WebClientResponseException(BAD_REQUEST.value(), "Reason Phrase", HttpHeaders.EMPTY, "An Error".toByteArray(), Charset.defaultCharset())

    val response = errorConfiguration.handleWebClientResponseException(exception)

    val responseBody = response.body
    assertThat(responseBody.status).isEqualTo(BAD_REQUEST.value())
    assertThat(responseBody.error).isEqualTo("Call to dependency response exception")
    assertThat(responseBody.message).isEqualTo("An Error")
    assertThat(responseBody.userMessage).isEqualTo("Problem has been encountered. Please contact Support")
  }

  @Test
  fun `user message is mapped correctly`() {

    assertThat(errorConfiguration.userMessageForWebClientException(null)).isNull()
    assertThat(errorConfiguration.userMessageForWebClientException(CONFLICT)).isNull()
    assertThat(errorConfiguration.userMessageForWebClientException(BAD_REQUEST))
      .isEqualTo("Problem has been encountered. Please contact Support")
    assertThat(errorConfiguration.userMessageForWebClientException(SERVICE_UNAVAILABLE))
      .isEqualTo("System is experiencing issues. Please try again later and if the issue persists contact Support")
    assertThat(errorConfiguration.userMessageForWebClientException(MOVED_PERMANENTLY)).isNull()
  }

  @Test
  fun `handles downstream api call error`() {

    val error = DownstreamApiCallError(BAD_REQUEST, "category of error", "{\"name\",\"value\"}", RuntimeException("An exception"))

    val response = errorConfiguration.handleDownstreamApiCallError(error).body!!

    assertThat(response.status).isEqualTo(BAD_REQUEST.value())
    assertThat(response.error).isEqualTo("category of error")
    assertThat(response.userMessage).isEqualTo("A problem has occurred. Please contact support")
    assertThat(response.message).isEqualTo("{\"name\",\"value\"}")

    verify(telemetryClient).trackEvent(
      "InterventionsDownstreamAPICallError",
      mapOf(
        "category" to "category of error",
        "userMessage" to "A problem has occurred. Please contact support"
      ),
      null
    )
  }
}
