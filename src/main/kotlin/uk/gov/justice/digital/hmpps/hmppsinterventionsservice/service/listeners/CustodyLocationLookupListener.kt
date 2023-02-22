package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.listeners

import com.microsoft.applicationinsights.TelemetryClient
import org.springframework.context.ApplicationListener
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.CustodyLocationLookupEvent
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ReferralLocationRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.CommunityAPIOffenderService
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.PrisonerOffenderSearchService

@Service
class CustodyLocationLookupListener(
  val communityAPIOffenderService: CommunityAPIOffenderService,
  val prisonerOffenderSearchService: PrisonerOffenderSearchService,
  val referralLocationRepository: ReferralLocationRepository,
  val telemetryClient: TelemetryClient
) : ApplicationListener<CustodyLocationLookupEvent> {
  override fun onApplicationEvent(event: CustodyLocationLookupEvent) {
    val eventName = "CustodyLocationLookup"
    var eventProperties: Map<String, String> = emptyMap()

    val offenderNomsId = communityAPIOffenderService.getOffenderIdentifiers(event.serviceUserCRN)
      .onErrorResume { e: Throwable ->
        eventProperties = mapOf(
          "result" to "noms lookup failed",
          "exception" to e.message.toString(),
          "crn" to event.serviceUserCRN
        )
        Mono.empty()
      }
      .block()?.primaryIdentifiers?.nomsNumber

    offenderNomsId?.let {
      val prisoner = prisonerOffenderSearchService.getPrisonerById(it)
        .onErrorResume(WebClientResponseException::class.java) { e ->
          eventProperties = mapOf(
            "result" to "prisoner search failed",
            "exception" to e.message.toString(),
            "crn" to event.serviceUserCRN
          )
          Mono.empty()
        }
        .block()

      prisoner?.let {
        val referralLocation = referralLocationRepository.findByReferralId(event.referralId)
        referralLocation?.let {
          with(prisoner) {
            it.nomisPrisonId = prisonId
            it.nomisReleaseDate = releaseDate
            it.nomisConfirmedReleaseDate = confirmedReleaseDate
            it.nomisNonDtoReleaseDate = nonDtoReleaseDate
            it.nomisAutomaticReleaseDate = automaticReleaseDate
            it.nomisPostRecallReleaseDate = postRecallReleaseDate
            it.nomisConditionalReleaseDate = conditionalReleaseDate
            it.nomisActualParoleDate = actualParoleDate
            it.nomisDischargeDate = dischargeDate

            eventProperties = mapOf(
              "result" to if (it.nomisPrisonId == it.prisonId) "match" else "no match",
              "crn" to event.serviceUserCRN
            )
          }
          referralLocationRepository.save(referralLocation)
        }
      } ?: run {
        if (eventProperties.isEmpty()) eventProperties = mapOf(
          "result" to "prisoner not found",
          "noms" to offenderNomsId,
          "crn" to event.serviceUserCRN
        )
      }
    } ?: run {
      if (eventProperties.isEmpty()) eventProperties = mapOf(
        "result" to "noms not found",
        "crn" to event.serviceUserCRN
      )
    }
    telemetryClient.trackEvent(eventName, eventProperties, null)
  }
}
