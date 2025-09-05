package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service

import jakarta.persistence.EntityExistsException
import jakarta.persistence.EntityNotFoundException
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.AddressDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ActionPlan
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Appointment
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AppointmentDeliveryType
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AppointmentSessionType
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AppointmentType.SERVICE_DELIVERY
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Attended
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AuthUser
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.DeliverySession
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.NoSessionReasonType
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Referral
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ActionPlanRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.AppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.AuthUserRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.DeliverySessionRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ReferralRepository
import java.time.OffsetDateTime
import java.util.UUID

@Service
@Transactional
class DeliverySessionService(
  val deliverySessionRepository: DeliverySessionRepository,
  val actionPlanRepository: ActionPlanRepository,
  val authUserRepository: AuthUserRepository,
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
    rescheduleRequestedBy: String? = null,
    rescheduledReason: String? = null,
    attended: Attended? = null,
    didSessionHappen: Boolean? = null,
    notifyProbationPractitionerOfBehaviour: Boolean? = null,
    notifyProbationPractitionerOfConcerns: Boolean? = null,
    late: Boolean? = null,
    lateReason: String? = null,
    futureSessionPlans: String? = null,
    noAttendanceInformation: String? = null,
    noSessionReasonType: NoSessionReasonType? = null,
    noSessionReasonPopAcceptable: String? = null,
    noSessionReasonPopUnacceptable: String? = null,
    noSessionReasonLogistics: String? = null,
    sessionSummary: String? = null,
    sessionResponse: String? = null,
    sessionBehaviour: String? = null,
    sessionConcerns: String? = null,
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
      notifyProbationPractitionerOfBehaviour,
      notifyProbationPractitionerOfConcerns,
      didSessionHappen,
      noSessionReasonType,
      rescheduleRequestedBy,
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

    existingAppointment?.let {
      it.supersededByAppointmentId = appointment.id
      it.superseded = true
      it.rescheduleRequestedBy = rescheduleRequestedBy
      it.rescheduledReason = rescheduledReason
    }

    session.appointments.map { appt ->
      {
        appt.superseded = true
        appt.supersededByAppointmentId = appointment.id
      }
    }
    appointmentRepository.saveAndFlush(appointment)
    if (existingAppointment != null) appointmentRepository.saveAndFlush(existingAppointment)
    appointmentService.createOrUpdateAppointmentDeliveryDetails(appointment, appointmentDeliveryType, appointmentSessionType, appointmentDeliveryAddress, npsOfficeCode)
    session.appointments.add(appointment)
    return deliverySessionRepository.saveAndFlush(session).also {
      // Occurring after saving the session to ensure that session has the latest appointment attached when publishing the session feedback event.
      setAttendanceAndFeedbackIfHistoricAppointment(
        session,
        appointment,
        attended,
        didSessionHappen,
        late,
        lateReason,
        futureSessionPlans,
        noAttendanceInformation,
        noSessionReasonType,
        noSessionReasonPopAcceptable,
        noSessionReasonPopUnacceptable,
        noSessionReasonLogistics,
        sessionSummary,
        sessionResponse,
        sessionBehaviour,
        sessionConcerns,
        notifyProbationPractitionerOfBehaviour,
        notifyProbationPractitionerOfConcerns,
        updatedBy,
      )
    }
  }

  // TODO: Returning Pair because Appointment does not link to DeliverySession. Suggestion to create a new DeliverySessionAppointment entity (it is currently embedded in DeliverySession)
  fun recordAttendanceFeedback(
    referralId: UUID,
    appointmentId: UUID,
    actor: AuthUser,
    attended: Attended,
    didSessionHappen: Boolean,
  ): Pair<DeliverySession, Appointment> {
    val sessionAndAppointment = getDeliverySessionAppointmentOrThrowException(referralId, appointmentId)
    if (sessionAndAppointment.second.appointmentTime.isAfter(OffsetDateTime.now())) {
      throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot submit feedback for a future appointment [id=${sessionAndAppointment.second.id}]")
    }
    val updatedAppointment = appointmentService.recordAppointmentAttendance(sessionAndAppointment.second, attended, didSessionHappen, actor)
    return Pair(sessionAndAppointment.first, updatedAppointment)
  }

  fun recordSessionFeedback(
    referralId: UUID,
    appointmentId: UUID,
    actor: AuthUser,
    late: Boolean?,
    lateReason: String?,
    futureSessionPlans: String?,
    noAttendanceInformation: String?,
    noSessionReasonType: NoSessionReasonType?,
    noSessionReasonPopAcceptable: String?,
    noSessionReasonPopUnacceptable: String?,
    noSessionReasonLogistics: String?,
    sessionSummary: String?,
    sessionResponse: String?,
    sessionBehaviour: String?,
    sessionConcerns: String?,
    notifyProbationPractitionerOfBehaviour: Boolean?,
    notifyProbationPractitionerOfConcerns: Boolean?,
  ): Pair<DeliverySession, Appointment> {
    val sessionAndAppointment = getDeliverySessionAppointmentOrThrowException(referralId, appointmentId)
    val updatedAppointment = appointmentService.recordSessionFeedback(
      sessionAndAppointment.second,
      late,
      lateReason,
      futureSessionPlans,
      noAttendanceInformation,
      noSessionReasonType,
      noSessionReasonPopAcceptable,
      noSessionReasonPopUnacceptable,
      noSessionReasonLogistics,
      sessionSummary,
      sessionResponse,
      sessionBehaviour,
      sessionConcerns,
      notifyProbationPractitionerOfBehaviour,
      notifyProbationPractitionerOfConcerns,
      actor,
    )
    return Pair(sessionAndAppointment.first, updatedAppointment)
  }

  fun submitAppointmentFeedback(referralId: UUID, appointmentId: UUID, submitter: AuthUser): Pair<DeliverySession, Appointment> {
    val sessionAndAppointment = getDeliverySessionAppointmentOrThrowException(referralId, appointmentId)
    val deliverySession = sessionAndAppointment.first
    val appointment = sessionAndAppointment.second
    if (appointment.appointmentTime.isAfter(OffsetDateTime.now())) {
      throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot submit feedback for a future appointment [id=${appointment.id}]")
    }
    val updatedAppointment = appointmentService.submitAppointmentFeedback(appointment, submitter, SERVICE_DELIVERY, deliverySession)
    return Pair(sessionAndAppointment.first, updatedAppointment)
  }

  fun getSessions(referralId: UUID): List<DeliverySession> = deliverySessionRepository.findAllByReferralId(referralId)

  fun getSession(referralId: UUID, sessionNumber: Int): DeliverySession = getDeliverySessionOrThrowException(referralId, sessionNumber)

  private fun setAttendanceAndFeedbackIfHistoricAppointment(
    session: DeliverySession,
    appointment: Appointment,
    attended: Attended?,
    didSessionHappen: Boolean?,
    late: Boolean?,
    lateReason: String?,
    futureSessionPlans: String?,
    noAttendanceInformation: String?,
    noSessionReasonType: NoSessionReasonType?,
    noSessionReasonPopAcceptable: String?,
    noSessionReasonPopUnacceptable: String?,
    noSessionReasonLogistics: String?,
    sessionSummary: String?,
    sessionResponse: String?,
    sessionBehaviour: String?,
    sessionConcerns: String?,
    notifyProbationPractitionerOfBehaviour: Boolean?,
    notifyProbationPractitionerOfConcerns: Boolean?,
    updatedBy: AuthUser,
  ) {
    attended?.let {
      setAttendanceFields(appointment, attended, didSessionHappen, updatedBy)
      setFeedbackFields(
        appointment,
        late,
        lateReason,
        futureSessionPlans,
        noAttendanceInformation,
        noSessionReasonType,
        noSessionReasonPopAcceptable,
        noSessionReasonPopUnacceptable,
        noSessionReasonLogistics,
        sessionSummary,
        sessionResponse,
        sessionBehaviour,
        sessionConcerns,
        notifyProbationPractitionerOfBehaviour,
        notifyProbationPractitionerOfConcerns,
        updatedBy,
      )
      appointmentService.submitAppointmentFeedback(appointment, updatedBy, SERVICE_DELIVERY, session)
    }
  }
  private fun setSuperseded(attended: Attended, appointment: Appointment) {
    when (attended) {
      Attended.LATE, Attended.YES -> {
        appointment.superseded = false
      }
      else -> return
    }
  }
  private fun setAttendanceFields(
    appointment: Appointment,
    attended: Attended,
    didSessionHappen: Boolean?,
    actor: AuthUser,
  ) {
    setSuperseded(attended, appointment)
    appointment.attended = attended
    appointment.didSessionHappen = didSessionHappen
    appointment.attendanceSubmittedAt = OffsetDateTime.now()
    appointment.attendanceSubmittedBy = authUserRepository.save(actor)
  }

  private fun setFeedbackFields(
    appointment: Appointment,
    late: Boolean?,
    lateReason: String?,
    futureSessionPlans: String?,
    noAttendanceInformation: String?,
    noSessionReasonType: NoSessionReasonType?,
    noSessionReasonPopAcceptable: String?,
    noSessionReasonPopUnacceptable: String?,
    noSessionReasonLogistics: String?,
    sessionSummary: String?,
    sessionResponse: String?,
    sessionBehaviour: String?,
    sessionConcerns: String?,
    notifyProbationPractitionerOfBehaviour: Boolean?,
    notifyProbationPractitionerOfConcerns: Boolean?,
    actor: AuthUser,
  ) {
    appointment.late = late
    appointment.lateReason = lateReason
    appointment.futureSessionPlans = futureSessionPlans
    appointment.noAttendanceInformation = noAttendanceInformation
    appointment.noSessionReasonType = noSessionReasonType
    appointment.noSessionReasonPopAcceptable = noSessionReasonPopAcceptable
    appointment.noSessionReasonPopUnacceptable = noSessionReasonPopUnacceptable
    appointment.noSessionReasonLogistics = noSessionReasonLogistics
    appointment.sessionSummary = sessionSummary
    appointment.sessionResponse = sessionResponse
    appointment.sessionConcerns = sessionConcerns
    appointment.sessionBehaviour = sessionBehaviour
    appointment.sessionFeedbackSubmittedAt = OffsetDateTime.now()
    appointment.sessionFeedbackSubmittedBy = authUserRepository.save(actor)
    appointment.notifyProbationPractitionerOfBehaviour = notifyProbationPractitionerOfBehaviour
    appointment.notifyProbationPractitionerOfConcerns = notifyProbationPractitionerOfConcerns
  }

  private fun checkSessionIsNotDuplicate(actionPlanId: UUID, sessionNumber: Int) {
    getDeliverySessionByActionPlanId(actionPlanId, sessionNumber)?.let {
      throw EntityExistsException("Action plan session already exists for [id=$actionPlanId, sessionNumber=$sessionNumber]")
    }
  }

  private fun getReferral(referralId: UUID): Referral = referralRepository.findById(referralId).orElseThrow {
    throw EntityNotFoundException("Referral not found [id=$referralId]")
  }

  fun getDeliverySessionByActionPlanIdOrThrowException(actionPlanId: UUID, sessionNumber: Int): DeliverySession = getDeliverySessionByActionPlanId(actionPlanId, sessionNumber)
    ?: throw EntityNotFoundException("Action plan session not found [actionPlanId=$actionPlanId, sessionNumber=$sessionNumber]")

  private fun getDeliverySessionOrThrowException(referralId: UUID, sessionNumber: Int): DeliverySession = getDeliverySession(referralId, sessionNumber)
    ?: throw EntityNotFoundException("Action plan session not found [referralId=$referralId, sessionNumber=$sessionNumber]")

  private fun getDeliverySession(referralId: UUID, sessionNumber: Int) = deliverySessionRepository.findByReferralIdAndSessionNumber(referralId, sessionNumber)

  private fun getDeliverySessionByActionPlanId(actionPlanId: UUID, sessionNumber: Int) = deliverySessionRepository.findAllByActionPlanIdAndSessionNumber(actionPlanId, sessionNumber)

  private fun getDeliverySessionAppointmentOrThrowException(referralId: UUID, appointmentId: UUID): Pair<DeliverySession, Appointment> = deliverySessionRepository.findAllByReferralId(referralId)
    .flatMap { session -> session.appointments.map { appointment -> Pair(session, appointment) } }
    .firstOrNull { appointment -> appointment.second.id == appointmentId } ?: throw EntityNotFoundException("No Delivery Session Appointment found [referralId=$referralId, appointmentId=$appointmentId]")
}
