package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.component

import mu.KLogging
import net.logstash.logback.argument.StructuredArguments
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClientResponseException.BadRequest
import reactor.core.publisher.Mono

@Component
class CommunityAPIClient(
  private val communityApiClient: RestClient,
) {
  companion object : KLogging()

  fun makeAsyncPostRequest(uri: String, requestBody: Any) {
    communityApiClient.post(uri, requestBody)
      .retrieve()
      .bodyToMono(Unit::class.java)
      .onErrorResume { e ->
        handleResponse(e, requestBody)
        Mono.empty()
      }
      .subscribe()
  }

  fun makeAsyncPatchRequest(uri: String, requestBody: Any) {
    communityApiClient.patch(uri, requestBody)
      .retrieve()
      .bodyToMono(Unit::class.java)
      .onErrorResume { e ->
        handleResponse(e, requestBody)
        Mono.empty()
      }
      .subscribe()
  }

  fun <T : Any> makeSyncPostRequest(uri: String, requestBody: Any, responseBodyClass: Class<T>): T {
    return communityApiClient.post(uri, requestBody)
      .retrieve()
      .bodyToMono(responseBodyClass)
      .onErrorMap { e ->
        handleResponse(e, requestBody)
        e
      }
      .block()
  }

  fun <T : Any> makeSyncGetRequest(uri: String, responseBodyClass: Class<T>): T {
    return communityApiClient.get(uri)
      .retrieve()
      .bodyToMono(responseBodyClass)
      .onErrorMap { e ->
        handleResponse(e, "")
        e
      }
      .block()
  }

  fun handleResponse(e: Throwable, requestBody: Any): String {
    val responseBodyAsString = when (e) {
      is BadRequest -> e.responseBodyAsString
      else -> e.localizedMessage
    }
    logger.error(
      "Call to community api failed",
      e,
      StructuredArguments.kv("req.body", requestBody),
      StructuredArguments.kv("res.body", responseBodyAsString)
    )
    return responseBodyAsString.let { it } ?: "No response body message"
  }
}
