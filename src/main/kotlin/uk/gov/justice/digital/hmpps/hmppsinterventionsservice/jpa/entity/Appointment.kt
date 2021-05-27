package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity

import com.fasterxml.jackson.annotation.JsonValue
import com.vladmihalcea.hibernate.type.basic.PostgreSQLEnumType
import org.hibernate.annotations.Fetch
import org.hibernate.annotations.FetchMode
import org.hibernate.annotations.Type
import org.hibernate.annotations.TypeDef
import java.time.OffsetDateTime
import java.util.*
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.Id
import javax.persistence.ManyToOne
import javax.persistence.Table
import javax.validation.constraints.NotNull

@Entity
@Table(name = "appointment")
@TypeDef(name = "attended", typeClass = PostgreSQLEnumType::class)
data class Appointment (
// Attributes
  @Type(type = "attended")
  @Enumerated(EnumType.STRING)
  var attended: Attended? = null,
  var additionalAttendanceInformation: String? = null,
  var attendanceSubmittedAt: OffsetDateTime? = null,
  var attendanceBehaviour: String? = null,
  var attendanceBehaviourSubmittedAt: OffsetDateTime? = null,
  var notifyPPOfAttendanceBehaviour: Boolean? = null,
  var sessionFeedbackSubmittedAt: OffsetDateTime? = null,

  // Activities
  var appointmentTime: OffsetDateTime? = null,
  var durationInMinutes: Int? = null,
  var deliusAppointmentId: Long? = null,

  // Status
  @NotNull @ManyToOne @Fetch(FetchMode.JOIN) val createdBy: AuthUser,
  @NotNull val createdAt: OffsetDateTime,

  @Id val id: UUID,
)

enum class Attended {
  YES,
  LATE,
  NO;

  @JsonValue
  open fun toLower(): String? {
    return this.toString().toLowerCase()
  }
}