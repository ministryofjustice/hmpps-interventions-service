package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.listeners

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationListener
import org.springframework.stereotype.Service
import org.springframework.web.util.UriComponentsBuilder
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.component.CommunityAPIClient
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.component.SNSPublisher
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.EventDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.ReferralEndingEvent
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AuthUser
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.CommunityAPIService
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.ReferralEndRequest
import java.time.OffsetDateTime
import java.util.UUID

@Service
class ReferralEndingListener(
  @Value("\${interventions-ui.baseurl}") private val interventionsUIBaseURL: String,
  @Value("\${interventions-ui.locations.probation-practitioner.referral-details}") private val ppReferralDetailsLocation: String,
  @Value("\${community-api.locations.ended-referral}") private val communityAPIEndedReferralLocation: String,
  @Value("\${community-api.integration-context}") private val integrationContext: String,
  private val snsPublisher: SNSPublisher,
  private val communityAPIClient: CommunityAPIClient,
) : ApplicationListener<ReferralEndingEvent>, CommunityAPIService {
  override fun onApplicationEvent(event: ReferralEndingEvent) {
    sendDomainEvent(event)
    terminateNSI(event)
  }

  private fun sendDomainEvent(event: ReferralEndingEvent) {
    val snsEvent = EventDTO(
      eventType = "intervention.referral.ended",
      description = "The referral has ended. It might still require admin work from providers",
      detailUrl = event.detailUrl,
      occurredAt = endedTime(event),
      additionalInformation = mapOf(
        "deliveryState" to event.state.name,
        "referralId" to event.referral.id.toString(),
        "referralURN" to event.referral.urn,
        "referralProbationUserURL" to referralDetailsUrl(event.referral.id),
      ),
    )
    snsPublisher.publish(event.referral.id, AuthUser.interventionsServiceUser, snsEvent)
  }

  private fun terminateNSI(event: ReferralEndingEvent) {
    communityAPIClient.makeAsyncPostRequest(
      communityAPISentReferralUrl(event.referral.serviceUserCRN),
      ReferralEndRequest(
        contractType = event.referral.intervention.dynamicFrameworkContract.contractType.code,
        startedAt = event.referral.sentAt!!,
        endedAt = endedTime(event),
        sentenceId = event.referral.relevantSentenceId!!,
        referralId = event.referral.id,
        endType = event.state.name,
        notes = getNotes(event.referral, referralDetailsUrl(event.referral.id), "Referral Ended"),
      )
    )
  }

  // TODO code smell: we are guessing at what happened by looking at data
  // Better would be to emit "ReferralCancelledEvent" and "ReferralCompletedEvent" and listen to them to create this domain event
  private fun endedTime(event: ReferralEndingEvent): OffsetDateTime =
    event.referral.concludedAt ?: event.referral.endRequestedAt ?: OffsetDateTime.now()

  private fun referralDetailsUrl(referralId: UUID) =
    UriComponentsBuilder.fromHttpUrl(interventionsUIBaseURL)
      .path(ppReferralDetailsLocation)
      .buildAndExpand(referralId)
      .toString()

  private fun communityAPISentReferralUrl(serviceUserCRN: String) =
    UriComponentsBuilder.fromPath(communityAPIEndedReferralLocation)
      .buildAndExpand(serviceUserCRN, integrationContext)
      .toString()
}