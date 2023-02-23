package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.listeners

import com.microsoft.applicationinsights.TelemetryClient
import org.springframework.context.ApplicationListener
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.ReferralEvent
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.ReferralEventType
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ReferralLocationRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.CommunityAPIOffenderService
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.PrisonerOffenderSearchService

@Service
class CustodyLocationLookupListener(
  val communityAPIOffenderService: CommunityAPIOffenderService,
  val prisonerOffenderSearchService: PrisonerOffenderSearchService,
  val referralLocationRepository: ReferralLocationRepository,
  val telemetryClient: TelemetryClient
) : ApplicationListener<ReferralEvent> {
  override fun onApplicationEvent(event: ReferralEvent) {
    if (event.type != ReferralEventType.SENT) {
      return
    }
    val eventName = "CustodyLocationLookup"
    var eventProperties: Map<String, String> = emptyMap()
    val serviceUserCRN = event.referral.serviceUserCRN
    val referralId = event.referral.id

    val offenderNomsId = communityAPIOffenderService.getOffenderIdentifiers(serviceUserCRN)
      .onErrorResume { e: Throwable ->
        eventProperties = mapOf(
          "result" to "noms lookup failed",
          "exception" to e.toString(),
          "crn" to serviceUserCRN
        )
        Mono.empty()
      }
      .block()?.primaryIdentifiers?.nomsNumber

    offenderNomsId?.let {
      val prisoner = prisonerOffenderSearchService.getPrisonerById(it)
        .onErrorResume(WebClientResponseException::class.java) { e ->
          eventProperties = mapOf(
            "result" to "prisoner search failed",
            "exception" to e.toString(),
            "crn" to serviceUserCRN
          )
          Mono.empty()
        }
        .block()

      prisoner?.let {
        val referralLocation = referralLocationRepository.findByReferralId(referralId)
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
              "crn" to serviceUserCRN
            )
          }
          referralLocationRepository.save(referralLocation)
        }
      } ?: run {
        if (eventProperties.isEmpty()) eventProperties = mapOf(
          "result" to "prisoner not found",
          "noms" to offenderNomsId,
          "crn" to serviceUserCRN
        )
      }
    } ?: run {
      if (eventProperties.isEmpty()) eventProperties = mapOf(
        "result" to "noms not found",
        "crn" to serviceUserCRN
      )
    }
    telemetryClient.trackEvent(eventName, eventProperties, null)
  }
}
