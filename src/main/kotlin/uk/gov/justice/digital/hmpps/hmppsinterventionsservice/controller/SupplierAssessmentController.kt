package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.controller

import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.authorization.UserMapper
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.AppointmentDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.AttendanceFeedbackRequestDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.SessionFeedbackRequestDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.UpdateAppointmentDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Appointment
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AppointmentType
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AuthUser
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.AppointmentService
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.ReferralService
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.SupplierAssessmentService
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.validator.AppointmentValidator
import java.util.UUID

@RestController
@PreAuthorize("hasRole('ROLE_PROBATION') or hasRole('ROLE_CRS_PROVIDER') or hasRole('ROLE_INTERVENTIONS_API_READ_ALL')")
class SupplierAssessmentController(
  private val referralService: ReferralService,
  private val supplierAssessmentService: SupplierAssessmentService,
  private val userMapper: UserMapper,
  private val appointmentValidator: AppointmentValidator,
  private val appointmentService: AppointmentService,
) {
  @Deprecated("superseded by POST /referral/{referralId}/supplier-assessment and PUT /referral/{referralId}/supplier-assessment/{appointmentId}")
  @PutMapping("/supplier-assessment/{id}/schedule-appointment")
  fun updateSupplierAssessmentAppointment(
    @PathVariable id: UUID,
    @RequestBody updateAppointmentDTO: UpdateAppointmentDTO,
    authentication: JwtAuthenticationToken,
  ): AppointmentDTO {
    val user = userMapper.fromToken(authentication)
    val supplierAssessment = supplierAssessmentService.getSupplierAssessmentById(id)
    appointmentValidator.validateUpdateAppointment(updateAppointmentDTO, supplierAssessment.referral.sentAt)
    return AppointmentDTO.from(
      supplierAssessmentService.createOrUpdateSupplierAssessmentAppointment(
        supplierAssessment,
        updateAppointmentDTO.durationInMinutes,
        updateAppointmentDTO.appointmentTime,
        user,
        updateAppointmentDTO.appointmentDeliveryType,
        updateAppointmentDTO.sessionType,
        updateAppointmentDTO.appointmentDeliveryAddress,
        updateAppointmentDTO.npsOfficeCode,
        updateAppointmentDTO.attendanceFeedback?.attended,
        updateAppointmentDTO.attendanceFeedback?.didSessionHappen,
        updateAppointmentDTO.sessionFeedback?.notifyProbationPractitionerOfBehaviour,
        updateAppointmentDTO.sessionFeedback?.notifyProbationPractitionerOfConcerns ?: updateAppointmentDTO.sessionFeedback?.notifyProbationPractitioner,
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
        updateAppointmentDTO.sessionFeedback?.sessionBehaviour,
        updateAppointmentDTO.sessionFeedback?.sessionConcerns,
      ),
    )
  }

  @PostMapping("/referral/{referralId}/supplier-assessment")
  fun scheduleSupplierAssessmentAppointment(
    @PathVariable referralId: UUID,
    @RequestBody updateAppointmentDTO: UpdateAppointmentDTO,
    authentication: JwtAuthenticationToken,
  ): AppointmentDTO {
    val user = userMapper.fromToken(authentication)
    appointmentValidator.validateUpdateAppointment(updateAppointmentDTO, supplierAssessmentService.getReferral(referralId).sentAt)
    val appointment = supplierAssessmentService.scheduleNewSupplierAssessmentAppointment(
      referralId,
      updateAppointmentDTO.durationInMinutes,
      updateAppointmentDTO.appointmentTime,
      user,
      updateAppointmentDTO.appointmentDeliveryType,
      updateAppointmentDTO.appointmentDeliveryAddress,
      updateAppointmentDTO.npsOfficeCode,
    )
    return AppointmentDTO.from(appointment)
  }

  @PutMapping("/referral/{referralId}/supplier-assessment/{appointmentId}")
  fun rescheduleSupplierAssessmentAppointment(
    @PathVariable referralId: UUID,
    @PathVariable appointmentId: UUID,
    @RequestBody updateAppointmentDTO: UpdateAppointmentDTO,
    authentication: JwtAuthenticationToken,
  ): AppointmentDTO {
    val user = userMapper.fromToken(authentication)
    appointmentValidator.validateUpdateAppointment(updateAppointmentDTO, supplierAssessmentService.getReferral(referralId).sentAt)
    val appointment = supplierAssessmentService.rescheduleSupplierAssessmentAppointment(
      referralId,
      appointmentId,
      updateAppointmentDTO.durationInMinutes,
      updateAppointmentDTO.appointmentTime,
      user,
      updateAppointmentDTO.appointmentDeliveryType,
      updateAppointmentDTO.sessionType,
      updateAppointmentDTO.appointmentDeliveryAddress,
      updateAppointmentDTO.npsOfficeCode,
    )
    return AppointmentDTO.from(appointment)
  }

  @PutMapping("/referral/{referralId}/supplier-assessment/record-session-feedback")
  fun recordSessionFeedback(
    @PathVariable referralId: UUID,
    @RequestBody request: SessionFeedbackRequestDTO,
    authentication: JwtAuthenticationToken,
  ): AppointmentDTO {
    val user = userMapper.fromToken(authentication)
    val supplierAssessmentAppointment = getSupplierAssessmentAppointment(referralId, user)

    return AppointmentDTO.from(
      appointmentService.recordSessionFeedback(
        supplierAssessmentAppointment,
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
        request.sessionBehaviour,
        request.sessionConcerns,
        request.notifyProbationPractitionerOfBehaviour,
        request.notifyProbationPractitionerOfConcerns ?: request.notifyProbationPractitioner,
        user,
      ),
    )
  }

  @PutMapping("/referral/{referralId}/supplier-assessment/record-attendance")
  fun recordAttendance(
    @PathVariable referralId: UUID,
    @RequestBody request: AttendanceFeedbackRequestDTO,
    authentication: JwtAuthenticationToken,
  ): AppointmentDTO {
    val submittedBy = userMapper.fromToken(authentication)
    val supplierAssessmentAppointment = getSupplierAssessmentAppointment(referralId, submittedBy)
    val updatedAppointment = appointmentService.recordAppointmentAttendance(
      supplierAssessmentAppointment,
      request.attended,
      request.didSessionHappen,
      submittedBy,
    )
    return AppointmentDTO.from(updatedAppointment)
  }

  @PostMapping("/referral/{referralId}/supplier-assessment/submit-feedback")
  fun submitFeedback(
    @PathVariable referralId: UUID,
    authentication: JwtAuthenticationToken,
  ): AppointmentDTO {
    val user = userMapper.fromToken(authentication)
    val supplierAssessmentAppointment = getSupplierAssessmentAppointment(referralId, user)
    return AppointmentDTO.from(appointmentService.submitAppointmentFeedback(supplierAssessmentAppointment, user, AppointmentType.SUPPLIER_ASSESSMENT))
  }

  private fun getSupplierAssessmentAppointment(referralId: UUID, user: AuthUser): Appointment {
    val referral = referralService.getSentReferralForUser(referralId, user)
      ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "referral not found [id=$referralId]")
    if (referral.supplierAssessment == null) {
      throw ResponseStatusException(HttpStatus.NOT_FOUND, "supplier assessment not found for referral [id=$referralId]")
    }
    if (referral.supplierAssessment!!.currentAppointment == null) {
      throw ResponseStatusException(HttpStatus.NOT_FOUND, "no current appointment exists on supplier assessment for referral [id=$referralId]")
    }
    return referral.supplierAssessment!!.currentAppointment!!
  }
}
