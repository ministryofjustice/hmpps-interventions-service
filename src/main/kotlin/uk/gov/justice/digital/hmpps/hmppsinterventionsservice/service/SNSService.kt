package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationListener
import org.springframework.stereotype.Service
import org.springframework.web.util.UriComponentsBuilder
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.component.SNSPublisher
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.EventDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.PersonReference
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.ActionPlanAppointmentEvent
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.ActionPlanAppointmentEventType
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.ActionPlanEvent
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.ActionPlanEventType
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.AppointmentEvent
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.AppointmentEventType
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.EndOfServiceReportEvent
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.EndOfServiceReportEventType.SUBMITTED
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.ReferralEvent
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.ReferralEventType
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.exception.AsyncEventExceptionHandling
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Attended

interface SNSService

@Service
class SNSActionPlanService(
  private val snsPublisher: SNSPublisher,
) : ApplicationListener<ActionPlanEvent>, SNSService {

  @AsyncEventExceptionHandling
  override fun onApplicationEvent(event: ActionPlanEvent) {
    when (event.type) {
      ActionPlanEventType.SUBMITTED -> {
        val snsEvent = EventDTO(
          "intervention.action-plan.submitted",
          "A draft action plan has been submitted",
          event.detailUrl,
          event.actionPlan.submittedAt!!,
          mapOf("actionPlanId" to event.actionPlan.id, "submittedBy" to (event.actionPlan.submittedBy?.userName!!)),
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
          mapOf("actionPlanId" to event.actionPlan.id, "approvedBy" to (event.actionPlan.approvedBy?.userName!!)),
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
class SNSActionPlanAppointmentService(
  private val snsPublisher: SNSPublisher,
  @Value("\${interventions-ui.baseurl}") private val interventionsUIBaseURL: String,
  @Value("\${interventions-ui.locations.probation-practitioner.session-feedback}") private val ppSessionFeedbackLocation: String,
) : ApplicationListener<ActionPlanAppointmentEvent>, SNSService {

  @AsyncEventExceptionHandling
  override fun onApplicationEvent(event: ActionPlanAppointmentEvent) {
    when (event.type) {
      ActionPlanAppointmentEventType.ATTENDANCE_RECORDED -> {
        val referral = event.referral
        event.deliverySession.appointmentTime ?: throw RuntimeException("event triggered for session with no appointments")

        val eventType = "intervention.session-appointment.${when (event.deliverySession.sessionFeedback.attendance.attended) {
          Attended.YES, Attended.LATE -> "attended"
          Attended.NO -> "missed"
          null -> throw RuntimeException("event triggered for appointment with no recorded attendance")
        }}"

        val snsEvent = EventDTO(
          eventType,
          "Attendance was recorded for a session appointment",
          event.detailUrl,
          event.deliverySession.sessionFeedback.attendance.submittedAt!!,
          mapOf("serviceUserCRN" to referral.serviceUserCRN, "referralId" to referral.id),
          PersonReference.crn(event.referral.serviceUserCRN),
        )

        snsPublisher.publish(referral.id, event.deliverySession.sessionFeedback.attendance.submittedBy!!, snsEvent)
      }
      ActionPlanAppointmentEventType.SESSION_FEEDBACK_RECORDED -> {
        val referral = event.referral
        event.deliverySession.appointmentTime ?: throw RuntimeException("event triggered for session with no appointments")

        val eventType = "intervention.session-appointment.session-feedback-submitted"

        val url = UriComponentsBuilder.fromHttpUrl(interventionsUIBaseURL)
          .path(ppSessionFeedbackLocation)
          .buildAndExpand(event.referral.id, event.deliverySession.sessionNumber, event.deliverySession.deliusAppointmentId!!)
          .toString()

        val snsEvent = EventDTO(
          eventType,
          "Session feedback submitted for a session appointment",
          event.detailUrl,
          event.deliverySession.sessionFeedback.attendance.submittedAt!!,
          mapOf(
            "serviceUserCRN" to referral.serviceUserCRN,
            "referralId" to referral.id,
            "referralReference" to referral.referenceNumber,
            "contractTypeName" to event.contractTypeName,
            "primeProviderName" to event.primeProviderName,
            "deliusAppointmentId" to event.deliverySession.deliusAppointmentId.toString(),
            "referralProbationUserURL" to url,
          ),
          PersonReference.crn(referral.serviceUserCRN),
        )

        snsPublisher.publish(referral.id, event.deliverySession.sessionFeedback.submittedBy!!, snsEvent)
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
) : ApplicationListener<AppointmentEvent>, SNSService {

  @AsyncEventExceptionHandling
  override fun onApplicationEvent(event: AppointmentEvent) {
    when (event.type) {
      AppointmentEventType.ATTENDANCE_RECORDED -> {
        val referral = event.appointment.referral
        val appointment = event.appointment

        val eventType = "intervention.initial-assessment-appointment.${when (appointment.attended) {
          Attended.YES, Attended.LATE -> "attended"
          Attended.NO -> "missed"
          null -> throw RuntimeException("event triggered for appointment with no recorded attendance")
        }}"

        val snsEvent = EventDTO(
          eventType,
          "Attendance was recorded for an initial assessment appointment",
          event.detailUrl,
          appointment.attendanceSubmittedAt!!,
          mapOf("serviceUserCRN" to referral.serviceUserCRN, "referralId" to referral.id),
          PersonReference.crn(referral.serviceUserCRN),
        )

        snsPublisher.publish(referral.id, appointment.appointmentFeedbackSubmittedBy!!, snsEvent)
      }
      AppointmentEventType.SESSION_FEEDBACK_RECORDED -> {
        val referral = event.appointment.referral
        val appointment = event.appointment

        val eventType = "intervention.initial-assessment-appointment.session-feedback-submitted"
        val url = UriComponentsBuilder.fromHttpUrl(interventionsUiBaseUrl)
          .path(saFeedbackLocation)
          .buildAndExpand(referral.id)
          .toString()
        val contractTypeName = referral.intervention.dynamicFrameworkContract.contractType.name
        val primeProviderName = referral.intervention.dynamicFrameworkContract.primeProvider.name

        val snsEvent = EventDTO(
          eventType,
          "Session feedback submitted for an initial assessment appointment",
          event.detailUrl,
          appointment.appointmentFeedbackSubmittedAt!!,
          mapOf(
            "serviceUserCRN" to referral.serviceUserCRN,
            "referralId" to referral.id,
            "referralReference" to referral.referenceNumber!!,
            "contractTypeName" to contractTypeName,
            "primeProviderName" to primeProviderName,
            "deliusAppointmentId" to appointment.deliusAppointmentId.toString(),
            "referralProbationUserURL" to url,
          ),
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
