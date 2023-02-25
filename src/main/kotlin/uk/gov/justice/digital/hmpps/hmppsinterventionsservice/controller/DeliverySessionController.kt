package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.controller

import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.authorization.ReferralAccessChecker
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.authorization.UserMapper
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.component.LocationMapper
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.AttendanceFeedbackRequestDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.DeliverySessionAppointmentDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.DeliverySessionAppointmentScheduleDetailsRequestDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.DeliverySessionDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.SessionFeedbackRequestDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.UpdateAppointmentDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.ActionPlanService
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.AppointmentService
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.DeliverySessionService
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.ReferralService
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.validator.AppointmentValidator
import java.util.UUID
import jakarta.persistence.EntityNotFoundException

@RestController
@PreAuthorize("hasRole('ROLE_PROBATION') or hasRole('ROLE_CRS_PROVIDER') or hasRole('ROLE_INTERVENTIONS_API_READ_ALL')")
class DeliverySessionController(
  val actionPlanService: ActionPlanService,
  val deliverySessionService: DeliverySessionService,
  val locationMapper: LocationMapper,
  val userMapper: UserMapper,
  val appointmentValidator: AppointmentValidator,
  val referralAccessChecker: ReferralAccessChecker,
  val referralService: ReferralService,
  val appointmentService: AppointmentService,
) {

  @Deprecated("superseded by scheduleNewDeliverySessionAppointment and rescheduleDeliverySessionAppointment")
  @PatchMapping("/action-plan/{id}/appointment/{sessionNumber}")
  fun updateSessionAppointment(
    @PathVariable(name = "id") actionPlanId: UUID,
    @PathVariable sessionNumber: Int,
    @RequestBody updateAppointmentDTO: UpdateAppointmentDTO,
    authentication: JwtAuthenticationToken,
  ): DeliverySessionDTO {
    val user = userMapper.fromToken(authentication)
    val session = deliverySessionService.getDeliverySessionByActionPlanIdOrThrowException(actionPlanId, sessionNumber)
    appointmentValidator.validateUpdateAppointment(updateAppointmentDTO, session.referral.sentAt)
    val deliverySession = deliverySessionService.updateSessionAppointment(
      actionPlanId,
      sessionNumber,
      updateAppointmentDTO.appointmentTime,
      updateAppointmentDTO.durationInMinutes,
      user,
      updateAppointmentDTO.appointmentDeliveryType,
      updateAppointmentDTO.sessionType,
      updateAppointmentDTO.appointmentDeliveryAddress,
      updateAppointmentDTO.npsOfficeCode,
      updateAppointmentDTO.attendanceFeedback?.attended,
      updateAppointmentDTO.attendanceFeedback?.didSessionHappen,
      updateAppointmentDTO.sessionFeedback?.notifyProbationPractitioner,
      updateAppointmentDTO.sessionFeedback?.late,
      updateAppointmentDTO.sessionFeedback?.lateReason,
      updateAppointmentDTO.sessionFeedback?.futureSessionPlans,
      updateAppointmentDTO.sessionFeedback?.noAttendanceInformation,
      updateAppointmentDTO.sessionFeedback?.noSessionReasonType,
      updateAppointmentDTO.sessionFeedback?.noSessionReasonPopAcceptable,
      updateAppointmentDTO.sessionFeedback?.noSessionReasonPopUnacceptable,
      updateAppointmentDTO.sessionFeedback?.noSessionReasonLogistics,
      updateAppointmentDTO.sessionFeedback?.sessionSummary,
      updateAppointmentDTO.sessionFeedback?.sessionResponse,
      updateAppointmentDTO.sessionFeedback?.sessionConcerns,
    )
    return DeliverySessionDTO.from(deliverySession)
  }

  @Deprecated("superseded by getDeliverySessionAppointments")
  @GetMapping("/action-plan/{id}/appointments")
  fun getSessionsForActionPlan(
    @PathVariable(name = "id") actionPlanId: UUID,
  ): List<DeliverySessionDTO> {
    val actionPlan = actionPlanService.getActionPlan(actionPlanId)
    val deliverySessions = deliverySessionService.getSessions(actionPlan.referral.id)
    return DeliverySessionDTO.from(deliverySessions)
  }

  @Deprecated("superseded by getDeliverySessionAppointments")
  @GetMapping("/action-plan/{id}/appointments/{sessionNumber}")
  fun getSessionForActionPlanId(
    @PathVariable(name = "id") actionPlanId: UUID,
    @PathVariable sessionNumber: Int,
  ): DeliverySessionDTO {
    val actionPlan = actionPlanService.getActionPlan(actionPlanId)
    val deliverySession = deliverySessionService.getSession(actionPlan.referral.id, sessionNumber)
    return DeliverySessionDTO.from(deliverySession)
  }

  @Deprecated("superseded by getDeliverySessionAppointments")
  @GetMapping("/referral/{id}/sessions/{sessionNumber}")
  fun getSessionForReferralId(
    @PathVariable(name = "id") referralId: UUID,
    @PathVariable sessionNumber: Int,
  ): DeliverySessionDTO {
    val deliverySession = deliverySessionService.getSession(referralId, sessionNumber)
    return DeliverySessionDTO.from(deliverySession)
  }

  @GetMapping("/referral/{referralId}/delivery-session-appointments/{appointmentId}")
  fun getDeliverySessionAppointment(
    @PathVariable(name = "referralId") referralId: UUID,
    @PathVariable(name = "appointmentId") appointmentId: UUID,
    authentication: JwtAuthenticationToken,
  ): DeliverySessionAppointmentDTO {
    val user = userMapper.fromToken(authentication)
    val referral = referralService.getSentReferral(referralId) ?: throw ResponseStatusException(
      HttpStatus.BAD_REQUEST,
      "sent referral not found [referralId=$referralId]",
    )
    referralAccessChecker.forUser(referral, user)
    val matchingAppointment = deliverySessionService.getSessions(referralId)
      .flatMap { session -> session.appointments.map { appointment -> Pair(session.sessionNumber, appointment) } }
      .firstOrNull { appointment -> appointment.second.id == appointmentId }
      ?: throw EntityNotFoundException("Delivery session appointment not found [referralId=$referralId, appointmentId=$appointmentId]")
    return DeliverySessionAppointmentDTO.from(matchingAppointment.first, matchingAppointment.second)
  }

  @GetMapping("/referral/{referralId}/delivery-session-appointments")
  fun getDeliverySessionAppointments(
    @PathVariable(name = "referralId") referralId: UUID,
    authentication: JwtAuthenticationToken,
  ): List<DeliverySessionAppointmentDTO> {
    val user = userMapper.fromToken(authentication)
    val referral = referralService.getSentReferral(referralId) ?: throw ResponseStatusException(
      HttpStatus.BAD_REQUEST,
      "sent referral not found [referralId=$referralId]",
    )
    referralAccessChecker.forUser(referral, user)
    return deliverySessionService.getSessions(referralId)
      .flatMap { session -> session.appointments.map { appointment -> DeliverySessionAppointmentDTO.from(session.sessionNumber, appointment) } }
  }

  @PostMapping("/referral/{referralId}/delivery-session-appointments")
  fun scheduleNewDeliverySessionAppointment(
    @PathVariable(name = "referralId") referralId: UUID,
    @RequestBody request: DeliverySessionAppointmentScheduleDetailsRequestDTO,
    authentication: JwtAuthenticationToken,
  ): DeliverySessionAppointmentDTO {
    val user = userMapper.fromToken(authentication)
    val referral = referralService.getSentReferral(referralId) ?: throw ResponseStatusException(
      HttpStatus.BAD_REQUEST,
      "sent referral not found [referralId=$referralId]",
    )
    referralAccessChecker.forUser(referral, user)
    val deliverySession = deliverySessionService.scheduleNewDeliverySessionAppointment(
      referralId,
      request.sessionId,
      request.appointmentTime,
      request.durationInMinutes,
      user,
      request.appointmentDeliveryType,
      request.sessionType,
      request.appointmentDeliveryAddress,
      request.npsOfficeCode,
      request.attendanceFeedback?.attended,
      request.attendanceFeedback?.didSessionHappen,
      request.sessionFeedback?.notifyProbationPractitioner,
      request.sessionFeedback?.late,
      request.sessionFeedback?.lateReason,
      request.sessionFeedback?.futureSessionPlans,
      request.sessionFeedback?.noAttendanceInformation,
      request.sessionFeedback?.noSessionReasonType,
      request.sessionFeedback?.noSessionReasonPopAcceptable,
      request.sessionFeedback?.noSessionReasonPopUnacceptable,
      request.sessionFeedback?.noSessionReasonLogistics,
      request.sessionFeedback?.sessionSummary,
      request.sessionFeedback?.sessionResponse,
      request.sessionFeedback?.sessionConcerns,
    )
    return DeliverySessionAppointmentDTO.from(deliverySession.sessionNumber, deliverySession.currentAppointment!!)
  }

  @PutMapping("/referral/{referralId}/delivery-session-appointments/{appointmentId}")
  fun rescheduleDeliverySessionAppointment(
    @PathVariable(name = "referralId") referralId: UUID,
    @PathVariable(name = "appointmentId") appointmentId: UUID,
    @RequestBody request: DeliverySessionAppointmentScheduleDetailsRequestDTO,
    authentication: JwtAuthenticationToken,
  ): DeliverySessionAppointmentDTO {
    val user = userMapper.fromToken(authentication)
    val referral = referralService.getSentReferral(referralId) ?: throw ResponseStatusException(
      HttpStatus.BAD_REQUEST,
      "sent referral not found [referralId=$referralId]",
    )
    referralAccessChecker.forUser(referral, user)
    val deliverySession = deliverySessionService.rescheduleDeliverySessionAppointment(
      referralId,
      request.sessionId,
      appointmentId,
      request.appointmentTime,
      request.durationInMinutes,
      user,
      request.appointmentDeliveryType,
      request.sessionType,
      request.appointmentDeliveryAddress,
      request.npsOfficeCode,
      request.attendanceFeedback?.attended,
      request.attendanceFeedback?.didSessionHappen,
      request.sessionFeedback?.notifyProbationPractitioner,
      request.sessionFeedback?.late,
      request.sessionFeedback?.lateReason,
      request.sessionFeedback?.futureSessionPlans,
      request.sessionFeedback?.noAttendanceInformation,
      request.sessionFeedback?.noSessionReasonType,
      request.sessionFeedback?.noSessionReasonPopAcceptable,
      request.sessionFeedback?.noSessionReasonPopUnacceptable,
      request.sessionFeedback?.noSessionReasonLogistics,
      request.sessionFeedback?.sessionSummary,
      request.sessionFeedback?.sessionResponse,
      request.sessionFeedback?.sessionConcerns,
    )
    return DeliverySessionAppointmentDTO.from(deliverySession.sessionNumber, deliverySession.currentAppointment!!)
  }

  @PutMapping("/referral/{referralId}/delivery-session-appointments/{appointmentId}/attendance")
  fun recordAttendanceFeedback(
    @PathVariable(name = "referralId") referralId: UUID,
    @PathVariable(name = "appointmentId") appointmentId: UUID,
    @RequestBody request: AttendanceFeedbackRequestDTO,
    authentication: JwtAuthenticationToken,
  ): DeliverySessionAppointmentDTO {
    val user = userMapper.fromToken(authentication)
    val referral = referralService.getSentReferral(referralId) ?: throw ResponseStatusException(
      HttpStatus.BAD_REQUEST,
      "sent referral not found [referralId=$referralId]",
    )
    referralAccessChecker.forUser(referral, user)
    val updatedSessionAppointment = deliverySessionService.recordAttendanceFeedback(
      referralId,
      appointmentId,
      user,
      request.attended,
      request.didSessionHappen,
    )
    return DeliverySessionAppointmentDTO.from(updatedSessionAppointment.first.sessionNumber, updatedSessionAppointment.second)
  }

  @PutMapping("/referral/{referralId}/delivery-session-appointments/{appointmentId}/session-feedback")
  fun recordSessionFeedback(
    @PathVariable(name = "referralId") referralId: UUID,
    @PathVariable(name = "appointmentId") appointmentId: UUID,
    @RequestBody request: SessionFeedbackRequestDTO,
    authentication: JwtAuthenticationToken,
  ): DeliverySessionAppointmentDTO {
    val user = userMapper.fromToken(authentication)
    val referral = referralService.getSentReferral(referralId) ?: throw ResponseStatusException(
      HttpStatus.BAD_REQUEST,
      "sent referral not found [referralId=$referralId]",
    )
    referralAccessChecker.forUser(referral, user)
    val updatedSessionAppointment = deliverySessionService.recordSessionFeedback(
      referralId,
      appointmentId,
      user,
      request.late,
      request.lateReason,
      request.futureSessionPlans,
      request.noAttendanceInformation,
      request.noSessionReasonType,
      request.noSessionReasonPopAcceptable,
      request.noSessionReasonPopUnacceptable,
      request.noSessionReasonLogistics,
      request.sessionSummary,
      request.sessionResponse,
      request.sessionConcerns,
      request.notifyProbationPractitioner,
    )
    return DeliverySessionAppointmentDTO.from(updatedSessionAppointment.first.sessionNumber, updatedSessionAppointment.second)
  }

  @PostMapping("/referral/{referralId}/delivery-session-appointments/{appointmentId}/submit-feedback")
  fun submitDeliverySessionAppointmentFeedback(
    @PathVariable(name = "referralId") referralId: UUID,
    @PathVariable(name = "appointmentId") appointmentId: UUID,
    authentication: JwtAuthenticationToken,
  ): DeliverySessionAppointmentDTO {
    val user = userMapper.fromToken(authentication)
    val referral = referralService.getSentReferral(referralId) ?: throw ResponseStatusException(
      HttpStatus.BAD_REQUEST,
      "sent referral not found [referralId=$referralId]",
    )
    referralAccessChecker.forUser(referral, user)
    val sessionAndAppointment = deliverySessionService.submitAppointmentFeedback(referralId, appointmentId, user)
    return DeliverySessionAppointmentDTO.from(sessionAndAppointment.first.sessionNumber, sessionAndAppointment.second)
  }
}
