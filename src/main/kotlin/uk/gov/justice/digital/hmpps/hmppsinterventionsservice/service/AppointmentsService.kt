package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service

import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.AppointmentEventPublisher
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ActionPlan
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ActionPlanSession
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Appointment
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Attended
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AuthUser
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Referral
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.SupplierAssessment
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ActionPlanRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ActionPlanSessionRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.AppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.AuthUserRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ReferralRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.SupplierAssessmentRepository
import java.time.OffsetDateTime
import java.util.UUID
import javax.persistence.EntityExistsException
import javax.persistence.EntityNotFoundException
import javax.transaction.Transactional

@Service
@Transactional
class AppointmentsService(
  val actionPlanSessionRepository: ActionPlanSessionRepository,
  val actionPlanRepository: ActionPlanRepository,
  val authUserRepository: AuthUserRepository,
  val appointmentRepository: AppointmentRepository,
  val appointmentEventPublisher: AppointmentEventPublisher,
  val communityAPIBookingService: CommunityAPIBookingService,
  val supplierAssessmentRepository: SupplierAssessmentRepository,
  val referralRepository: ReferralRepository,
) {
  fun createAppointment(
    actionPlan: ActionPlan,
    sessionNumber: Int,
    appointmentTime: OffsetDateTime?,
    durationInMinutes: Int?,
    createdByUser: AuthUser,
  ): ActionPlanSession {
    val actionPlanId = actionPlan.id
    checkAppointmentSessionIsNotDuplicate(actionPlanId, sessionNumber)

    val appointment = ActionPlanSession(
      id = UUID.randomUUID(),
      sessionNumber = sessionNumber,
      appointments = setOf(
        appointmentRepository.save(
          Appointment(
            id = UUID.randomUUID(), appointmentTime = appointmentTime,
            durationInMinutes = durationInMinutes,
            createdBy = authUserRepository.save(createdByUser),
            createdAt = OffsetDateTime.now()
          )
        )
      ),
      actionPlan = actionPlan,
    )

    return actionPlanSessionRepository.save(appointment)
  }

  fun createUnscheduledAppointmentsForActionPlan(submittedActionPlan: ActionPlan, actionPlanSubmitter: AuthUser) {
    val numberOfSessions = submittedActionPlan.numberOfSessions!!
    for (i in 1..numberOfSessions) {
      createAppointment(submittedActionPlan, i, null, null, actionPlanSubmitter)
    }
  }

  fun updateActionPlanSessionAppointment(
    actionPlanId: UUID,
    sessionNumber: Int,
    appointmentTime: OffsetDateTime?,
    durationInMinutes: Int?
  ): ActionPlanSession {

    val actionPlanSession = getActionPlanSessionOrThrowException(actionPlanId, sessionNumber)
    communityAPIBookingService.book(actionPlanSession, appointmentTime, durationInMinutes)?.let {
      actionPlanSession.appointment.deliusAppointmentId = it
    }

    mergeAppointment(actionPlanSession, appointmentTime, durationInMinutes)
    return actionPlanSessionRepository.save(actionPlanSession)
  }

  fun recordActionPlanSessionAttendance(
    actionPlanId: UUID,
    sessionNumber: Int,
    attended: Attended,
    additionalInformation: String?
  ): ActionPlanSession {
    val actionPlanSession = getActionPlanSessionOrThrowException(actionPlanId, sessionNumber)

    if (actionPlanSession.appointment.sessionFeedbackSubmittedAt != null) {
      throw ResponseStatusException(HttpStatus.CONFLICT, "session feedback has already been submitted for this appointment")
    }

    setAttendanceFields(actionPlanSession, attended, additionalInformation)
    return actionPlanSessionRepository.save(actionPlanSession)
  }

  fun recordActionPlanSessionBehaviour(
    actionPlanId: UUID,
    sessionNumber: Int,
    behaviourDescription: String,
    notifyProbationPractitioner: Boolean,
  ): ActionPlanSession {
    val actionPlanSession = getActionPlanSessionOrThrowException(actionPlanId, sessionNumber)

    if (actionPlanSession.appointment.sessionFeedbackSubmittedAt != null) {
      throw ResponseStatusException(HttpStatus.CONFLICT, "session feedback has already been submitted for this appointment")
    }

    setBehaviourFields(actionPlanSession, behaviourDescription, notifyProbationPractitioner)
    return actionPlanSessionRepository.save(actionPlanSession)
  }

  fun submitActionPlanSessionFeedback(actionPlanId: UUID, sessionNumber: Int): ActionPlanSession {
    val actionPlanSession = getActionPlanSessionOrThrowException(actionPlanId, sessionNumber)

    if (actionPlanSession.appointment.sessionFeedbackSubmittedAt != null) {
      throw ResponseStatusException(HttpStatus.CONFLICT, "session feedback has already been submitted for this appointment")
    }

    if (actionPlanSession.appointment.attendanceSubmittedAt == null) {
      throw ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "can't submit session feedback unless attendance has been recorded")
    }

    actionPlanSession.appointment.sessionFeedbackSubmittedAt = OffsetDateTime.now()
    actionPlanSessionRepository.save(actionPlanSession)

    appointmentEventPublisher.attendanceRecordedEvent(actionPlanSession, actionPlanSession.appointment.attended == Attended.NO)
    appointmentEventPublisher.behaviourRecordedEvent(actionPlanSession, actionPlanSession.appointment.notifyPPOfAttendanceBehaviour!!)
    appointmentEventPublisher.sessionFeedbackRecordedEvent(actionPlanSession, actionPlanSession.appointment.notifyPPOfAttendanceBehaviour!!)
    return actionPlanSession
  }

  fun getActionPlanSessionAppointments(actionPlanId: UUID): List<ActionPlanSession> {
    return actionPlanSessionRepository.findAllByActionPlanId(actionPlanId)
  }

  fun getActionPlanSessionAppointment(actionPlanId: UUID, sessionNumber: Int): ActionPlanSession {
    return getActionPlanSessionOrThrowException(actionPlanId, sessionNumber)
  }

  private fun setAttendanceFields(
    actionPlanSession: ActionPlanSession,
    attended: Attended,
    additionalInformation: String?
  ) {
    actionPlanSession.appointment.attended = attended
    additionalInformation?.let { actionPlanSession.appointment.additionalAttendanceInformation = additionalInformation }
    actionPlanSession.appointment.attendanceSubmittedAt = OffsetDateTime.now()
  }

  private fun setBehaviourFields(
    actionPlanSession: ActionPlanSession,
    behaviour: String,
    notifyProbationPractitioner: Boolean,
  ) {
    actionPlanSession.appointment.attendanceBehaviour = behaviour
    actionPlanSession.appointment.attendanceBehaviourSubmittedAt = OffsetDateTime.now()
    actionPlanSession.appointment.notifyPPOfAttendanceBehaviour = notifyProbationPractitioner
  }

  private fun checkAppointmentSessionIsNotDuplicate(actionPlanId: UUID, sessionNumber: Int) {
    getActionPlanAppointment(actionPlanId, sessionNumber)?.let {
      throw EntityExistsException("Action plan appointment already exists for [id=$actionPlanId, sessionNumber=$sessionNumber]")
    }
  }

  private fun getActionPlanSessionOrThrowException(actionPlanId: UUID, sessionNumber: Int): ActionPlanSession =

    getActionPlanAppointment(actionPlanId, sessionNumber)
      ?: throw EntityNotFoundException("Action plan appointment not found [id=$actionPlanId, sessionNumber=$sessionNumber]")

  private fun getActionPlanAppointment(actionPlanId: UUID, sessionNumber: Int) =
    actionPlanSessionRepository.findByActionPlanIdAndSessionNumber(actionPlanId, sessionNumber)

  private fun mergeAppointment(
    actionPlanSession: ActionPlanSession,
    appointmentTime: OffsetDateTime?,
    durationInMinutes: Int?
  ) {
    appointmentTime?.let { actionPlanSession.appointment.appointmentTime = it }
    durationInMinutes?.let { actionPlanSession.appointment.durationInMinutes = it }
  }

  fun createInitialAssessment(
    referral: Referral,
    createdByUser: AuthUser
  ): Referral {

    referral.supplierAssessment = supplierAssessmentRepository.save(
      SupplierAssessment(
        id = UUID.randomUUID(),
        referral = referral,
        appointments = setOf(
          appointmentRepository.save(
            Appointment(
              id = UUID.randomUUID(),
              createdBy = authUserRepository.save(createdByUser),
              createdAt = OffsetDateTime.now()
            )
          )
        ),
      )
    )
    return referralRepository.save(referral)
  }

  fun updateInitialAssessment(
    referral: Referral,
    durationInMinutes: Int?,
    appointmentTime: OffsetDateTime?
  ): SupplierAssessment {
    durationInMinutes?.let { referral.supplierAssessment!!.appointment.durationInMinutes = durationInMinutes }
    appointmentTime?. let { referral.supplierAssessment!!.appointment.appointmentTime = appointmentTime }

    return supplierAssessmentRepository.save(referral.supplierAssessment!!)
  }
}
