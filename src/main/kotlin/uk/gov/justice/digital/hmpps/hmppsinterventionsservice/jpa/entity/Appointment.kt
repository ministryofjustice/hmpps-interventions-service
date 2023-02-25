package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity

import com.fasterxml.jackson.annotation.JsonValue
import com.vladmihalcea.hibernate.type.basic.PostgreSQLEnumType
import org.hibernate.annotations.Fetch
import org.hibernate.annotations.FetchMode
import org.hibernate.type.YesNoConverter
import jakarta.persistence.Convert
import jakarta.persistence.ConvertDef
import java.time.OffsetDateTime
import java.util.UUID
import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToOne
import jakarta.persistence.PrimaryKeyJoinColumn
import jakarta.validation.constraints.NotNull

@Entity
@TypeDef(name = "attended", typeClass = PostgreSQLEnumType::class)
data class Appointment(
  @Type(type = "attended")
  @Enumerated(EnumType.STRING)
  var attended: Attended? = null,
  var additionalAttendanceInformation: String? = null,
  var attendanceFailureInformation: String? = null,
  var attendanceSubmittedAt: OffsetDateTime? = null,
  @ManyToOne
  @Fetch(FetchMode.JOIN)
  var attendanceSubmittedBy: AuthUser? = null,

  var attendanceBehaviour: String? = null,
  var attendanceBehaviourSubmittedAt: OffsetDateTime? = null,
  @ManyToOne
  @Fetch(FetchMode.JOIN)
  var attendanceBehaviourSubmittedBy: AuthUser? = null,

  var sessionSummary: String? = null,
  var sessionResponse: String? = null,
  var sessionConcerns: String? = null,
  var sessionFeedbackSubmittedAt: OffsetDateTime? = null,
  @ManyToOne
  @Fetch(FetchMode.JOIN)
  var sessionFeedbackSubmittedBy: AuthUser? = null,

  var notifyPPOfAttendanceBehaviour: Boolean? = null,

  var appointmentFeedbackSubmittedAt: OffsetDateTime? = null,
  @ManyToOne
  @Fetch(FetchMode.JOIN)
  var appointmentFeedbackSubmittedBy: AuthUser? = null,

  var deliusAppointmentId: Long? = null,

  @NotNull
  @ManyToOne
  @Fetch(FetchMode.JOIN)
  val createdBy: AuthUser,
  @NotNull val createdAt: OffsetDateTime,
  var appointmentTime: OffsetDateTime,
  var durationInMinutes: Int,

  @OneToOne(cascade = [CascadeType.ALL])
  @PrimaryKeyJoinColumn
  var appointmentDelivery: AppointmentDelivery? = null,

  @ManyToOne(fetch = FetchType.LAZY) var referral: Referral,
  var superseded: Boolean = false,
  @Id var id: UUID,
  var supersededByAppointmentId: UUID? = null,
  var stale: Boolean = false,
) {
  override fun equals(other: Any?): Boolean {
    if (other == null || other !is Appointment) {
      return false
    }

    return id == other.id
  }

  override fun hashCode(): Int {
    return id.hashCode()
  }
}

enum class Attended {
  YES,
  LATE,
  NO,
  ;

  @JsonValue
  open fun toLower(): String? {
    return this.toString().lowercase()
  }
}

enum class AppointmentType {
  SERVICE_DELIVERY,
  SUPPLIER_ASSESSMENT,
}
