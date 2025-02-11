package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util

import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Appointment
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Attended
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AuthUser
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.NoSessionReasonType
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
    attendanceFailureInformation: String? = null,
    noAttendanceInformation: String? = null,
    didSessionHappen: Boolean? = null,
    late: Boolean? = null,
    lateReason: String? = null,
    futureSessionPlans: String? = null,
    noSessionReasonType: NoSessionReasonType? = null,
    noSessionReasonPopAcceptable: String? = null,
    noSessionReasonPopUnacceptable: String? = null,
    noSessionReasonLogistics: String? = null,
    sessionSummary: String? = null,
    sessionResponse: String? = null,
    sessionConcerns: String? = null,
    sessionFeedbackSubmittedAt: OffsetDateTime? = null,
    sessionFeedbackSubmittedBy: AuthUser? = null,
    attendanceSubmittedAt: OffsetDateTime? = null,
    attendanceSubmittedBy: AuthUser? = null,
    attendanceBehaviour: String? = null,
    attendanceBehaviourSubmittedAt: OffsetDateTime? = null,
    notifyPPOfAttendanceBehaviour: Boolean? = null,
    appointmentFeedbackSubmittedAt: OffsetDateTime? = null,
    appointmentFeedbackSubmittedBy: AuthUser? = null,
    deliusAppointmentId: Long? = null,
    superseded: Boolean = false,
    supersededById: UUID? = null,
    referral: Referral = referralFactory.createSent(),
  ): Appointment = save(
    Appointment(
      id = id,
      createdAt = createdAt,
      createdBy = createdBy,
      appointmentTime = appointmentTime,
      durationInMinutes = durationInMinutes,
      attended = attended,
      attendanceFailureInformation = attendanceFailureInformation,
      didSessionHappen = didSessionHappen,
      late = late,
      lateReason = lateReason,
      futureSessionPlans = futureSessionPlans,
      noSessionReasonType = noSessionReasonType,
      noSessionReasonPopAcceptable = noSessionReasonPopAcceptable,
      noSessionReasonPopUnacceptable = noSessionReasonPopUnacceptable,
      noSessionReasonLogistics = noSessionReasonLogistics,
      noAttendanceInformation = noAttendanceInformation,
      sessionSummary = sessionSummary,
      sessionResponse = sessionResponse,
      sessionConcerns = sessionConcerns,
      sessionFeedbackSubmittedAt = sessionFeedbackSubmittedAt,
      sessionFeedbackSubmittedBy = sessionFeedbackSubmittedBy,
      attendanceSubmittedAt = attendanceSubmittedAt,
      attendanceSubmittedBy = attendanceSubmittedBy,
      attendanceBehaviour = attendanceBehaviour,
      attendanceBehaviourSubmittedAt = attendanceBehaviourSubmittedAt,
      notifyPPOfAttendanceBehaviour = notifyPPOfAttendanceBehaviour,
      appointmentFeedbackSubmittedAt = appointmentFeedbackSubmittedAt,
      appointmentFeedbackSubmittedBy = appointmentFeedbackSubmittedBy,
      deliusAppointmentId = deliusAppointmentId,
      superseded = superseded,
      supersededByAppointmentId = supersededById,
      referral = referral,
    ),
  )

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
  ): Appointment = save(
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
