package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationListener
import org.springframework.stereotype.Service
import org.springframework.web.util.UriComponentsBuilder
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.component.SNSPublisher
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.EventDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.PersonReference
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.ActionPlanEvent
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.ActionPlanEventType
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.AppointmentEvent
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.AppointmentEventType
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.EndOfServiceReportEvent
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.EndOfServiceReportEventType.SUBMITTED
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.ReferralEvent
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.ReferralEventType
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.exception.AsyncEventExceptionHandling
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AppointmentType
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Attended

interface SNSService

@Service
class SNSActionPlanService(
  private val snsPublisher: SNSPublisher,
  @Value("\${interventions-ui.baseurl}") private val interventionsUiBaseUrl: String,
  @Value("\${interventions-ui.locations.probation-practitioner.action-plan}") private val actionPlanLocation: String,
) : ApplicationListener<ActionPlanEvent>, SNSService {

  @AsyncEventExceptionHandling
  override fun onApplicationEvent(event: ActionPlanEvent) {
    val uiUrl = UriComponentsBuilder.fromHttpUrl(interventionsUiBaseUrl)
      .path(actionPlanLocation)
      .buildAndExpand(event.actionPlan.referral.id)
      .toString()
    val referral = event.actionPlan.referral
    when (event.type) {
      ActionPlanEventType.SUBMITTED -> {
        val snsEvent = EventDTO(
          "intervention.action-plan.submitted",
          "A draft action plan has been submitted",
          event.detailUrl,
          event.actionPlan.submittedAt!!,
          mapOf(
            "actionPlanId" to event.actionPlan.id,
            "submittedBy" to (event.actionPlan.submittedBy?.userName!!),
            "referralId" to referral.id,
            "referralReference" to referral.referenceNumber!!,
            "contractTypeName" to referral.intervention.dynamicFrameworkContract.contractType.name,
            "primeProviderName" to referral.intervention.dynamicFrameworkContract.primeProvider.name,
            "actionPlanProbationUserUrl" to uiUrl,
          ),
          PersonReference.crn(event.actionPlan.referral.serviceUserCRN),
        )
        snsPublisher.publish(event.actionPlan.referral.id, event.actionPlan.submittedBy!!, snsEvent)
      }
      ActionPlanEventType.APPROVED -> {
        val snsEvent = EventDTO(
          "intervention.action-plan.approved",
          "An action plan has been approved",
          event.detailUrl,
          event.actionPlan.approvedAt!!,
          mapOf(
            "actionPlanId" to event.actionPlan.id,
            "approvedBy" to (event.actionPlan.approvedBy?.userName!!),
            "referralId" to referral.id,
            "referralReference" to referral.referenceNumber!!,
            "contractTypeName" to referral.intervention.dynamicFrameworkContract.contractType.name,
            "primeProviderName" to referral.intervention.dynamicFrameworkContract.primeProvider.name,
            "actionPlanProbationUserUrl" to uiUrl,
          ),
          PersonReference.crn(event.actionPlan.referral.serviceUserCRN),
        )
        snsPublisher.publish(event.actionPlan.referral.id, event.actionPlan.approvedBy!!, snsEvent)
      }
    }
  }
}

@Service
class SNSReferralService(
  private val snsPublisher: SNSPublisher,
) : ApplicationListener<ReferralEvent>, SNSService {

  @AsyncEventExceptionHandling
  override fun onApplicationEvent(event: ReferralEvent) {
    when (event.type) {
      ReferralEventType.SENT -> {
        val snsEvent = EventDTO(
          "intervention.referral.sent",
          "A referral has been sent to a Service Provider",
          event.detailUrl,
          event.referral.sentAt!!,
          mapOf("referralId" to event.referral.id),
          PersonReference.crn(event.referral.serviceUserCRN),
        )
        snsPublisher.publish(event.referral.id, event.referral.sentBy!!, snsEvent)
      }
      ReferralEventType.ASSIGNED -> {
        val assignment = event.referral.currentAssignment!!
        val snsEvent = EventDTO(
          "intervention.referral.assigned",
          "A referral has been assigned to a caseworker / service provider",
          event.detailUrl,
          assignment.assignedAt,
          mapOf("referralId" to event.referral.id, "assignedTo" to (assignment.assignedTo.userName)),
          PersonReference.crn(event.referral.serviceUserCRN),
        )
        snsPublisher.publish(event.referral.id, assignment.assignedBy, snsEvent)
      }
      else -> {}
    }
  }
}

@Service
class SNSAppointmentService(
  private val snsPublisher: SNSPublisher,
  @Value("\${interventions-ui.baseurl}") private val interventionsUiBaseUrl: String,
  @Value("\${interventions-ui.locations.probation-practitioner.supplier-assessment-feedback}") private val saFeedbackLocation: String,
  @Value("\${interventions-ui.locations.probation-practitioner.session-feedback}") private val ppSessionFeedbackLocation: String,
) : ApplicationListener<AppointmentEvent>, SNSService {

  @AsyncEventExceptionHandling
  override fun onApplicationEvent(event: AppointmentEvent) {
    val appointmentType = if (event.appointmentType == AppointmentType.SERVICE_DELIVERY) "session-appointment" else "initial-assessment-appointment"
    val appointmentTypeDescription = if (event.appointmentType == AppointmentType.SERVICE_DELIVERY) "a session appointment" else "an initial assessment appointment"

    when (event.type) {
      AppointmentEventType.ATTENDANCE_RECORDED -> {
        val referral = event.appointment.referral
        val appointment = event.appointment

        val eventType = "intervention.$appointmentType.${when (appointment.attended) {
          Attended.YES, Attended.LATE -> "attended"
          Attended.NO -> "missed"
          null -> throw RuntimeException("event triggered for appointment with no recorded attendance")
        }}"

        val snsEvent = EventDTO(
          eventType,
          "Attendance was recorded for $appointmentTypeDescription",
          event.detailUrl,
          appointment.attendanceSubmittedAt!!,
          mapOf("serviceUserCRN" to referral.serviceUserCRN, "referralId" to referral.id),
          PersonReference.crn(referral.serviceUserCRN),
        )

        snsPublisher.publish(referral.id, appointment.appointmentFeedbackSubmittedBy!!, snsEvent)
      }
      AppointmentEventType.APPOINTMENT_FEEDBACK_RECORDED -> {
        val referral = event.appointment.referral
        val appointment = event.appointment

        val eventType = "intervention.$appointmentType.session-feedback-submitted"
        val url =
          if (event.appointmentType == AppointmentType.SERVICE_DELIVERY) {
            UriComponentsBuilder.fromHttpUrl(interventionsUiBaseUrl)
              .path(ppSessionFeedbackLocation)
              .buildAndExpand(referral.id, event.deliverySession?.sessionNumber, event.appointment.deliusAppointmentId!!)
              .toString()
          } else {
            UriComponentsBuilder.fromHttpUrl(interventionsUiBaseUrl)
              .path(saFeedbackLocation)
              .buildAndExpand(referral.id)
              .toString()
          }
        val contractTypeName = referral.intervention.dynamicFrameworkContract.contractType.name
        val primeProviderName = referral.intervention.dynamicFrameworkContract.primeProvider.name

        val snsEvent = EventDTO(
          eventType,
          "Session feedback submitted for $appointmentTypeDescription",
          event.detailUrl,
          appointment.appointmentFeedbackSubmittedAt!!,
          listOfNotNull(
            "serviceUserCRN" to referral.serviceUserCRN,
            "referralId" to referral.id,
            "referralReference" to referral.referenceNumber!!,
            "contractTypeName" to contractTypeName,
            "primeProviderName" to primeProviderName,
            appointment.deliusAppointmentId?.let { "deliusAppointmentId" to it.toString() },
            "referralProbationUserURL" to url,
          ).toMap(),
          PersonReference.crn(referral.serviceUserCRN),
        )

        snsPublisher.publish(referral.id, appointment.appointmentFeedbackSubmittedBy!!, snsEvent)
      }
      else -> {}
    }
  }
}

@Service
class SNSEndOfServiceReportService(
  private val snsPublisher: SNSPublisher,
) : ApplicationListener<EndOfServiceReportEvent>, SNSService {

  @AsyncEventExceptionHandling
  override fun onApplicationEvent(event: EndOfServiceReportEvent) {
    when (event.type) {
      SUBMITTED -> {
        val snsEvent = EventDTO(
          "intervention.end-of-service-report.submitted",
          "An end of service report has been submitted",
          event.detailUrl,
          event.endOfServiceReport.submittedAt!!,
          mapOf("endOfServiceReportId" to event.endOfServiceReport.id, "submittedBy" to (event.endOfServiceReport.submittedBy?.userName!!)),
          PersonReference.crn(event.endOfServiceReport.referral.serviceUserCRN),
        )
        snsPublisher.publish(event.endOfServiceReport.referral.id, event.endOfServiceReport.submittedBy!!, snsEvent)
      }
    }
  }
}
