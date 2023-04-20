package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.util.UriComponentsBuilder
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.component.RestClient

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
          // not all delius users are staff
          HttpStatus.NOT_FOUND -> Mono.empty()
          else -> Mono.error(e)
        }
      }
      .block()

    if (officerDetails == null || !(officerDetails.communityOfficer.responsibleOfficer)) {
      telemetryService.reportInvalidAssumption(
        "service users always have a responsible officer",
        mapOf("crn" to crn),
        recoverable = false,
      )
    }
    return officerDetails
  }
}
