package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.util.UriComponentsBuilder
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.component.RestClient

data class OfficerDetails(
  val communityManager: Manager,
  val prisonManager: Manager? = null,
) {
  val responsibleManager = when {
    communityManager.responsibleOfficer -> communityManager
    prisonManager?.responsibleOfficer == true -> prisonManager
    else -> null
  }
}

data class Manager(
  val code: String,
  val name: Name,
  val username: String?,
  val email: String?,
  val responsibleOfficer: Boolean,
)

data class Name(
  val forename: String,
  val surname: String,
)

@Service
class RamDeliusAPIOffenderService(
  @Value("\${refer-and-monitor-and-delius.locations.responsible-officer}") private val responsibleOfficerLocation: String,
  private val ramDeliusApiClient: RestClient,
  private val telemetryService: TelemetryService,
) {
  fun getResponsibleOfficerDetails(crn: String): OfficerDetails? {
    val officerDetailsPath = UriComponentsBuilder.fromPath(responsibleOfficerLocation)
      .buildAndExpand(crn)
      .toString()

    val officerDetails = ramDeliusApiClient.get(officerDetailsPath)
      .retrieve()
      .bodyToMono(OfficerDetails::class.java)
      .onErrorResume(WebClientResponseException::class.java) { e ->
        when (e.statusCode) {
          // crn not found - may have been merged
          HttpStatus.NOT_FOUND -> Mono.empty()
          else -> Mono.error(e)
        }
      }
      .block()

    if (officerDetails?.responsibleManager == null) {
      telemetryService.reportInvalidAssumption(
        "service users always have a responsible officer",
        mapOf("crn" to crn),
        recoverable = false,
      )
    }
    return officerDetails
  }
}
