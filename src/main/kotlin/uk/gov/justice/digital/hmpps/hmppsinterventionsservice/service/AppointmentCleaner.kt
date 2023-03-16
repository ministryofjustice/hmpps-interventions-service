package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Appointment
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.AppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.DeliverySessionRepository

@Service
@Transactional
class AppointmentCleaner(
  val appointmentRepository: AppointmentRepository,
  val deliverySessionRepository: DeliverySessionRepository,
) {
  fun linkIfSuperseded(appointment: Appointment): Appointment {
    val isSuperseded = appointment.superseded
    val result: Appointment

    if (isSuperseded) {
      val nextAppointment = getNextAppointmentInSupersededChain(appointment)
      if (nextAppointment != null) {
        result = linkAppointments(appointment, nextAppointment)
        return result
      }
      // we couldn't find next so send as normal
      result = appointment
    } else {
      // it's not superseded, so send out as normal
      result = appointment
    }
    return result
  }

  fun markAppoinmentAsStale(appointment: Appointment): Appointment {
    appointment.stale = true
    appointmentRepository.save(appointment)
    return appointment
  }

  private fun getNextAppointmentInSupersededChain(currentAppointment: Appointment): Appointment {
    val deliverySessions = deliverySessionRepository.findAllByReferralId(currentAppointment.referral.id)
    val targetDeliverySessionList = deliverySessions.filter { it.appointments.contains(currentAppointment) }
    val targetDeliverySession = targetDeliverySessionList.get(0)
    val sortedAppointmentFromDeliverySession = targetDeliverySession.appointments.sortedBy { it.createdAt }
    val nextAppointmentIndex = sortedAppointmentFromDeliverySession.indexOf(currentAppointment) + 1

    return sortedAppointmentFromDeliverySession.get(nextAppointmentIndex)
  }

  private fun linkAppointments(targetAppointment: Appointment, nextAppointment: Appointment): Appointment {
    targetAppointment.supersededByAppointmentId = nextAppointment.id
    appointmentRepository.save(targetAppointment)
    return targetAppointment
  }
}
