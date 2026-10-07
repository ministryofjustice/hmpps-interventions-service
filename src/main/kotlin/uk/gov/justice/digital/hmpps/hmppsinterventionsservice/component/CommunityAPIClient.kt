package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.component

import mu.KLogging
import net.logstash.logback.argument.StructuredArguments
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.WebClientResponseException.BadRequest
import reactor.core.publisher.Mono
import tools.jackson.core.JacksonException
import tools.jackson.databind.json.JsonMapper
import tools.jackson.databind.node.ObjectNode
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.exception.CommunityApiCallError

@Component
class CommunityAPIClient(
  private val communityApiClient: RestClient,
  private val jsonMapper: JsonMapper,
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

  fun <T : Any> makeSyncPostRequest(uri: String, requestBody: Any, responseBodyClass: Class<T>): T? = communityApiClient.post(uri, requestBody)
    .retrieve()
    .bodyToMono(responseBodyClass)
    .onErrorMap { e ->
      handleResponse(e, requestBody)
    }
    .block()

  fun handleResponse(e: Throwable, requestBody: Any): CommunityApiCallError {
    val responseBodyAsString = when (e) {
      is BadRequest -> e.responseBodyAsString
      else -> e.localizedMessage
    }

    val statusCode = when (e) {
      is WebClientResponseException -> e.statusCode
      else -> INTERNAL_SERVER_ERROR
    }

    val causeMessage = userMessageOrDeveloperMessageOrResponseBodyInThatOrder(responseBodyAsString)
    val error = CommunityApiCallError(statusCode, causeMessage, responseBodyAsString, e)
    logger.error(
      "Call to community api failed [${error.category}]",
      e,
      StructuredArguments.kv("req.body", requestBody),
      StructuredArguments.kv("res.body", responseBodyAsString),
      StructuredArguments.kv("res.causeMessage", causeMessage),
    )

    return error
  }

  private fun userMessageOrDeveloperMessageOrResponseBodyInThatOrder(responseBody: String): String {
    try {
      jsonMapper.readValue(responseBody, ObjectNode::class.java)?.let { node ->
        val userMessage = node.get("userMessage") ?: run {
          val developerMessage = node.get("developerMessage")
          return developerMessage.stringValue()
        }
        return userMessage.stringValue()
      }
      return responseBody
    } catch (e: JacksonException) {
      // response body does not contain json
      return responseBody
    }
  }
}
