package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto

import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Appointment
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AppointmentDeliveryType
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AppointmentSessionType
import java.time.OffsetDateTime
import java.util.UUID

data class AppointmentDTO(
  val id: UUID,
  val appointmentTime: OffsetDateTime?,
  val durationInMinutes: Int?,
  val appointmentFeedback: AppointmentFeedbackDTO,
  val appointmentDeliveryType: AppointmentDeliveryType?,
  val sessionType: AppointmentSessionType?,
  val appointmentDeliveryAddress: AddressDTO?,
  val npsOfficeCode: String?,
  val createdAt: OffsetDateTime?,
  val superseded: Boolean,
  val rescheduleRequestedBy: String?,
  val rescheduledReason: String?,
) {
  companion object {
    fun from(appointment: Appointment): AppointmentDTO {
      var addressDTO: AddressDTO? = null
      if (appointment.appointmentDelivery?.appointmentDeliveryType == AppointmentDeliveryType.IN_PERSON_MEETING_OTHER) {
        val address = appointment.appointmentDelivery?.appointmentDeliveryAddress
        if (address != null) {
          addressDTO = AddressDTO(address.firstAddressLine, address.secondAddressLine, address.townCity, address.county, address.postCode)
        }
      }
      return AppointmentDTO(
        id = appointment.id,
        appointmentTime = appointment.appointmentTime,
        durationInMinutes = appointment.durationInMinutes,
        appointmentFeedback = AppointmentFeedbackDTO.from(appointment),
        appointmentDeliveryType = appointment.appointmentDelivery?.appointmentDeliveryType,
        sessionType = appointment.appointmentDelivery?.appointmentSessionType,
        appointmentDeliveryAddress = addressDTO,
        npsOfficeCode = appointment.appointmentDelivery?.npsOfficeCode,
        createdAt = appointment.createdAt,
        superseded = appointment.superseded,
        rescheduleRequestedBy = appointment.rescheduleRequestedBy,
        rescheduledReason = appointment.rescheduledReason,
      )
    }
    fun from(appointments: MutableSet<Appointment>): List<AppointmentDTO> {
      return appointments.map { from(it) }
    }
  }
}
