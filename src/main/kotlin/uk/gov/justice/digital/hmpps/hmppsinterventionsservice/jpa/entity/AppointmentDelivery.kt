package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity

import com.vladmihalcea.hibernate.type.basic.PostgreSQLEnumType
import jakarta.persistence.Convert
import jakarta.persistence.ConvertDef
import org.hibernate.type.YesNoConverter
import jakarta.persistence.ConvertDefs
import java.util.UUID
import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.OneToOne
import jakarta.persistence.PrimaryKeyJoinColumn
import jakarta.validation.constraints.NotNull

@Entity
@TypeDefs(
  value = [
    TypeDef(name = "appointment_delivery_type", typeClass = PostgreSQLEnumType::class),
    TypeDef(name = "appointment_session_type", typeClass = PostgreSQLEnumType::class),
  ],
)
data class AppointmentDelivery(
  @Id
  var appointmentId: UUID,
  @Type(type = "appointment_delivery_type")
  @Enumerated(EnumType.STRING)
  @NotNull
  var appointmentDeliveryType: AppointmentDeliveryType,
  // TODO: Transform to @NotNull this change is live
  @Type(type = "appointment_session_type")
  @Enumerated(EnumType.STRING)
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
  ;
}

enum class AppointmentSessionType {
  ONE_TO_ONE, GROUP
}
