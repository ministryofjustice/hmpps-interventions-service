package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.OneToOne
import jakarta.persistence.PrimaryKeyJoinColumn
import jakarta.validation.constraints.NotNull
import org.hibernate.annotations.JdbcType
import org.hibernate.dialect.PostgreSQLEnumJdbcType
import java.util.UUID

@Entity
data class AppointmentDelivery(
  @Id
  var appointmentId: UUID,

  @Column(columnDefinition = "appointment_delivery_type")
  @Enumerated(EnumType.STRING)
  @JdbcType(PostgreSQLEnumJdbcType::class)
  @NotNull
  var appointmentDeliveryType: AppointmentDeliveryType,

  // TODO: Transform to @NotNull this change is live
  @Column(columnDefinition = "appointment_session_type")
  @Enumerated(EnumType.STRING)
  @JdbcType(PostgreSQLEnumJdbcType::class)
  var appointmentSessionType: AppointmentSessionType? = null,

  var npsOfficeCode: String? = null,
  @OneToOne(cascade = [CascadeType.ALL])
  @PrimaryKeyJoinColumn
  var appointmentDeliveryAddress: AppointmentDeliveryAddress? = null,
)

enum class AppointmentDeliveryType {
  PHONE_CALL,
  VIDEO_CALL,
  IN_PERSON_MEETING_PROBATION_OFFICE,
  IN_PERSON_MEETING_OTHER,
}

enum class AppointmentSessionType {
  ONE_TO_ONE,
  GROUP,
}
