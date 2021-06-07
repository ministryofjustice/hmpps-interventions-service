package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.config

import io.netty.handler.timeout.ReadTimeoutHandler
import io.netty.handler.timeout.WriteTimeoutHandler
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.component.RestClient
import java.time.Duration

@Configuration
class WebClientConfiguration(
  @Value("\${webclient.connect-timeout-seconds}") private val connectTimeoutSeconds: Long,
  @Value("\${webclient.read-timeout-seconds}") private val readTimeoutSeconds: Int,
  @Value("\${webclient.write-timeout-seconds}") private val writeTimeoutSeconds: Int,
  @Value("\${community-api.baseurl}") private val communityApiBaseUrl: String,
  @Value("\${hmppsauth.baseurl}") private val hmppsAuthBaseUrl: String,
  @Value("\${assess-risks-and-needs.baseurl}") private val assessRisksAndNeedsBaseUrl: String,
  private val webClientBuilder: WebClient.Builder
) {
  @Bean
  fun assessRisksAndNeedsClient(authorizedClientManager: OAuth2AuthorizedClientManager): WebClient {
    return createAuthorizedWebClient(authorizedClientManager, assessRisksAndNeedsBaseUrl)
  }

  @Bean
  @Deprecated("usage of newer 'RestClient' interface is preferred")
  fun communityApiWebClient(authorizedClientManager: OAuth2AuthorizedClientManager): WebClient {
    return createAuthorizedWebClient(authorizedClientManager, communityApiBaseUrl)
  }

  @Bean
  fun hmppsAuthApiWebClient(authorizedClientManager: OAuth2AuthorizedClientManager): WebClient {
    return createAuthorizedWebClient(authorizedClientManager, hmppsAuthBaseUrl)
  }

  @Bean
  fun communityApiRestClient(communityApiWebClient: WebClient): RestClient {
    return RestClient(communityApiWebClient)
  }

  @Bean
  fun authorizedClientManager(
    clientRegistrationRepository: ClientRegistrationRepository?,
    clientService: OAuth2AuthorizedClientService?
  ): OAuth2AuthorizedClientManager? {
    val authorizedClientProvider = OAuth2AuthorizedClientProviderBuilder.builder()
      .clientCredentials()
      .build()
    val authorizedClientManager = AuthorizedClientServiceOAuth2AuthorizedClientManager(
      clientRegistrationRepository,
      clientService
    )
    authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider)
    return authorizedClientManager
  }

  private fun createAuthorizedWebClient(clientManager: OAuth2AuthorizedClientManager, baseUrl: String): WebClient {
    val oauth2Client = ServletOAuth2AuthorizedClientExchangeFilterFunction(clientManager)
    oauth2Client.setDefaultClientRegistrationId("interventions-client")

    val httpClient = HttpClient.create()
      .doOnConnected {
        it
          .addHandlerLast(ReadTimeoutHandler(readTimeoutSeconds))
          .addHandlerLast(WriteTimeoutHandler(writeTimeoutSeconds))
      }
      .responseTimeout(Duration.ofSeconds(connectTimeoutSeconds))

    return webClientBuilder
      .clientConnector(ReactorClientHttpConnector(httpClient))
      .baseUrl(baseUrl)
      .apply(oauth2Client.oauth2Configuration())
      .build()
  }
}
