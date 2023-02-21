package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.listeners

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationListener
import org.springframework.stereotype.Service
import org.springframework.web.util.UriComponentsBuilder
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.component.CommunityAPIClient
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.ReferralEndingEvent
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
  private val communityAPIClient: CommunityAPIClient,
) : ApplicationListener<ReferralEndingEvent>, CommunityAPIService {
  override fun onApplicationEvent(event: ReferralEndingEvent) {
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
