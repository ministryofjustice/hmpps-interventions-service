package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events

import org.springframework.context.ApplicationEvent
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.component.LocationMapper
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.controller.DeliverySessionController
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.controller.ReferralController
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Appointment
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AppointmentType
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.DeliverySession

enum class AppointmentEventType {
  ATTENDANCE_RECORDED,
  SESSION_FEEDBACK_RECORDED,
  APPOINTMENT_FEEDBACK_RECORDED,
  SCHEDULED,
}

class
AppointmentEvent(
  source: Any,
  val type: AppointmentEventType,
  val appointment: Appointment,
  val detailUrl: String,
  val notifyPP: Boolean,
  val appointmentType: AppointmentType,
  val deliverySession: DeliverySession? = null,
  val notifyProbationPractitionerOfBehaviour: Boolean? = null,
  val notifyProbationPractitionerOfConcerns: Boolean? = null,
) : ApplicationEvent(source) {
  override fun toString(): String {
    return "AppointmentEvent(type=$type, appointmentId=${appointment.id}, detailUrl='$detailUrl', source=$source)"
  }
}

@Component
class AppointmentEventPublisher(
  private val applicationEventPublisher: ApplicationEventPublisher,
  private val locationMapper: LocationMapper,
) {
  fun appointmentScheduledEvent(appointment: Appointment, appointmentType: AppointmentType) {
    applicationEventPublisher.publishEvent(
      AppointmentEvent(
        this,
        AppointmentEventType.SCHEDULED,
        appointment,
        getAppointmentURL(appointment, appointmentType),
        appointmentType == AppointmentType.SUPPLIER_ASSESSMENT,
        appointmentType,
      ),
    )
  }

  fun attendanceRecordedEvent(appointment: Appointment, notifyPP: Boolean, appointmentType: AppointmentType, deliverySession: DeliverySession? = null) {
    applicationEventPublisher.publishEvent(
      AppointmentEvent(
        this,
        AppointmentEventType.ATTENDANCE_RECORDED,
        appointment,
        getAppointmentURL(appointment, appointmentType, deliverySession),
        notifyPP,
        appointmentType,
        deliverySession,
      ),
    )
  }

  fun sessionFeedbackRecordedEvent(appointment: Appointment, notifyProbationPractitionerOfBehaviour: Boolean, notifyProbationPractitionerOfConcerns: Boolean, appointmentType: AppointmentType, deliverySession: DeliverySession? = null) {
    applicationEventPublisher.publishEvent(
      AppointmentEvent(
        this,
        AppointmentEventType.SESSION_FEEDBACK_RECORDED,
        appointment,
        getAppointmentURL(appointment, appointmentType, deliverySession),
        notifyProbationPractitionerOfBehaviour || notifyProbationPractitionerOfConcerns,
        appointmentType,
        deliverySession,
        notifyProbationPractitionerOfBehaviour,
        notifyProbationPractitionerOfConcerns,
      ),
    )
  }

  fun appointmentFeedbackRecordedEvent(appointment: Appointment, notifyProbationPractitionerOfBehaviour: Boolean, notifyProbationPractitionerOfConcerns: Boolean, appointmentType: AppointmentType, deliverySession: DeliverySession? = null) {
    applicationEventPublisher.publishEvent(
      AppointmentEvent(
        this,
        AppointmentEventType.APPOINTMENT_FEEDBACK_RECORDED,
        appointment,
        getAppointmentURL(appointment, appointmentType, deliverySession),
        notifyProbationPractitionerOfBehaviour || notifyProbationPractitionerOfConcerns,
        appointmentType,
        deliverySession,
        notifyProbationPractitionerOfBehaviour,
        notifyProbationPractitionerOfConcerns,
      ),
    )
  }

  private fun getAppointmentURL(appointment: Appointment, appointmentType: AppointmentType, deliverySession: DeliverySession? = null): String {
    when (appointmentType) {
      AppointmentType.SERVICE_DELIVERY -> run {
        if (deliverySession !== null) {
          val path = locationMapper.getPathFromControllerMethod(DeliverySessionController::getSessionForReferralId)
          return locationMapper.expandPathToCurrentContextPathUrl(path, appointment.referral.id, deliverySession.sessionNumber).toString()
        }
        return ""
      }
      AppointmentType.SUPPLIER_ASSESSMENT -> run {
        val path = locationMapper.getPathFromControllerMethod(ReferralController::getSupplierAssessmentAppointment)
        return locationMapper.expandPathToCurrentContextPathUrl(path, appointment.referral.id).toString()
      }
    }
  }
}
