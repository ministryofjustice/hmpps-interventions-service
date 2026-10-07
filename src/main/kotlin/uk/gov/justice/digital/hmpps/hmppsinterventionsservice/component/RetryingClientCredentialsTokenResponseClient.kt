package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.component

import mu.KLogging
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder
import org.springframework.boot.http.client.HttpClientSettings
import org.springframework.core.retry.RetryPolicy
import org.springframework.core.retry.RetryTemplate
import org.springframework.http.converter.FormHttpMessageConverter
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient
import org.springframework.security.oauth2.client.endpoint.OAuth2ClientCredentialsGrantRequest
import org.springframework.security.oauth2.client.endpoint.RestClientClientCredentialsTokenResponseClient
import org.springframework.security.oauth2.client.http.OAuth2ErrorResponseErrorHandler
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse
import org.springframework.security.oauth2.core.http.converter.OAuth2AccessTokenResponseHttpMessageConverter
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.time.Duration
import java.util.function.Supplier

@ConfigurationProperties(prefix = "spring.security.oauth2.client.provider.hmppsauth.token-request")
data class TokenRequestConfig(
  val connectTimeoutMs: Long,
  val readTimeoutMs: Long,
  val retries: Int,
  val retryDelayMs: Long,
)

@Component
@EnableConfigurationProperties(TokenRequestConfig::class)
class RetryingClientCredentialsTokenResponseClient(
  private val config: TokenRequestConfig,
) : OAuth2AccessTokenResponseClient<OAuth2ClientCredentialsGrantRequest> {
  companion object : KLogging()

  private val retryTemplate = RetryTemplate(
    RetryPolicy.builder()
      // `retries` is the total number of attempts, whereas maxRetries excludes the first attempt
      .maxRetries((config.retries - 1L).coerceAtLeast(0))
      .delay(Duration.ofMillis(config.retryDelayMs))
      .build(),
  )

  private val customizedClient = RestClientClientCredentialsTokenResponseClient().apply {
    // see https://docs.spring.io/spring-security/reference/servlet/oauth2/client/authorization-grants.html#oauth2-client-client-credentials-access-token-response-client
    // for information on how to configure the defaults in this RestClient.
    setRestClient(
      RestClient.builder()
        .requestFactory(
          ClientHttpRequestFactoryBuilder.detect().build(
            HttpClientSettings.defaults().withTimeouts(
              Duration.ofMillis(config.connectTimeoutMs),
              Duration.ofMillis(config.readTimeoutMs),
            ),
          ),
        )
        .configureMessageConverters { converters ->
          converters
            .disableDefaults()
            .addCustomConverter(FormHttpMessageConverter())
            .addCustomConverter(OAuth2AccessTokenResponseHttpMessageConverter())
        }
        .defaultStatusHandler(OAuth2ErrorResponseErrorHandler())
        .build(),
    )
  }

  // invoke() rethrows the last failure once retries are exhausted, so callers still see the OAuth2 exception
  override fun getTokenResponse(authorizationGrantRequest: OAuth2ClientCredentialsGrantRequest): OAuth2AccessTokenResponse = retryTemplate.invoke(
    Supplier {
      try {
        customizedClient.getTokenResponse(authorizationGrantRequest)
      } catch (e: Exception) {
        logger.info("token request failed; retrying", e)
        throw e
      }
    },
  )
}
