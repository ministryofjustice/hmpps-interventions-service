package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.util.UriComponentsBuilder
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.component.RestClient

data class Prisoner(
  val prisonId: String,
  val releaseDate: String,
  val confirmedReleaseDate: String,
  val nonDtoReleaseDate: String,
  val automaticReleaseDate: String,
  val postRecallReleaseDate: String,
  val conditionalReleaseDate: String,
  val actualParoleDate: String,
  val dischargeDate: String
)

@Service
class PrisonerOffenderSearchService(
  @Value("\${prisoner-offender-search.locations.prisoner}") private val prisonerSearchPrisonerLocation: String,
  private val prisonerOffenderSearchClient: RestClient
) {
  fun getPrisonerById(nomsId: String): Prisoner? {
    val prisonSearchPrisonerPath = UriComponentsBuilder.fromPath(prisonerSearchPrisonerLocation)
      .buildAndExpand(nomsId)
      .toString()

    return prisonerOffenderSearchClient.get(prisonSearchPrisonerPath)
      .retrieve()
      .bodyToMono(Prisoner::class.java)
      .onErrorResume(WebClientResponseException::class.java) { e ->
        when (e.statusCode) {
          HttpStatus.NOT_FOUND -> Mono.empty()
          else -> Mono.error(e)
        }
      }
      .block()
  }
}
