package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util

import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Appointment
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Attended
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AuthUser
import java.time.OffsetDateTime
import java.util.UUID

class AppointmentFactory(em: TestEntityManager? = null) : EntityFactory(em) {
  private val authUserFactory = AuthUserFactory(em)

  fun create(
    id: UUID = UUID.randomUUID(),
    createdAt: OffsetDateTime = OffsetDateTime.now(),
    createdBy: AuthUser = authUserFactory.create(),
    attended: Attended? = null,
    additionalAttendanceInformation: String? = null,
    attendanceSubmittedAt: OffsetDateTime? = null,
    notifyPPOfAttendanceBehaviour: Boolean? = null,
    sessionFeedbackSubmittedAt: OffsetDateTime? = null,
  ): Appointment {
    return save(
      Appointment(
        id = id,
        createdAt = createdAt,
        createdBy = createdBy,
        attended = attended,
        additionalAttendanceInformation = additionalAttendanceInformation,
        attendanceSubmittedAt = attendanceSubmittedAt,
        notifyPPOfAttendanceBehaviour = notifyPPOfAttendanceBehaviour,
        sessionFeedbackSubmittedAt = sessionFeedbackSubmittedAt,
      )
    )
  }

  fun createAttended(
    id: UUID = UUID.randomUUID(),
    createdAt: OffsetDateTime = OffsetDateTime.now(),
    createdBy: AuthUser = authUserFactory.create(),
    attended: Attended? = Attended.YES,
    attendanceSubmittedAt: OffsetDateTime? = createdAt,
    notifyPPOfAttendanceBehaviour: Boolean? = false,
    sessionFeedbackSubmittedAt: OffsetDateTime? = attendanceSubmittedAt,
  ): Appointment {
    return save(
      Appointment(
        id = id,
        createdAt = createdAt,
        createdBy = createdBy,
        attended = attended,
        attendanceSubmittedAt = attendanceSubmittedAt,
        notifyPPOfAttendanceBehaviour = notifyPPOfAttendanceBehaviour,
        sessionFeedbackSubmittedAt = sessionFeedbackSubmittedAt,
      )
    )
  }
}
