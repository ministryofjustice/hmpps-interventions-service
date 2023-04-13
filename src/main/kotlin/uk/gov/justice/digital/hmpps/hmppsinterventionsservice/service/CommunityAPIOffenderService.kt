package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.util.UriComponentsBuilder
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.component.RestClient
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AuthUser

data class Offender(
  val crnNumber: String,
)

data class ServiceUserAccessResult(
  val canAccess: Boolean,
  val messages: List<String>,
)

private data class UserAccessResponse(
  val exclusionMessage: String?,
  val restrictionMessage: String?,
)

private data class StaffDetailsResponse(
  val staffIdentifier: Long,
)

data class OffenderIdentifiersResponse(
  val primaryIdentifiers: PrimaryIdentifiersResponse?,
)

data class PrimaryIdentifiersResponse(
  val nomsNumber: String?,
)

data class OfficerDetails(
  val communityOfficer: CommunityOfficer,
)

data class CommunityOfficer(
  val code: String,
  val name: OfficerName,
  val username: String,
  val email: String?,
  val responsibleOfficer: Boolean,
)

data class OfficerName(
  val forename: String,
  val surname: String,
)

@Service
class CommunityAPIOffenderService(
  @Value("\${community-api.locations.offender-access}") private val offenderAccessLocation: String,
  @Value("\${community-api.locations.managed-offenders}") private val managedOffendersLocation: String,
  @Value("\${community-api.locations.staff-details}") private val staffDetailsLocation: String,
  @Value("\${community-api.locations.offender-managers}") private val offenderManagersLocation: String,
  @Value("\${community-api.locations.offender-identifiers}") private val offenderIdentifiersLocation: String,
  @Value("\${refer-and-monitor-and-delius.locations.responsible-officer}") private val responsibleOfficerLocation: String,
  private val communityApiClient: RestClient,
  private val telemetryService: TelemetryService,
) {
  fun checkIfAuthenticatedDeliusUserHasAccessToServiceUser(user: AuthUser, crn: String): ServiceUserAccessResult {
    val userAccessPath = UriComponentsBuilder.fromPath(offenderAccessLocation)
      .buildAndExpand(crn, user.userName)
      .toString()

    val response = communityApiClient.get(userAccessPath)
      .retrieve()
      .onStatus({ it.equals(HttpStatus.FORBIDDEN) }, { Mono.empty() })
      .toEntity(UserAccessResponse::class.java)
      .block()

    return ServiceUserAccessResult(
      !response.statusCode.equals(HttpStatus.FORBIDDEN),
      listOfNotNull(response.body.restrictionMessage, response.body.exclusionMessage),
    )
  }

  fun getManagedOffendersForDeliusUser(user: AuthUser): List<Offender> {
    val staffIdentifier = getStaffIdentifier(user)
      // if a delius user does not have a staff identifier it's not an error;
      // but it does mean they do not have any managed offenders allocated.
      ?: return emptyList()

    val managedOffendersPath = UriComponentsBuilder.fromPath(managedOffendersLocation)
      .buildAndExpand(staffIdentifier)
      .toString()

    return communityApiClient.get(managedOffendersPath)
      .retrieve()
      .bodyToFlux(Offender::class.java)
      .collectList()
      .block()
  }

  fun getStaffIdentifier(user: AuthUser): Long? {
    val staffDetailsPath = UriComponentsBuilder.fromPath(staffDetailsLocation)
      .buildAndExpand(user.userName)
      .toString()

    return communityApiClient.get(staffDetailsPath)
      .retrieve()
      .bodyToMono(StaffDetailsResponse::class.java)
      .onErrorResume(WebClientResponseException::class.java) { e ->
        when (e.statusCode) {
          // not all delius users are staff
          HttpStatus.NOT_FOUND -> Mono.empty()
          else -> Mono.error(e)
        }
      }
      .block()
      ?.staffIdentifier
  }

  fun getOffenderIdentifiers(crn: String): Mono<OffenderIdentifiersResponse> {
    val offenderIdentifiersPath = UriComponentsBuilder.fromPath(offenderIdentifiersLocation)
      .buildAndExpand(crn)
      .toString()

    return communityApiClient.get(offenderIdentifiersPath)
      .retrieve()
      .bodyToMono(OffenderIdentifiersResponse::class.java)
  }

  fun getResponsibleOfficerDetails(crn: String): OfficerDetails? {
    val officerDetailsPath = UriComponentsBuilder.fromPath(responsibleOfficerLocation)
      .buildAndExpand(crn)
      .toString()

    val responsibleOfficer = communityApiClient.get(officerDetailsPath)
      .retrieve()
      .bodyToMono(OfficerDetails::class.java)
      .onErrorResume(WebClientResponseException::class.java) { e ->
        when (e.statusCode) {
          // not all delius users are staff
          HttpStatus.NOT_FOUND -> Mono.empty()
          else -> Mono.error(e)
        }
      }
      .block()

    if (responsibleOfficer == null) {
      telemetryService.reportInvalidAssumption(
        "service users always have a responsible officer",
        mapOf("crn" to crn),
        recoverable = false,
      )
    }
    return responsibleOfficer
  }
}
