package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util

import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Appointment
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Attended
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AuthUser
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Referral
import java.time.OffsetDateTime
import java.util.UUID

class AppointmentFactory(em: TestEntityManager? = null) : EntityFactory(em) {
  private val authUserFactory = AuthUserFactory(em)
  private val referralFactory = ReferralFactory(em)

  fun create(
    id: UUID = UUID.randomUUID(),
    createdAt: OffsetDateTime = OffsetDateTime.now(),
    createdBy: AuthUser = authUserFactory.create(),
    appointmentTime: OffsetDateTime = OffsetDateTime.now().plusMonths(2),
    durationInMinutes: Int = 60,
    attended: Attended? = null,
    additionalAttendanceInformation: String? = null,
    attendanceSubmittedAt: OffsetDateTime? = null,
    attendanceSubmittedBy: AuthUser? = null,
    attendanceBehaviour: String? = null,
    attendanceBehaviourSubmittedAt: OffsetDateTime? = null,
    notifyPPOfAttendanceBehaviour: Boolean? = null,
    appointmentFeedbackSubmittedAt: OffsetDateTime? = null,
    appointmentFeedbackSubmittedBy: AuthUser? = null,
    deliusAppointmentId: Long? = null,
    superseded: Boolean = false,
    referral: Referral = referralFactory.createSent(),
  ): Appointment {
    return save(
      Appointment(
        id = id,
        createdAt = createdAt,
        createdBy = createdBy,
        appointmentTime = appointmentTime,
        durationInMinutes = durationInMinutes,
        attended = attended,
        additionalAttendanceInformation = additionalAttendanceInformation,
        attendanceSubmittedAt = attendanceSubmittedAt,
        attendanceSubmittedBy = attendanceSubmittedBy,
        attendanceBehaviour = attendanceBehaviour,
        attendanceBehaviourSubmittedAt = attendanceBehaviourSubmittedAt,
        notifyPPOfAttendanceBehaviour = notifyPPOfAttendanceBehaviour,
        appointmentFeedbackSubmittedAt = appointmentFeedbackSubmittedAt,
        appointmentFeedbackSubmittedBy = appointmentFeedbackSubmittedBy,
        deliusAppointmentId = deliusAppointmentId,
        superseded = superseded,
        referral = referral,
      ),
    )
  }

  fun createDuplicateReferral(
    id: UUID = UUID.randomUUID(),
    createdAt: OffsetDateTime = OffsetDateTime.now(),
    createdBy: AuthUser = authUserFactory.create(),
    appointmentTime: OffsetDateTime = OffsetDateTime.now().plusMonths(2),
    durationInMinutes: Int = 60,
    attended: Attended? = null,
    additionalAttendanceInformation: String? = null,
    attendanceSubmittedAt: OffsetDateTime? = null,
    attendanceSubmittedBy: AuthUser? = null,
    attendanceBehaviour: String? = null,
    attendanceBehaviourSubmittedAt: OffsetDateTime? = null,
    notifyPPOfAttendanceBehaviour: Boolean? = null,
    appointmentFeedbackSubmittedAt: OffsetDateTime? = null,
    appointmentFeedbackSubmittedBy: AuthUser? = null,
    deliusAppointmentId: Long? = null,
    superseded: Boolean = false,
    referral: Referral,
  ): Appointment {
    return save(
      Appointment(
        id = id,
        createdAt = createdAt,
        createdBy = createdBy,
        appointmentTime = appointmentTime,
        durationInMinutes = durationInMinutes,
        attended = attended,
        additionalAttendanceInformation = additionalAttendanceInformation,
        attendanceSubmittedAt = attendanceSubmittedAt,
        attendanceSubmittedBy = attendanceSubmittedBy,
        attendanceBehaviour = attendanceBehaviour,
        attendanceBehaviourSubmittedAt = attendanceBehaviourSubmittedAt,
        notifyPPOfAttendanceBehaviour = notifyPPOfAttendanceBehaviour,
        appointmentFeedbackSubmittedAt = appointmentFeedbackSubmittedAt,
        appointmentFeedbackSubmittedBy = appointmentFeedbackSubmittedBy,
        deliusAppointmentId = deliusAppointmentId,
        superseded = superseded,
        referral = referral,
      ),
    )
  }
}
