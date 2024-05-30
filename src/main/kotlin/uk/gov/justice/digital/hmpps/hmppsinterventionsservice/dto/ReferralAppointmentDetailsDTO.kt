package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto

import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Appointment
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.ReferralAppointmentLocationDetails
import java.time.OffsetDateTime
import java.util.UUID

class ReferralAppointmentDetailsDTO(
  val crn: String,
  val referrals: List<ReferralDetailDTO>,
) {

  companion object {
    fun from(crn: String, referralAppointmentDetails: List<ReferralAppointmentLocationDetails>): ReferralAppointmentDetailsDTO {
      return ReferralAppointmentDetailsDTO(
        crn = crn,
        referrals = referralAppointmentDetails.map { ReferralDetailDTO.from(it) },
      )
    }
  }
}

class ReferralDetailDTO(
  val referral_number: String?,
  val appointments: List<AppointmentDetailsDTO>,
) {
  companion object {
    fun from(referralAppointmentDetails: ReferralAppointmentLocationDetails): ReferralDetailDTO {
      return ReferralDetailDTO(
        referral_number = referralAppointmentDetails.referral.referenceNumber,
        appointments = AppointmentDetailsDTO.from(referralAppointmentDetails.appointments),
      )
    }
  }
}

class AppointmentDetailsDTO(
  val appointment_id: UUID?,
  val appointment_date_time: OffsetDateTime?,
  val appointment_duration_in_minutes: Int?,
  val superseded_indicator: Boolean?,
  val appointment_delivery_first_address_line: String?,
  val appointment_delivery_second_address_line: String?,
  val appointment_delivery_town_city: String?,
  val appointment_delivery_county: String?,
  val appointment_delivery_postcode: String?,
) {
  companion object {

    fun from(appointments: List<Appointment>): List<AppointmentDetailsDTO> {
      return appointments.map {
        AppointmentDetailsDTO(
          appointment_id = it.id,
          appointment_date_time = it.appointmentTime,
          appointment_duration_in_minutes = it.durationInMinutes,
          superseded_indicator = it.superseded,
          appointment_delivery_first_address_line = it.appointmentDelivery?.appointmentDeliveryAddress?.firstAddressLine ?: "",
          appointment_delivery_second_address_line = it.appointmentDelivery?.appointmentDeliveryAddress?.secondAddressLine ?: "",
          appointment_delivery_town_city = it.appointmentDelivery?.appointmentDeliveryAddress?.townCity ?: "",
          appointment_delivery_county = it.appointmentDelivery?.appointmentDeliveryAddress?.county ?: "",
          appointment_delivery_postcode = it.appointmentDelivery?.appointmentDeliveryAddress?.postCode ?: "",
        )
      }
    }
  }
}
