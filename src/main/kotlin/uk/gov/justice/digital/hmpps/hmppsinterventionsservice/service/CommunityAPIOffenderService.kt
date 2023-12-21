package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.util.UriComponentsBuilder
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.component.RestClient
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AuthUser

data class Offender(
  val crnNumber: String,
)

private data class ManagedCases(val managedCases: List<CaseIdentifier>)
private data class CaseIdentifier(val crn: String, val nomsId: String? = null)

data class ServiceUserAccessResult(
  val canAccess: Boolean,
  val messages: List<String>,
)

private data class CaseAccess(
  val crn: String,
  val userExcluded: Boolean,
  val userRestricted: Boolean,
  val exclusionMessage: String? = null,
  val restrictionMessage: String? = null,
) {
  val limited = userExcluded || userRestricted
}

private data class UserAccessResponse(val access: List<CaseAccess>)

data class OffenderIdentifiersResponse(
  val primaryIdentifiers: PrimaryIdentifiersResponse?,
)

data class PrimaryIdentifiersResponse(
  val nomsNumber: String?,
)

@Service
class CommunityAPIOffenderService(
  @Value("\${refer-and-monitor-and-delius.locations.case-access}") private val caseAccessLocation: String,
  @Value("\${refer-and-monitor-and-delius.locations.managed-cases}") private val managedCasesLocation: String,
  @Value("\${refer-and-monitor-and-delius.locations.case-identifiers}") private val caseIdentifiersLocation: String,
  private val ramDeliusApiClient: RestClient,
) {
  fun checkIfAuthenticatedDeliusUserHasAccessToServiceUser(user: AuthUser, crn: String): ServiceUserAccessResult {
    val userAccessPath = UriComponentsBuilder.fromPath(caseAccessLocation)
      .buildAndExpand(user.id)
      .toString()

    val response = ramDeliusApiClient.post(userAccessPath, listOf(crn))
      .retrieve()
      .bodyToMono(UserAccessResponse::class.java)
      .block()
    val userAccess = response?.access?.firstOrNull { it.crn == crn }
    return ServiceUserAccessResult(
      userAccess?.limited == false,
      listOfNotNull(userAccess?.restrictionMessage, userAccess?.exclusionMessage),
    )
  }

  fun getManagedOffendersForDeliusUser(user: AuthUser): List<Offender> {
    val managedOffendersPath = UriComponentsBuilder.fromPath(managedCasesLocation)
      .buildAndExpand(user.id)
      .toString()

    return ramDeliusApiClient.get(managedOffendersPath)
      .retrieve()
      .bodyToMono(ManagedCases::class.java)
      .block().let { res -> res?.managedCases?.map { Offender(it.crn) } ?: listOf() }
  }

  fun getOffenderIdentifiers(crn: String): Mono<OffenderIdentifiersResponse> {
    val offenderIdentifiersPath = UriComponentsBuilder.fromPath(caseIdentifiersLocation)
      .buildAndExpand(crn)
      .toString()

    return ramDeliusApiClient.get(offenderIdentifiersPath)
      .retrieve()
      .bodyToMono(CaseIdentifier::class.java).map { OffenderIdentifiersResponse(PrimaryIdentifiersResponse(it.nomsId)) }
  }
}
