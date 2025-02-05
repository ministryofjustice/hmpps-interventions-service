package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service

import io.netty.channel.ConnectTimeoutException
import io.netty.handler.timeout.ReadTimeoutException
import mu.KLogging
import net.logstash.logback.argument.StructuredArguments
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.util.UriComponentsBuilder
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.util.retry.Retry
import reactor.util.retry.Retry.RetrySignal
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.component.RestClient
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.AuthUserDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AuthGroupID
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AuthUser

class UnverifiedEmailException : RuntimeException()

data class UserDetail(
  override val firstName: String,
  override val email: String,
  override val lastName: String,
) : ContactablePerson

private const val AUTH_SERVICE_PROVIDER_GROUP_PREFIX = "INT_SP_"

private data class AuthGroupResponse(
  val groupCode: String,
  val groupName: String,
)

private data class AuthUserDetailResponse(
  val firstName: String,
  val email: String,
  val verified: Boolean,
  val lastName: String,
)

private data class UserEmailResponse(
  val email: String,
)

private data class UserDetailResponse(
  val name: String,
)

@Service
@Transactional
class HMPPSAuthService(
  @Value("\${manage-users.api.locations.auth-user-groups}") private val authUserGroupsLocation: String,
  @Value("\${manage-users.api.locations.auth-user-detail}") private val authUserDetailLocation: String,
  @Value("\${manage-users.api.locations.user-email}") private val userEmailLocation: String,
  @Value("\${manage-users.api.locations.user-detail}") private val userDetailLocation: String,
  @Value("\${webclient.hmpps-auth.max-retry-attempts}") private val maxRetryAttempts: Long,
  private val mangeUsersAuthApiClient: RestClient,
) {
  companion object : KLogging()

  fun getUserGroups(user: AuthUser): List<AuthGroupID>? {
    val url = UriComponentsBuilder.fromPath(authUserGroupsLocation)
      .buildAndExpand(user.id)
      .toString()

    return mangeUsersAuthApiClient.get(url)
      .retrieve()
      .onStatus({ HttpStatus.NOT_FOUND == it }, { Mono.just(null) })
      .bodyToFlux(AuthGroupResponse::class.java)
      .withRetryPolicy()
      .map { it.groupCode }
      .collectList().block()
  }

  fun getUserDetail(user: AuthUser): UserDetail = getUserDetail(AuthUserDTO.from(user))

  fun getUserDetail(user: AuthUserDTO): UserDetail = if (user.authSource == "auth") {
    val url = UriComponentsBuilder.fromPath(authUserDetailLocation).buildAndExpand(user.username).toString()
    mangeUsersAuthApiClient.get(url)
      .retrieve()
      .bodyToMono(AuthUserDetailResponse::class.java)
      .withRetryPolicy()
      .map {
        if (!it.verified) {
          throw UnverifiedEmailException()
        }
        UserDetail(it.firstName, it.email, it.lastName)
      }
      .block()
  } else {
    val detailUrl = UriComponentsBuilder.fromPath(userDetailLocation).buildAndExpand(user.username).toString()
    val emailUrl = UriComponentsBuilder.fromPath(userEmailLocation).buildAndExpand(user.username).toString()
    Mono.zip(
      mangeUsersAuthApiClient.get(detailUrl)
        .retrieve()
        .bodyToMono(UserDetailResponse::class.java)
        .withRetryPolicy()
        .map { Pair(it.name.substringBefore(' '), it.name.substringAfterLast(' ')) },
      mangeUsersAuthApiClient.get(emailUrl)
        .retrieve()
        .onStatus({ it.equals(HttpStatus.NO_CONTENT) }, { Mono.error(UnverifiedEmailException()) })
        .bodyToMono(UserEmailResponse::class.java)
        .withRetryPolicy()
        .map { it.email },
    )
      .map { UserDetail(it.t1.first, it.t2, it.t1.second) }
      .block()
  }

  fun <T> Flux<T>.withRetryPolicy(): Flux<T> = this
    .retryWhen(
      Retry.max(maxRetryAttempts)
        .filter { isTimeoutException(it) }
        .doBeforeRetry { logRetrySignal(it) },
    )

  fun <T> Mono<T>.withRetryPolicy(): Mono<T> = this
    .retryWhen(
      Retry.max(maxRetryAttempts)
        .filter { isTimeoutException(it) }
        .doBeforeRetry { logRetrySignal(it) },
    )

  fun isTimeoutException(it: Throwable): Boolean {
    // Timeout for NO_RESPONSE is wrapped in a WebClientRequestException
    return it is ReadTimeoutException ||
      it is ConnectTimeoutException ||
      it.cause is ReadTimeoutException ||
      it.cause is ConnectTimeoutException
  }

  fun logRetrySignal(retrySignal: RetrySignal) {
    val exception = retrySignal.failure()?.cause.let { it } ?: retrySignal.failure()
    val message = exception.message ?: exception.javaClass.canonicalName
    logger.debug(
      "Retrying due to [$message]",
      exception,
      StructuredArguments.kv("res.causeMessage", message),
      StructuredArguments.kv("totalRetries", retrySignal.totalRetries()),
    )
  }
}
