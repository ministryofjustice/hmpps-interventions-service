package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity

import com.vladmihalcea.hibernate.type.basic.PostgreSQLEnumType
import org.hibernate.annotations.Type
import java.util.UUID
import javax.persistence.CascadeType
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.Id
import javax.persistence.OneToOne
import javax.persistence.PrimaryKeyJoinColumn
import javax.validation.constraints.NotNull

@Entity
data class AppointmentDelivery(
  @Id
  var appointmentId: UUID,

  @Type(PostgreSQLEnumType::class)
  @Column(columnDefinition = "appointment_delivery_type")
  @Enumerated(EnumType.STRING)
  @NotNull
  var appointmentDeliveryType: AppointmentDeliveryType,

  @Type(PostgreSQLEnumType::class)
  @Column(columnDefinition = "appointment_session_type")
  @Enumerated(EnumType.STRING)
  // TODO: Transform to @NotNull this change is live
  var appointmentSessionType: AppointmentSessionType? = null,

  @OneToOne(cascade = [CascadeType.ALL])
  @PrimaryKeyJoinColumn
  var appointmentDeliveryAddress: AppointmentDeliveryAddress? = null,

  var npsOfficeCode: String? = null,
)

enum class AppointmentDeliveryType {
  PHONE_CALL,
  VIDEO_CALL,
  IN_PERSON_MEETING_PROBATION_OFFICE,
  IN_PERSON_MEETING_OTHER;
}

enum class AppointmentSessionType {
  ONE_TO_ONE, GROUP
}
