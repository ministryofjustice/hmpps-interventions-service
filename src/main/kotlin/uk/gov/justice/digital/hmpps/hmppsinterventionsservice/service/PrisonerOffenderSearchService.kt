package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.util.UriComponentsBuilder
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.component.RestClient
import java.time.LocalDate

data class Prisoner(
  val prisonId: String,
  val releaseDate: LocalDate?,
  val confirmedReleaseDate: LocalDate?,
  val nonDtoReleaseDate: LocalDate?,
  val automaticReleaseDate: LocalDate?,
  val postRecallReleaseDate: LocalDate?,
  val conditionalReleaseDate: LocalDate?,
  val actualParoleDate: LocalDate?,
  val dischargeDate: LocalDate?,
)

@Service
class PrisonerOffenderSearchService(
  @Value("\${prisoner-offender-search.locations.prisoner}") private val prisonerSearchPrisonerLocation: String,
  private val prisonerOffenderSearchClient: RestClient,
) {
  fun getPrisonerById(nomsId: String): Mono<Prisoner> {
    val prisonSearchPrisonerPath = UriComponentsBuilder.fromPath(prisonerSearchPrisonerLocation)
      .buildAndExpand(nomsId)
      .toString()

    return prisonerOffenderSearchClient.get(prisonSearchPrisonerPath)
      .retrieve()
      .bodyToMono(Prisoner::class.java)
  }
}
