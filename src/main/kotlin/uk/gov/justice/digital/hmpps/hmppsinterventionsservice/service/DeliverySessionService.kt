package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service

import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.config.ValidationError
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.AddressDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.ActionPlanAppointmentEventPublisher
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ActionPlan
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Appointment
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AppointmentDeliveryType
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AppointmentSessionType
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AppointmentType.SERVICE_DELIVERY
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Attended
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AuthUser
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.DeliverySession
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Referral
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ActionPlanRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.AppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.AuthUserRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.DeliverySessionRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ReferralRepository
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID
import javax.persistence.EntityExistsException
import javax.persistence.EntityNotFoundException

@Service
@Transactional
class DeliverySessionService(
  val deliverySessionRepository: DeliverySessionRepository,
  val actionPlanRepository: ActionPlanRepository,
  val authUserRepository: AuthUserRepository,
  val actionPlanAppointmentEventPublisher: ActionPlanAppointmentEventPublisher,
  val communityAPIBookingService: CommunityAPIBookingService,
  val appointmentService: AppointmentService,
  val appointmentRepository: AppointmentRepository,
  val referralRepository: ReferralRepository,
) {
  fun createUnscheduledSessionsForActionPlan(approvedActionPlan: ActionPlan) {
    val previouslyApprovedSessions = approvedActionPlan.referral.approvedActionPlan?. let {
      deliverySessionRepository.findAllByActionPlanId(it.id).count()
    } ?: 0

    val numberOfSessions = approvedActionPlan.numberOfSessions!!
    for (i in previouslyApprovedSessions + 1..numberOfSessions) {
      createSession(approvedActionPlan, i)
    }
  }

  private fun createSession(
    actionPlan: ActionPlan,
    sessionNumber: Int,
  ): DeliverySession {
    val actionPlanId = actionPlan.id
    checkSessionIsNotDuplicate(actionPlanId, sessionNumber)

    val session = DeliverySession(
      id = UUID.randomUUID(),
      sessionNumber = sessionNumber,
      referral = actionPlan.referral,
    )

    return deliverySessionRepository.save(session)
  }

  fun scheduleNewDeliverySessionAppointment(
    referralId: UUID,
    sessionNumber: Int,
    appointmentTime: OffsetDateTime,
    durationInMinutes: Int,
    createdBy: AuthUser,
    appointmentDeliveryType: AppointmentDeliveryType,
    appointmentSessionType: AppointmentSessionType,
    appointmentDeliveryAddress: AddressDTO? = null,
    npsOfficeCode: String? = null,
    attended: Attended? = null,
    additionalAttendanceInformation: String? = null,
    notifyProbationPractitioner: Boolean? = null,
    behaviourDescription: String? = null,
  ): DeliverySession {
    val session = getDeliverySession(referralId, sessionNumber) ?: throw EntityNotFoundException("Session not found for referral [referralId=$referralId, sessionNumber=$sessionNumber]")
    val existingAppointment = session.currentAppointment?.let {
      if (it.appointmentTime.isAfter(appointmentTime)) {
        throw EntityExistsException("can't schedule new appointment for session; new appointment occurs before previously scheduled appointment for session [referralId=$referralId, sessionNumber=$sessionNumber]")
      }
      val sentAtAtStartOfDay = getReferral(referralId).sentAt?.withHour(0)?.withMinute(0)?.withSecond(1)

      if (it.appointmentTime.isBefore(sentAtAtStartOfDay)) {
        throw ValidationError("can't schedule new appointment for session; new appointment occurs before referral creation date for session [referralId=$referralId, sessionNumber=$sessionNumber]", listOf())
      }

      val maximumDate =
        OffsetDateTime.now().plus(6, ChronoUnit.MONTHS).withHour(23).withMinute(59).withSecond(59)

      if (it.appointmentTime.isAfter(maximumDate)) {
        throw ValidationError("can't schedule new appointment for session; new appointment occurs later than 6 months from now [referralId=$referralId, sessionNumber=$sessionNumber]", listOf())
      }
      it.appointmentFeedbackSubmittedAt ?: throw ValidationError("can't schedule new appointment for session; latest appointment has no feedback delivered [referralId=$referralId, sessionNumber=$sessionNumber]", listOf())
      it
    }
    val appointment = Appointment(
      id = UUID.randomUUID(),
      createdBy = authUserRepository.save(createdBy),
      createdAt = OffsetDateTime.now(),
      appointmentTime = appointmentTime,
      durationInMinutes = durationInMinutes,
      referral = session.referral,
    )
    return scheduleDeliverySessionAppointment(
      session, appointment, existingAppointment, appointmentTime, durationInMinutes, appointmentDeliveryType, createdBy, appointmentSessionType, appointmentDeliveryAddress, npsOfficeCode, attended, additionalAttendanceInformation, notifyProbationPractitioner, behaviourDescription,
    )
  }

  fun rescheduleDeliverySessionAppointment(
    referralId: UUID,
    sessionNumber: Int,
    appointmentId: UUID,
    appointmentTime: OffsetDateTime,
    durationInMinutes: Int,
    updatedBy: AuthUser,
    appointmentDeliveryType: AppointmentDeliveryType,
    appointmentSessionType: AppointmentSessionType? = null,
    appointmentDeliveryAddress: AddressDTO? = null,
    npsOfficeCode: String? = null,
    attended: Attended? = null,
    additionalAttendanceInformation: String? = null,
    notifyProbationPractitioner: Boolean? = null,
    behaviourDescription: String? = null,
  ): DeliverySession {
    val session = getDeliverySession(referralId, sessionNumber) ?: throw EntityNotFoundException("Session not found for referral [referralId=$referralId, sessionNumber=$sessionNumber]")
    val existingAppointment = session.currentAppointment
    if (existingAppointment == null || existingAppointment.id !== appointmentId) {
      throw ValidationError("can't reschedule appointment for session; no appointment exists for session [referralId=$referralId, sessionNumber=$sessionNumber, appointmentId=$appointmentId]", listOf())
    }
    existingAppointment.appointmentFeedbackSubmittedAt?.let { throw ValidationError("can't reschedule appointment for session; appointment feedback already supplied [referralId=$referralId, sessionNumber=$sessionNumber, appointmentId=$appointmentId]", listOf()) }
    return scheduleDeliverySessionAppointment(
      session, existingAppointment, existingAppointment, appointmentTime, durationInMinutes, appointmentDeliveryType, updatedBy, appointmentSessionType, appointmentDeliveryAddress, npsOfficeCode, attended, additionalAttendanceInformation, notifyProbationPractitioner, behaviourDescription,
    )
  }

  private fun scheduleDeliverySessionAppointment(
    deliverySession: DeliverySession,
    appointmentToSchedule: Appointment,
    latestAppointment: Appointment?,
    appointmentTime: OffsetDateTime,
    durationInMinutes: Int,
    appointmentDeliveryType: AppointmentDeliveryType,
    scheduledBy: AuthUser,
    appointmentSessionType: AppointmentSessionType? = null,
    appointmentDeliveryAddress: AddressDTO? = null,
    npsOfficeCode: String? = null,
    attended: Attended? = null,
    additionalAttendanceInformation: String? = null,
    notifyProbationPractitioner: Boolean? = null,
    behaviourDescription: String? = null,
  ): DeliverySession {
    val (deliusAppointmentId, _) = communityAPIBookingService.book(
      deliverySession.referral,
      latestAppointment,
      appointmentTime,
      durationInMinutes,
      SERVICE_DELIVERY,
      npsOfficeCode,
    )
    if (appointmentTime.isBefore(OffsetDateTime.now()) && attended == null) {
      throw IllegalArgumentException("Appointment feedback must be provided for appointments in the past.")
    }
    appointmentToSchedule.appointmentTime = appointmentTime
    appointmentToSchedule.durationInMinutes = durationInMinutes
    appointmentToSchedule.deliusAppointmentId = deliusAppointmentId
    appointmentRepository.saveAndFlush(appointmentToSchedule)
    appointmentService.createOrUpdateAppointmentDeliveryDetails(appointmentToSchedule, appointmentDeliveryType, appointmentSessionType, appointmentDeliveryAddress, npsOfficeCode)
    deliverySession.appointments.add(appointmentToSchedule)
    return deliverySessionRepository.saveAndFlush(deliverySession).also {
      // Occurring after saving the session to ensure that session has the latest appointment attached when publishing the session feedback event.
      setAttendanceAndBehaviourIfHistoricAppointment(deliverySession, appointmentToSchedule, attended, additionalAttendanceInformation, behaviourDescription, notifyProbationPractitioner, scheduledBy)
    }
  }

  @Deprecated("superseded by scheduleNewDeliverySessionAppointment and rescheduleDeliverySessionAppointment")
  fun updateSessionAppointment(
    actionPlanId: UUID,
    sessionNumber: Int,
    appointmentTime: OffsetDateTime,
    durationInMinutes: Int,
    updatedBy: AuthUser,
    appointmentDeliveryType: AppointmentDeliveryType,
    appointmentSessionType: AppointmentSessionType? = null,
    appointmentDeliveryAddress: AddressDTO? = null,
    npsOfficeCode: String? = null,
    attended: Attended? = null,
    additionalAttendanceInformation: String? = null,
    notifyProbationPractitioner: Boolean? = null,
    behaviourDescription: String? = null,
  ): DeliverySession {
    val session = getDeliverySessionByActionPlanIdOrThrowException(actionPlanId, sessionNumber)
    val existingAppointment = session.currentAppointment

    // TODO: Some code duplication here with AppointmentService.kt
    val (deliusAppointmentId, appointmentId) = communityAPIBookingService.book(
      session.referral,
      if (existingAppointment?.attended != Attended.NO) existingAppointment else null,
      appointmentTime,
      durationInMinutes,
      SERVICE_DELIVERY,
      npsOfficeCode,
      attended,
      notifyProbationPractitioner,
    )

    // creating a new appointment from the existing appointment or else create a new appointment
    val appointment = Appointment(
      id = appointmentId ?: UUID.randomUUID(),
      durationInMinutes = durationInMinutes,
      appointmentTime = appointmentTime,
      deliusAppointmentId = deliusAppointmentId,
      createdAt = OffsetDateTime.now(),
      createdBy = existingAppointment?.createdBy ?: authUserRepository.save(updatedBy),
      referral = existingAppointment?.referral ?: session.referral,
    )
    session.appointments.map { appt -> appt.superseded = true }
    appointmentRepository.saveAndFlush(appointment)
    appointmentService.createOrUpdateAppointmentDeliveryDetails(appointment, appointmentDeliveryType, appointmentSessionType, appointmentDeliveryAddress, npsOfficeCode)
    session.appointments.add(appointment)
    deliverySessionRepository.save(session)
    return deliverySessionRepository.save(session).also {
      // Occuring after saving the session to ensure that session has the latest appointment attached when publishing the session feedback event.
      setAttendanceAndBehaviourIfHistoricAppointment(session, appointment, attended, additionalAttendanceInformation, behaviourDescription, notifyProbationPractitioner, updatedBy)
    }
  }

  @Deprecated("Deprecated in favour of method that uses common AppointmentService")
  fun recordAppointmentAttendance(
    actor: AuthUser,
    actionPlanId: UUID,
    sessionNumber: Int,
    attended: Attended,
    additionalInformation: String?,
  ): DeliverySession {
    val session = getDeliverySessionByActionPlanIdOrThrowException(actionPlanId, sessionNumber)
    val appointment = session.currentAppointment
      ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "can't record appointment attendance; no appointments have been booked for this session")

    if (appointment.appointmentFeedbackSubmittedAt != null) {
      throw ResponseStatusException(HttpStatus.CONFLICT, "session feedback has already been submitted for this session")
    }

    setAttendanceFields(appointment, attended, additionalInformation, actor)
    return deliverySessionRepository.save(session)
  }

  // TODO: Returning Pair because Appointment does not link to DeliverySession. Suggestion to create a new DeliverySessionAppointment entity (it is currently embedded in DeliverySession)
  fun recordAppointmentAttendance(
    referralId: UUID,
    appointmentId: UUID,
    actor: AuthUser,
    attended: Attended,
    additionalInformation: String?,
  ): Pair<DeliverySession, Appointment> {
    val sessionAndAppointment = getDeliverySessionAppointmentOrThrowException(referralId, appointmentId)
    if (sessionAndAppointment.second.appointmentTime.isAfter(OffsetDateTime.now())) {
      throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot submit feedback for a future appointment [id=${sessionAndAppointment.second.id}]")
    }
    val updatedAppointment = appointmentService.recordAppointmentAttendance(sessionAndAppointment.second, attended, additionalInformation, actor)
    return Pair(sessionAndAppointment.first, updatedAppointment)
  }

  @Deprecated("Deprecated in favour of method that uses common AppointmentService")
  fun recordBehaviour(
    actor: AuthUser,
    actionPlanId: UUID,
    sessionNumber: Int,
    behaviourDescription: String,
    notifyProbationPractitioner: Boolean,
  ): DeliverySession {
    val session = getDeliverySessionByActionPlanIdOrThrowException(actionPlanId, sessionNumber)
    val appointment = session.currentAppointment
      ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "can't record appointment behaviour; no appointments have been booked for this session")

    if (appointment.appointmentFeedbackSubmittedAt != null) {
      throw ResponseStatusException(HttpStatus.CONFLICT, "session feedback has already been submitted for this session")
    }

    setBehaviourFields(appointment, behaviourDescription, notifyProbationPractitioner, actor)
    appointmentRepository.save(appointment)
    return deliverySessionRepository.save(session)
  }

  fun recordAppointmentBehaviour(
    referralId: UUID,
    appointmentId: UUID,
    actor: AuthUser,
    behaviourDescription: String,
    notifyProbationPractitioner: Boolean,
  ): Pair<DeliverySession, Appointment> {
    val sessionAndAppointment = getDeliverySessionAppointmentOrThrowException(referralId, appointmentId)
    val updatedAppointment = appointmentService.recordBehaviour(sessionAndAppointment.second, behaviourDescription, notifyProbationPractitioner, actor)
    return Pair(sessionAndAppointment.first, updatedAppointment)
  }

  @Deprecated("Deprecated in favour of method that uses common AppointmentService")
  fun submitSessionFeedback(actionPlanId: UUID, sessionNumber: Int, submitter: AuthUser): DeliverySession {
    val session = getDeliverySessionByActionPlanIdOrThrowException(actionPlanId, sessionNumber)
    val appointment = session.currentAppointment
    this.submitSessionFeedback(session, appointment, submitter)
    return session
  }

  private fun submitSessionFeedback(session: DeliverySession, appointment: Appointment?, submitter: AuthUser) {
    if (appointment?.appointmentFeedbackSubmittedAt != null) {
      throw ResponseStatusException(HttpStatus.CONFLICT, "session feedback has already been submitted for this session")
    }

    if (appointment?.attendanceSubmittedAt == null) {
      throw ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "can't submit session feedback unless attendance has been recorded")
    }

    if (appointment.appointmentTime.isAfter(OffsetDateTime.now())) {
      throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot submit feedback for a future appointment [id=${appointment.id}]")
    }

    appointment.appointmentFeedbackSubmittedAt = OffsetDateTime.now()
    appointment.appointmentFeedbackSubmittedBy = authUserRepository.save(submitter)
    appointmentRepository.saveAndFlush(appointment)
    deliverySessionRepository.save(session)

    actionPlanAppointmentEventPublisher.attendanceRecordedEvent(session)

    if (appointment.attendanceBehaviourSubmittedAt != null) { // excluding the case of non attendance
      actionPlanAppointmentEventPublisher.behaviourRecordedEvent(session)
    }

    actionPlanAppointmentEventPublisher.sessionFeedbackRecordedEvent(session)
  }

  fun submitSessionFeedback(referralId: UUID, appointmentId: UUID, submitter: AuthUser): Pair<DeliverySession, Appointment> {
    val sessionAndAppointment = getDeliverySessionAppointmentOrThrowException(referralId, appointmentId)
    val appointment = sessionAndAppointment.second
    if (appointment.appointmentTime.isAfter(OffsetDateTime.now())) {
      throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot submit feedback for a future appointment [id=${appointment.id}]")
    }
    val updatedAppointment = appointmentService.submitSessionFeedback(appointment, submitter, SERVICE_DELIVERY)
    return Pair(sessionAndAppointment.first, updatedAppointment)
  }

  fun getSessions(referralId: UUID): List<DeliverySession> {
    return deliverySessionRepository.findAllByReferralId(referralId)
  }

  fun getSession(referralId: UUID, sessionNumber: Int): DeliverySession {
    return getDeliverySessionOrThrowException(referralId, sessionNumber)
  }

  private fun setAttendanceAndBehaviourIfHistoricAppointment(
    session: DeliverySession,
    appointment: Appointment,
    attended: Attended?,
    additionalAttendanceInformation: String?,
    behaviourDescription: String?,
    notifyProbationPractitioner: Boolean?,
    updatedBy: AuthUser,
  ) {
    attended?.let {
      setAttendanceFields(appointment, attended, additionalAttendanceInformation, updatedBy)
      if (attended != Attended.NO) {
        setBehaviourFields(appointment, behaviourDescription!!, notifyProbationPractitioner!!, updatedBy)
      }
      this.submitSessionFeedback(session, appointment, updatedBy)
    }
  }
  private fun setSuperseded(attended: Attended, appointment: Appointment) {
    when (attended) {
      Attended.LATE, Attended.YES -> { appointment.superseded = false }
      else -> return
    }
  }
  private fun setAttendanceFields(
    appointment: Appointment,
    attended: Attended,
    additionalInformation: String?,
    actor: AuthUser,
  ) {
    setSuperseded(attended, appointment)
    appointment.attended = attended
    additionalInformation?.let { appointment.additionalAttendanceInformation = additionalInformation }
    appointment.attendanceSubmittedAt = OffsetDateTime.now()
    appointment.attendanceSubmittedBy = authUserRepository.save(actor)
  }

  private fun setBehaviourFields(
    appointment: Appointment,
    behaviour: String,
    notifyProbationPractitioner: Boolean,
    actor: AuthUser,
  ) {
    appointment.attendanceBehaviour = behaviour
    appointment.attendanceBehaviourSubmittedAt = OffsetDateTime.now()
    appointment.attendanceBehaviourSubmittedBy = authUserRepository.save(actor)
    appointment.notifyPPOfAttendanceBehaviour = notifyProbationPractitioner
  }

  private fun checkSessionIsNotDuplicate(actionPlanId: UUID, sessionNumber: Int) {
    getDeliverySessionByActionPlanId(actionPlanId, sessionNumber)?.let {
      throw EntityExistsException("Action plan session already exists for [id=$actionPlanId, sessionNumber=$sessionNumber]")
    }
  }

  private fun getReferral(referralId: UUID): Referral {
    return referralRepository.findById(referralId).orElseThrow {
      throw EntityNotFoundException("Referral not found [id=$referralId]")
    }
  }

  @Deprecated(
    "looking up by action plan ID is no longer necessary",
    ReplaceWith("getDeliverySessionOrThrowException(referralId, sessionNumber)"),
  )
  fun getDeliverySessionByActionPlanIdOrThrowException(actionPlanId: UUID, sessionNumber: Int): DeliverySession =
    getDeliverySessionByActionPlanId(actionPlanId, sessionNumber)
      ?: throw EntityNotFoundException("Action plan session not found [actionPlanId=$actionPlanId, sessionNumber=$sessionNumber]")

  private fun getDeliverySessionOrThrowException(referralId: UUID, sessionNumber: Int): DeliverySession =
    getDeliverySession(referralId, sessionNumber)
      ?: throw EntityNotFoundException("Action plan session not found [referralId=$referralId, sessionNumber=$sessionNumber]")

  private fun getDeliverySession(referralId: UUID, sessionNumber: Int) =
    deliverySessionRepository.findByReferralIdAndSessionNumber(referralId, sessionNumber)

  @Deprecated(
    "looking up by action plan ID is no longer necessary",
    ReplaceWith("getDeliverySession(referralId, sessionNumber)"),
  )
  private fun getDeliverySessionByActionPlanId(actionPlanId: UUID, sessionNumber: Int) =
    deliverySessionRepository.findAllByActionPlanIdAndSessionNumber(actionPlanId, sessionNumber)

  private fun getDeliverySessionAppointmentOrThrowException(referralId: UUID, appointmentId: UUID): Pair<DeliverySession, Appointment> = deliverySessionRepository.findAllByReferralId(referralId)
    .flatMap { session -> session.appointments.map { appointment -> Pair(session, appointment) } }
    .firstOrNull { appointment -> appointment.second.id == appointmentId } ?: throw EntityNotFoundException("No Delivery Session Appointment found [referralId=$referralId, appointmentId=$appointmentId]")
}
