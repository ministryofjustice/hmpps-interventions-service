package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto

import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Appointment
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.SupplierAssessment
import java.time.OffsetDateTime
import java.util.UUID

data class SupplierAssessmentDTO(
  val id: UUID,
  val appointments: List<AppointmentDTO>,
  val currentAppointmentId: UUID,
  val referralId: UUID,
) {
  companion object {
    fun from(supplierAssessment: SupplierAssessment): SupplierAssessmentDTO {
      return SupplierAssessmentDTO(
        id = supplierAssessment.id,
        appointments = AppointmentDTO.from(supplierAssessment.appointments),
        currentAppointmentId = supplierAssessment.currentAppointment.id,
        referralId = supplierAssessment.referral.id
      )
    }
  }
}

data class SupplierAssessmentAppointmentDTO(
  val id: UUID,
  val appointmentTime: OffsetDateTime?,
  val durationInMinutes: Int?,
) {
  companion object {
    fun from(appointment: Appointment): SupplierAssessmentAppointmentDTO {
      return SupplierAssessmentAppointmentDTO(
        id = appointment.id,
        appointmentTime = appointment.appointmentTime,
        durationInMinutes = appointment.durationInMinutes
      )
    }
  }
}
