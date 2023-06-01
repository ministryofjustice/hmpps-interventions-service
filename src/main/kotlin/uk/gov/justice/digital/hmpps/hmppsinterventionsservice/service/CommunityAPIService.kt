package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service

import mu.KLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationListener
import org.springframework.stereotype.Service
import org.springframework.web.util.UriComponentsBuilder
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.component.CommunityAPIClient
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.ActionPlanEvent
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.ActionPlanEventType
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Referral
import java.time.OffsetDateTime
import java.util.UUID

interface CommunityAPIService {
  fun getNotes(referral: Referral, url: String, description: String): String {
    val contractTypeName = referral.intervention.dynamicFrameworkContract.contractType.name
    val primeProviderName = referral.intervention.dynamicFrameworkContract.primeProvider.name
    return "$description for $contractTypeName Referral ${referral.referenceNumber} with Prime Provider $primeProviderName\n$url"
  }
}

@Service
class CommunityAPIActionPlanEventService(
  @Value("\${interventions-ui.baseurl}") private val interventionsUIBaseURL: String,
  @Value("\${interventions-ui.locations.probation-practitioner.action-plan}") private val ppActionPlanLocation: String,
  @Value("\${community-api.locations.notification-request}") private val communityAPINotificationLocation: String,
  @Value("\${community-api.integration-context}") private val integrationContext: String,
  private val communityAPIClient: CommunityAPIClient,
) : ApplicationListener<ActionPlanEvent>, CommunityAPIService {
  companion object : KLogging()

  override fun onApplicationEvent(event: ActionPlanEvent) {
    when (event.type) {
      ActionPlanEventType.SUBMITTED -> {
        val url = UriComponentsBuilder.fromHttpUrl(interventionsUIBaseURL)
          .path(ppActionPlanLocation)
          .buildAndExpand(event.actionPlan.referral.id)
          .toString()

        postNotificationRequest(event, url, "Submitted", event.actionPlan.submittedAt!!)
      }

      ActionPlanEventType.APPROVED -> {
        val url = UriComponentsBuilder.fromHttpUrl(interventionsUIBaseURL)
          .path(ppActionPlanLocation)
          .buildAndExpand(event.actionPlan.referral.id)
          .toString()

        postNotificationRequest(event, url, "Approved", event.actionPlan.approvedAt!!)
      }
    }
  }

  private fun postNotificationRequest(event: ActionPlanEvent, url: String, status: String, eventTime: OffsetDateTime) {
    val referral = event.actionPlan.referral

    val request = NotificationCreateRequestDTO(
      referral.intervention.dynamicFrameworkContract.contractType.code,
      referral.sentAt!!,
      referral.id,
      eventTime,
      getNotes(referral, url, "Action Plan $status"),
    )

    val communityApiSentReferralPath = UriComponentsBuilder.fromPath(communityAPINotificationLocation)
      .buildAndExpand(referral.serviceUserCRN, referral.relevantSentenceId!!, integrationContext)
      .toString()

    communityAPIClient.makeAsyncPostRequest(communityApiSentReferralPath, request)
  }
}

data class NotificationCreateRequestDTO(
  val contractType: String,
  val referralStart: OffsetDateTime,
  val referralId: UUID,
  val contactDateTime: OffsetDateTime,
  val notes: String,
)
