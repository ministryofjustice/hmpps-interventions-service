package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.component

import mu.KLogging
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

@Component
class CommunityAPIClient(
  private val communityApiWebClient: WebClient,
) {
  companion object : KLogging()

  fun withCustomAuth(authentication: JwtAuthenticationToken): CommunityAPIClient {
    return CommunityAPIClient(
      communityApiWebClient.mutate()
        .defaultHeaders { headers -> headers.setBearerAuth(authentication.token.tokenValue) }
        .build()
    )
  }

  fun makeFireAndForgetPostRequest(uri: String, requestBody: Any) {
    communityApiWebClient.post().uri(uri)
      .body(Mono.just(requestBody), requestBody::class.java)
      .retrieve()
      .bodyToMono(Unit::class.java)
      .subscribe()
  }

  fun makeAsyncPatchRequest(uri: String, requestBody: Any) {
    communityApiWebClient.patch().uri(uri)
      .body(Mono.just(requestBody), requestBody::class.java)
      .retrieve()
      .bodyToMono(Unit::class.java)
      .subscribe()
  }

  fun <T : Any> makeSyncPostRequest(uri: String, requestBody: Any, responseBodyClass: Class<T>): T {
    return communityApiWebClient.post().uri(uri)
      .body(Mono.just(requestBody), requestBody::class.java)
      .retrieve()
      .bodyToMono(responseBodyClass)
      .block()
  }

  fun <T : Any> makeSyncGetRequest(uri: String, responseBodyClass: Class<T>): T {
    return communityApiWebClient.get().uri(uri)
      .retrieve()
      .bodyToMono(responseBodyClass)
      .block()
  }
}
