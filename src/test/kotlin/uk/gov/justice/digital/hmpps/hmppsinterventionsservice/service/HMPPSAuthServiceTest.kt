package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service

import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.component.RestClient
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AuthUser

class HMPPSAuthServiceTest {
  private val mockWebServer = MockWebServer()
  private val hmppsAuthService = HMPPSAuthService(
    "/authuser/groups",
    "/authuser/detail",
    "/user/email",
    "/user/detail",
    0L,
    RestClient(
      WebClient.create(
        mockWebServer.url("/").toString(),
      ),
      "client-registration-id",
    ),
  )

  @Test
  fun `getUserDetail gets details for verified auth user`() {
    mockWebServer.enqueue(
      MockResponse()
        .setHeader("content-type", "application/json")
        .setBody(
          """
          {
            "email": "tom@tom.tom",
            "verified": true,
            "firstName": "tom",
            "lastName": "tom"
          }
          """.trimIndent(),
        ),
    )
    val detail = hmppsAuthService.getUserDetail(AuthUser("id", "auth", "username"))

    assertThat(mockWebServer.takeRequest().path).isEqualTo("/authuser/detail")
    assertThat(detail.email).isEqualTo("tom@tom.tom")
    assertThat(detail.firstName).isEqualTo("tom")
  }

  @Test
  fun `getUserDetail fails for unverified auth user`() {
    mockWebServer.enqueue(
      MockResponse()
        .setHeader("content-type", "application/json")
        .setBody(
          """
          {
            "email": "tom@tom.tom",
            "verified": false,
            "firstName": "tom",
            "lastName": "tom"
          }
          """.trimIndent(),
        ),
    )
    assertThrows<UnverifiedEmailException> {
      hmppsAuthService.getUserDetail(AuthUser("id", "auth", "username"))
    }
  }

  @Test
  fun `getUserDetail gets details for delius user`() {
    mockWebServer.dispatcher = object : Dispatcher() {
      override fun dispatch(request: RecordedRequest): MockResponse = when (request.path) {
        "/user/email" ->
          MockResponse()
            .setHeader("content-type", "application/json")
            .setBody(
              """
              {
                "email": "tom@tom.tom"
              }
              """.trimIndent(),
            )
        "/user/detail" ->
          MockResponse()
            .setHeader("content-type", "application/json")
            .setBody(
              """
              {
                "name": "tom timothy"
              }
              """.trimIndent(),
            )
        else -> MockResponse().setResponseCode(404)
      }
    }
    val detail = hmppsAuthService.getUserDetail(AuthUser("id", "delius", "username"))

    assertThat(detail.email).isEqualTo("tom@tom.tom")
    assertThat(detail.firstName).isEqualTo("tom")
  }

  @Test
  fun `getUserDetail fails for email 204 responses`() {
    mockWebServer.dispatcher = object : Dispatcher() {
      override fun dispatch(request: RecordedRequest): MockResponse = when (request.path) {
        "/user/email" -> MockResponse().setResponseCode(204)
        "/user/detail" ->
          MockResponse()
            .setHeader("content-type", "application/json")
            .setBody(
              """
              {
                "name": "tom timothy"
              }
              """.trimIndent(),
            )
        else -> MockResponse().setResponseCode(404)
      }
    }

    assertThrows<UnverifiedEmailException> {
      val detail = hmppsAuthService.getUserDetail(AuthUser("id", "delius", "username"))
    }
  }

  @Test
  fun `getUserDetail propagates http errors for delius users`() {
    mockWebServer.enqueue(MockResponse().setResponseCode(500))
    assertThrows<WebClientResponseException> {
      val detail = hmppsAuthService.getUserDetail(AuthUser("id", "delius", "username"))
    }
  }

  @Test
  fun `getUserDetail propagates http errors for auth users`() {
    mockWebServer.enqueue(MockResponse().setResponseCode(500))
    assertThrows<WebClientResponseException> {
      val detail = hmppsAuthService.getUserDetail(AuthUser("id", "auth", "username"))
    }
  }
}
