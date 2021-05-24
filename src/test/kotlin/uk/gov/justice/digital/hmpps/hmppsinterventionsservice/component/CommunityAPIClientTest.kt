package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.component

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.OK
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.ExchangeFunction
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Hooks
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.ReferralEvent
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.ReferralEventType
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.SampleData
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.AppointmentCreateRequestDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.AppointmentResponseDTO
import java.lang.UnsupportedOperationException
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

class CommunityAPIClientTest {

  private val exchangeFunction = mock<ExchangeFunction>()
  private lateinit var communityAPIClient: CommunityAPIClient

  @Test
  fun `makes fire-and-forget post request successfully`() {
    communityAPIClient = CommunityAPIClient(
      WebClient.builder().exchangeFunction(exchangeFunction).build()
    )
    whenever(exchangeFunction.exchange(any())).thenReturn(Mono.just(ClientResponse.create(OK).build()))

    communityAPIClient.makeAsyncPostRequest("/uriValue", referralSentEvent)

    verify(exchangeFunction, times(1)).exchange(any())
    val requestCaptor = argumentCaptor<ClientRequest>()
    verify(exchangeFunction).exchange(requestCaptor.capture())
    val requestDetails = requestCaptor.firstValue

    assertThat("/uriValue").isEqualTo(requestDetails.url().toString())
  }

  @Test
  fun `makes async patch request successfully`() {

    communityAPIClient = CommunityAPIClient(
      WebClient.builder().exchangeFunction(exchangeFunction).build()
    )
    whenever(exchangeFunction.exchange(any())).thenReturn(Mono.empty())

    communityAPIClient.makeAsyncPatchRequest("/uriValue", referralSentEvent)

    verify(exchangeFunction, times(1)).exchange(any())
    val requestCaptor = argumentCaptor<ClientRequest>()
    verify(exchangeFunction).exchange(requestCaptor.capture())
    val requestDetails = requestCaptor.firstValue

    assertThat("/uriValue").isEqualTo(requestDetails.url().toString())
  }

  @Test
  fun `exceptions are propagated during fire-and-forget post request`() {
    val captured = mutableListOf<Throwable>()
    Hooks.onErrorDropped { e -> captured.add(e) }
    communityAPIClient = CommunityAPIClient(
      WebClient.builder().exchangeFunction(exchangeFunction).build()
    )

    val demoException = java.lang.RuntimeException("raised during test")
    whenever(exchangeFunction.exchange(any())).thenThrow(demoException)

    communityAPIClient.makeFireAndForgetPostRequest("/uriValue", referralSentEvent)
    assertThat(captured.first()).isInstanceOf(UnsupportedOperationException::class.java)
    assertThat(captured.first().cause).isEqualTo(demoException)
  }

  @Test
  fun `makes sync post request successfully`() {

    communityAPIClient = CommunityAPIClient(
      WebClient.builder().exchangeFunction(exchangeFunction).build()
    )
    val clientResponse: ClientResponse = ClientResponse
      .create(OK)
      .header("Content-Type", "application/json")
      .body("{\"appointmentId\":\"1234\"}")
      .build()
    whenever(exchangeFunction.exchange(any())).thenReturn(Mono.just(clientResponse))

    val response =
      communityAPIClient.makeSyncPostRequest("/uriValue", appointmentCreateRequest, AppointmentResponseDTO::class.java)

    assertThat(response.appointmentId).isEqualTo(1234L)

    verify(exchangeFunction, times(1)).exchange(any())
    val requestCaptor = argumentCaptor<ClientRequest>()
    verify(exchangeFunction).exchange(requestCaptor.capture())
    val requestDetails = requestCaptor.firstValue

    assertThat("/uriValue").isEqualTo(requestDetails.url().toString())
  }

  @Test
  fun `exceptions are thrown during sync post request`() {

    communityAPIClient = CommunityAPIClient(
      WebClient.builder().exchangeFunction(exchangeFunction).build()
    )
    whenever(exchangeFunction.exchange(any())).thenThrow(RuntimeException("A problem"))

    val exception = Assertions.assertThrows(RuntimeException::class.java) {
      communityAPIClient.makeSyncPostRequest("/uriValue", appointmentCreateRequest, AppointmentResponseDTO::class.java)
    }
    assertThat(exception.localizedMessage).isEqualTo("A problem")
  }

  @Test
  fun `propagates error response body on exception during sync post request`() {

    communityAPIClient = CommunityAPIClient(
      WebClient.builder().exchangeFunction(exchangeFunction).build()
    )
    val clientResponse: ClientResponse = ClientResponse
      .create(BAD_REQUEST)
      .header("Content-Type", "application/json")
      .body("There was a problem Houston")
      .build()
    whenever(exchangeFunction.exchange(any())).thenReturn(Mono.just(clientResponse))

    val exception = Assertions.assertThrows(WebClientResponseException::class.java) {
      communityAPIClient.makeSyncPostRequest("/uriValue", appointmentCreateRequest, AppointmentResponseDTO::class.java)
    }
    assertThat(exception.localizedMessage).isEqualTo("400 Bad Request from UNKNOWN ")
    assertThat(exception.responseBodyAsString).isEqualTo("There was a problem Houston")
  }

  private val referralSentEvent = ReferralEvent(
    "source",
    ReferralEventType.SENT,
    SampleData.sampleReferral(
      id = UUID.fromString("68df9f6c-3fcb-4ec6-8fcf-96551cd9b080"),
      referenceNumber = "HAS71263",
      crn = "X123456",
      relevantSentenceId = 123456,
      serviceProviderName = "Harmony Living",
      sentAt = OffsetDateTime.of(2020, 1, 1, 1, 1, 1, 0, ZoneOffset.of("+00:00"))
    ),
    "http://url"
  )

  private val appointmentCreateRequest = AppointmentCreateRequestDTO(
    contractType = "ACC",
    referralStart = OffsetDateTime.now(),
    referralId = UUID.fromString("68df9f6c-3fcb-4ec6-8fcf-96551cd9b080"),
    appointmentStart = OffsetDateTime.now(),
    appointmentEnd = OffsetDateTime.now(),
    officeLocationCode = "CRSEXTL",
    notes = "http://backLink",
    countsTowardsRarDays = true,
  )
}
