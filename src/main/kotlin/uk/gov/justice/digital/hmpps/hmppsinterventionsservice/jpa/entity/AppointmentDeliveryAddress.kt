package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "appointment_delivery_address")
data class AppointmentDeliveryAddress(
  @Id
  var appointmentDeliveryId: UUID,
  var firstAddressLine: String,
  var secondAddressLine: String? = null,
  var townCity: String? = null,
  var county: String? = null,
  var postCode: String,
)
