package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.component

import mu.KLogging
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.retry.RetryCallback
import org.springframework.retry.RetryContext
import org.springframework.retry.backoff.FixedBackOffPolicy
import org.springframework.retry.listener.RetryListenerSupport
import org.springframework.retry.policy.SimpleRetryPolicy
import org.springframework.retry.support.RetryTemplate
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient
import org.springframework.security.oauth2.client.endpoint.OAuth2ClientCredentialsGrantRequest
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse
import org.springframework.stereotype.Component

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

  private val retryLogger = object : RetryListenerSupport() {
    override fun <T : Any?, E : Throwable?> onError(context: RetryContext?, callback: RetryCallback<T, E>?, throwable: Throwable?) {
      logger.info("token request failed; retrying", throwable)
    }
  }

  private val retryTemplate = RetryTemplate().apply {
    setBackOffPolicy(
      FixedBackOffPolicy().apply {
        backOffPeriod = config.retryDelayMs
      },
    )
    setRetryPolicy(SimpleRetryPolicy(config.retries))
    setListeners(arrayOf(retryLogger))
  }

  private val delegate by lazy {
    try {
      // Try to get the default implementation from Spring Security
      Class.forName("org.springframework.security.oauth2.client.endpoint.DefaultClientCredentialsTokenResponseClient")
        .getDeclaredConstructor()
        .newInstance() as OAuth2AccessTokenResponseClient<OAuth2ClientCredentialsGrantRequest>
    } catch (e: Exception) {
      // If DefaultClientCredentialsTokenResponseClient is not available in Spring Security 7.1.0,
      // create a simple wrapper that delegates to RestClient
      object : OAuth2AccessTokenResponseClient<OAuth2ClientCredentialsGrantRequest> {
        override fun getTokenResponse(authorizationGrantRequest: OAuth2ClientCredentialsGrantRequest): OAuth2AccessTokenResponse {
          throw UnsupportedOperationException("OAuth2 client credentials token response not configured. Check Spring Security version and dependencies.")
        }
      }
    }
  }

  override fun getTokenResponse(authorizationGrantRequest: OAuth2ClientCredentialsGrantRequest): OAuth2AccessTokenResponse {
    return retryTemplate.execute<OAuth2AccessTokenResponse, Exception> {
      delegate.getTokenResponse(authorizationGrantRequest)
    }!!
  }
}
