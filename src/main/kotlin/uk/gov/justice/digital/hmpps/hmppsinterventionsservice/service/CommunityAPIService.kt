package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service

import mu.KLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationListener
import org.springframework.stereotype.Service
import org.springframework.web.util.UriComponentsBuilder
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.component.CommunityAPIClient
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.component.RamDeliusClient
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.ActionPlanEvent
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.ActionPlanEventType
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.AppointmentEvent
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.AppointmentEventType
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Appointment
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Attended
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Attended.NO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Referral
import java.time.OffsetDateTime
import java.util.UUID

interface CommunityAPIService {
  fun getNotes(referral: Referral, url: String, description: String): String {
    val contractTypeName = referral.intervention.dynamicFrameworkContract.contractType.name
    val primeProviderName = referral.intervention.dynamicFrameworkContract.primeProvider.name
    return "$description for $contractTypeName Referral ${referral.referenceNumber} with Prime Provider $primeProviderName\n$url"
  }

  fun setNotifyPPIfRequired(appointment: Appointment) =
    NO == appointment.attended || appointment.notifyPPOfAttendanceBehaviour == true
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

@Service
class CommunityAPIAppointmentEventService(
  @Value("\${interventions-ui.baseurl}") private val interventionsUIBaseURL: String,
  @Value("\${interventions-ui.locations.probation-practitioner.supplier-assessment-feedback}") private val ppSessionFeedbackLocation: String,
  @Value("\${community-api.appointments.outcome.enabled}") private val outcomeNotificationEnabled: Boolean,
  @Value("\${refer-and-monitor-and-delius.locations.appointment-merge}") private val appointmentMergeLocation: String,
  private val ramDeliusClient: RamDeliusClient,
) : ApplicationListener<AppointmentEvent>, CommunityAPIService {
  companion object : KLogging()

  override fun onApplicationEvent(event: AppointmentEvent) {
    when (event.type) {
      AppointmentEventType.SESSION_FEEDBACK_RECORDED -> {
        if (!outcomeNotificationEnabled) {
          return
        }
        mergeAppointment(event.appointment.forMerge())
      }
      else -> {}
    }
  }

  private fun Appointment.forMerge() = AppointmentMerge(
    id,
    referral.id,
    referral.referenceNumber!!,
    referral.serviceUserCRN,
    referral.relevantSentenceId,
    appointmentTime,
    appointmentTime.plusMinutes(durationInMinutes.toLong()),
    getNotes(
      referral,
      UriComponentsBuilder.fromHttpUrl(interventionsUIBaseURL)
        .path(ppSessionFeedbackLocation)
        .buildAndExpand(referral.id)
        .toString(),
      "Session Feedback Recorded",
    ),
    appointmentDelivery?.npsOfficeCode,
    false,
    attended?.let { AppointmentMerge.Outcome(it.forMerge(), (attended == NO || notifyPPOfAttendanceBehaviour == true)) },
    null,
    null,
  )

  private fun Attended.forMerge() = when (this) {
    Attended.YES -> AppointmentMerge.Outcome.Attended.YES
    NO -> AppointmentMerge.Outcome.Attended.NO
    Attended.LATE -> AppointmentMerge.Outcome.Attended.LATE
  }

  private fun mergeAppointment(appointmentMerge: AppointmentMerge): Pair<Long?, UUID> {
    val path = UriComponentsBuilder.fromPath(appointmentMergeLocation)
      .buildAndExpand(appointmentMerge.serviceUserCrn, appointmentMerge.referralId)
      .toString()
    return ramDeliusClient.makePutAppointmentRequest(path, appointmentMerge)?.appointmentId to appointmentMerge.id
  }
}

data class NotificationCreateRequestDTO(
  val contractType: String,
  val referralStart: OffsetDateTime,
  val referralId: UUID,
  val contactDateTime: OffsetDateTime,
  val notes: String,
)

data class AppointmentOutcomeRequest(
  val notes: String,
  val attended: String,
  val notifyPPOfAttendanceBehaviour: Boolean,
)
