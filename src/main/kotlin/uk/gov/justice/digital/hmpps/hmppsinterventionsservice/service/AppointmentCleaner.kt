package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service

import mu.KLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Appointment
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.AppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.DeliverySessionRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.SupplierAssessmentRepository

@Service
@Transactional
class AppointmentCleaner(
  val appointmentRepository: AppointmentRepository,
  val deliverySessionRepository: DeliverySessionRepository,
  val supplierAssessmentRepository: SupplierAssessmentRepository,
) {
  companion object : KLogging()

  @Transactional
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

  @Transactional
  fun markAppoinmentAsStale(appointment: Appointment): Appointment {
    appointment.stale = true
    appointmentRepository.save(appointment)
    return appointment
  }

  private fun getNextAppointmentInSupersededChain(currentAppointment: Appointment): Appointment? {
    val deliverySessions = deliverySessionRepository.findAllByReferralId(currentAppointment.referral.id)
    val targetDeliverySessionList = deliverySessions.filter { it.appointments.contains(currentAppointment) }

    if (!targetDeliverySessionList.isEmpty()) {
      // delivery session appointments
      val targetDeliverySession = targetDeliverySessionList.get(0)
      val sortedAppointmentFromDeliverySession = targetDeliverySession.appointments.sortedBy { it.createdAt }
      val nextAppointmentIndex = sortedAppointmentFromDeliverySession.indexOf(currentAppointment) + 1

      if (nextAppointmentIndex > sortedAppointmentFromDeliverySession.lastIndex) {
        logger.info("superseded delivery appointment ${currentAppointment.id} has no appointments created after it, skipping")
        return null
      }

      return sortedAppointmentFromDeliverySession.get(nextAppointmentIndex)
    } else {
      // supplier assessment appointments
      val referraL = currentAppointment.referral
      val supplierAssessment = referraL.supplierAssessment

      if (supplierAssessment != null) {
        val sortedAppointmentFromSupplierAssessment = supplierAssessment.appointments.sortedBy { it.createdAt }
        val nextAppointmentIndex = sortedAppointmentFromSupplierAssessment.indexOf(currentAppointment) + 1

        if (nextAppointmentIndex > sortedAppointmentFromSupplierAssessment.lastIndex) {
          logger.info("superseded supplier assessement appointment ${currentAppointment.id} has no appointments created after it, skipping")
          return null
        }

        return sortedAppointmentFromSupplierAssessment.get(nextAppointmentIndex)
      } else {
        logger.info("unable to find delivery session or supplier assessment for superseded appointment ${currentAppointment.id}")
        return null
      }
    }
  }

  private fun linkAppointments(targetAppointment: Appointment, nextAppointment: Appointment): Appointment {
    targetAppointment.supersededByAppointmentId = nextAppointment.id
    appointmentRepository.save(targetAppointment)
    return targetAppointment
  }
}
