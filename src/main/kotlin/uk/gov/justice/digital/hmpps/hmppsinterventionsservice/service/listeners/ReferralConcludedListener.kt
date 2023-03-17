package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.listeners

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationListener
import org.springframework.stereotype.Service
import org.springframework.web.util.UriComponentsBuilder
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.component.CommunityAPIClient
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.component.SNSPublisher
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.EventDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.PersonReference
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.ReferralConcludedEvent
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.exception.AsyncEventExceptionHandling
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AuthUser
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.EndOfServiceReport
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.CommunityAPIService
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.NotificationCreateRequestDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.ReferralConcludedState
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.SNSService

@Service
class ReferralConcludedListener(
  private val snsPublisher: SNSPublisher,
) : ApplicationListener<ReferralConcludedEvent>, SNSService {
  @AsyncEventExceptionHandling
  override fun onApplicationEvent(event: ReferralConcludedEvent) {
    val snsEvent = EventDTO(
      "intervention.referral.concluded",
      "The referral has concluded, no more work is needed",
      event.detailUrl,
      event.referral.concludedAt!!,
      mapOf(
        "deliveryState" to event.type.name,
        "referralId" to event.referral.id,
      ),
      PersonReference.crn(event.referral.serviceUserCRN)
    )
    snsPublisher.publish(event.referral.id, AuthUser.interventionsServiceUser, snsEvent)
  }
}

@Service
class ReferralConcludedIntegrationListener(
  @Value("\${interventions-ui.baseurl}") private val interventionsUIBaseURL: String,
  @Value("\${interventions-ui.locations.probation-practitioner.end-of-service-report}") private val ppEndOfServiceReportLocation: String,
  @Value("\${community-api.locations.notification-request}") private val communityAPINotificationLocation: String,
  @Value("\${community-api.integration-context}") private val integrationContext: String,
  private val communityAPIClient: CommunityAPIClient,
) : ApplicationListener<ReferralConcludedEvent>, CommunityAPIService {
  override fun onApplicationEvent(event: ReferralConcludedEvent) {
    // this really is just the "end of service report submitted" event now
    when (event.type) {
      ReferralConcludedState.CANCELLED -> {}
      ReferralConcludedState.PREMATURELY_ENDED,
      ReferralConcludedState.COMPLETED,
      -> {
        // This should be an independent event based notification
        // However a race condition arises with the referral end
        // notification. To Avoid a NSI not found in community-api
        // this must be sent and processed before referral end
        postSyncNotificationRequest(event.referral.endOfServiceReport)
      }
    }
  }

  private fun postSyncNotificationRequest(endOfServiceReport: EndOfServiceReport?) {
    endOfServiceReport?.submittedAt ?: run {
      throw IllegalStateException("End of service report not submitted so should not get to this point")
    }

    val url = UriComponentsBuilder.fromHttpUrl(interventionsUIBaseURL)
      .path(ppEndOfServiceReportLocation)
      .buildAndExpand(endOfServiceReport.id)
      .toString()

    postSyncNotificationRequest(endOfServiceReport, url)
  }

  private fun postSyncNotificationRequest(endOfServiceReport: EndOfServiceReport, url: String) {
    val referral = endOfServiceReport.referral

    val request = NotificationCreateRequestDTO(
      endOfServiceReport.referral.intervention.dynamicFrameworkContract.contractType.code,
      referral.sentAt!!,
      referral.id,
      endOfServiceReport.submittedAt!!,
      getNotes(referral, url, "End of Service Report Submitted"),
    )

    val communityApiSentReferralPath = UriComponentsBuilder.fromPath(communityAPINotificationLocation)
      .buildAndExpand(referral.serviceUserCRN, referral.relevantSentenceId!!, integrationContext)
      .toString()

    communityAPIClient.makeSyncPostRequest(communityApiSentReferralPath, request, Unit::class.java)
  }
}
