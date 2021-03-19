package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.component

import mu.KLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

@Component
class CommunityAPIClient(
  @Qualifier(value = "communityApiWebClient") private val communityAPIWebClient: WebClient,
) {
  companion object : KLogging()

  fun makeRequest(uri: String, requestBody: Any) {
    communityAPIWebClient.post().uri(uri)
      .body(Mono.just(requestBody), requestBody::class.java)
      .retrieve()
      .bodyToMono(Unit::class.java)
      .onErrorResume { e ->
        logger.error("Call to community api failed for: $requestBody", e)
        Mono.empty()
      }
      .subscribe()
  }
}
