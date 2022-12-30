package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.listeners

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationListener
import org.springframework.stereotype.Service
import org.springframework.web.util.UriComponentsBuilder
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.component.CommunityAPIClient
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.component.SNSPublisher
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.EventDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.ReferralConcludedEvent
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.exception.AsyncEventExceptionHandling
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AuthUser
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.EndOfServiceReport
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.CommunityAPIService
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.NotificationCreateRequestDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.ReferralConcludedState
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.ReferralEndRequest
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.SNSService

@Service
class ReferralConcludedListener(
  private val snsPublisher: SNSPublisher,
) : ApplicationListener<ReferralConcludedEvent>, SNSService {
  @AsyncEventExceptionHandling
  override fun onApplicationEvent(event: ReferralConcludedEvent) {
    when (event.type) {
      ReferralConcludedState.CANCELLED -> {
        val snsEvent = EventDTO(
          "intervention.referral.cancelled",
          "A referral has been cancelled",
          event.detailUrl,
          event.referral.concludedAt!!,
          mapOf("referralId" to event.referral.id)
        )
        snsPublisher.publish(event.referral.id, event.referral.endRequestedBy!!, snsEvent)
      }

      ReferralConcludedState.PREMATURELY_ENDED -> {
        val snsEvent = EventDTO(
          "intervention.referral.prematurely-ended",
          "A referral has been ended prematurely",
          event.detailUrl,
          event.referral.concludedAt!!,
          mapOf("referralId" to event.referral.id)
        )
        snsPublisher.publish(event.referral.id, event.referral.endRequestedBy!!, snsEvent)
      }

      ReferralConcludedState.COMPLETED -> {
        val snsEvent = EventDTO(
          "intervention.referral.completed",
          "A referral has been completed",
          event.detailUrl,
          event.referral.concludedAt!!,
          mapOf("referralId" to event.referral.id)
        )
        // This is a system generated event at present and as such the actor will represent this
        snsPublisher.publish(event.referral.id, AuthUser.interventionsServiceUser, snsEvent)
      }
    }
  }
}

@Service
class ReferralConcludedIntegrationListener(
  @Value("\${interventions-ui.baseurl}") private val interventionsUIBaseURL: String,
  @Value("\${interventions-ui.locations.probation-practitioner.referral-details}") private val ppReferralDetailsLocation: String,
  @Value("\${interventions-ui.locations.probation-practitioner.end-of-service-report}") private val ppEndOfServiceReportLocation: String,
  @Value("\${community-api.locations.ended-referral}") private val communityAPIEndedReferralLocation: String,
  @Value("\${community-api.locations.notification-request}") private val communityAPINotificationLocation: String,
  @Value("\${community-api.integration-context}") private val integrationContext: String,
  private val communityAPIClient: CommunityAPIClient,
) : ApplicationListener<ReferralConcludedEvent>, CommunityAPIService {
  override fun onApplicationEvent(event: ReferralConcludedEvent) {
    when (event.type) {
      ReferralConcludedState.CANCELLED,
      -> {
        val url = UriComponentsBuilder.fromHttpUrl(interventionsUIBaseURL)
          .path(ppReferralDetailsLocation)
          .buildAndExpand(event.referral.id)
          .toString()

        postReferralEndRequest(event, url)
      }

      ReferralConcludedState.PREMATURELY_ENDED,
      ReferralConcludedState.COMPLETED,
      -> {

        // This should be an independent event based notification
        // However a race condition arises with the referral end
        // notification. To Avoid a NSI not found in community-api
        // this must be sent and processed before referral end
        postSyncNotificationRequest(event.referral.endOfServiceReport)

        val url = UriComponentsBuilder.fromHttpUrl(interventionsUIBaseURL)
          .path(ppEndOfServiceReportLocation)
          .buildAndExpand(event.referral.endOfServiceReport!!.id)
          .toString()

        postReferralEndRequest(event, url)
      }
    }
  }

  private fun postReferralEndRequest(event: ReferralConcludedEvent, url: String) {
    val referralEndRequest = ReferralEndRequest(
      event.referral.intervention.dynamicFrameworkContract.contractType.code,
      event.referral.sentAt!!,
      event.referral.concludedAt!!,
      event.referral.relevantSentenceId!!,
      event.referral.id,
      event.type.name,
      getNotes(event.referral, url, "Referral Ended"),
    )

    val communityApiSentReferralPath = UriComponentsBuilder.fromPath(communityAPIEndedReferralLocation)
      .buildAndExpand(event.referral.serviceUserCRN, integrationContext)
      .toString()

    communityAPIClient.makeAsyncPostRequest(communityApiSentReferralPath, referralEndRequest)
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
