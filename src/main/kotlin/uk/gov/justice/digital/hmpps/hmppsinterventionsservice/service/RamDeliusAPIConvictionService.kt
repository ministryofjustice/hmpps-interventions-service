package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.util.UriComponentsBuilder
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.component.RamDeliusClient
import java.time.LocalDate

data class Sentence(
  val description: String,
  val expectedEndDate: LocalDate,
)

data class Offence(
  val category: String,
  val subCategory: String,
)

data class Conviction(
  val id: Long,
  val date: LocalDate,
  val sentence: Sentence,
  val mainOffence: Offence,
  val active: Boolean,
)

data class ConvictionDetails(
  val conviction: Conviction,
)

@Service
class RamDeliusAPIConvictionService(
  @Value("\${refer-and-monitor-and-delius.locations.conviction}") private val convictionLocation: String,
  private val ramDeliusApiClient: RamDeliusClient,
  private val telemetryService: TelemetryService,
) {
  fun getConvictionDetails(crn: String, id: Long): ConvictionDetails? {
    val convictionPath = UriComponentsBuilder.fromPath(convictionLocation)
      .buildAndExpand(crn, id)
      .toString()

    val convictionDetails = ramDeliusApiClient.makeGetConvictionRequest(convictionPath, null)

    if (convictionDetails?.conviction?.sentence?.expectedEndDate == null) {
      telemetryService.reportInvalidAssumption(
        "service user does not have an expected end date for their sentence",
        mapOf("crn" to crn),
        recoverable = false,
      )
    }

    if (convictionDetails?.conviction == null) {
      telemetryService.reportInvalidAssumption(
        "service user does not have the expected conviction",
        mapOf("conviction" to id.toString()),
        recoverable = false,
      )
    }

    return convictionDetails
  }
}
