package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events

import org.springframework.context.ApplicationEvent
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.component.LocationMapper
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.controller.AppointmentsController
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ActionPlanSession

enum class AppointmentEventType {
  ATTENDANCE_RECORDED,
  BEHAVIOUR_RECORDED,
  SESSION_FEEDBACK_RECORDED,
}

class AppointmentEvent(source: Any, val type: AppointmentEventType, val actionPlanSession: ActionPlanSession, val detailUrl: String, val notifyPP: Boolean) : ApplicationEvent(source) {
  override fun toString(): String {
    return "AppointmentEvent(type=$type, appointment=${actionPlanSession.id}, detailUrl='$detailUrl', source=$source)"
  }
}

@Component
class AppointmentEventPublisher(
  private val applicationEventPublisher: ApplicationEventPublisher,
  private val locationMapper: LocationMapper
) {
  fun attendanceRecordedEvent(actionPlanSession: ActionPlanSession, notifyPP: Boolean) {
    applicationEventPublisher.publishEvent(
      AppointmentEvent(this, AppointmentEventType.ATTENDANCE_RECORDED, actionPlanSession, getAppointmentURL(actionPlanSession), notifyPP)
    )
  }

  fun behaviourRecordedEvent(actionPlanSession: ActionPlanSession, notifyPP: Boolean) {
    applicationEventPublisher.publishEvent(
      AppointmentEvent(this, AppointmentEventType.BEHAVIOUR_RECORDED, actionPlanSession, getAppointmentURL(actionPlanSession), notifyPP)
    )
  }

  fun sessionFeedbackRecordedEvent(actionPlanSession: ActionPlanSession, notifyPP: Boolean) {
    applicationEventPublisher.publishEvent(
      AppointmentEvent(this, AppointmentEventType.SESSION_FEEDBACK_RECORDED, actionPlanSession, getAppointmentURL(actionPlanSession), notifyPP)
    )
  }

  private fun getAppointmentURL(actionPlanSession: ActionPlanSession): String {
    val path = locationMapper.getPathFromControllerMethod(AppointmentsController::getAppointment)
    return locationMapper.expandPathToCurrentRequestBaseUrl(path, actionPlanSession.actionPlan.id, actionPlanSession.sessionNumber).toString()
  }
}
