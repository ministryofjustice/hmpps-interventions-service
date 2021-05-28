package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.component.LocationMapper
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.SupplierAssessmentAppointmentDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.UpdateAppointmentAttendanceDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.UpdateAppointmentBehaviourDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.UpdateAppointmentDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.AppointmentsService
import java.util.UUID

@RestController
class AppointmentsController(
  val appointmentsService: AppointmentsService,
  val locationMapper: LocationMapper
) {
  @PatchMapping("/action-plan/{id}/appointment/{sessionNumber}")
  fun updateAppointment(
    @PathVariable(name = "id") actionPlanId: UUID,
    @PathVariable sessionNumber: Int,
    @RequestBody updateAppointmentDTO: UpdateAppointmentDTO,
  ): SupplierAssessmentAppointmentDTO {

    val actionPlanAppointment = appointmentsService.updateAppointment(
      actionPlanId,
      sessionNumber,
      updateAppointmentDTO.appointmentTime,
      updateAppointmentDTO.durationInMinutes
    )
    return SupplierAssessmentAppointmentDTO.from(actionPlanAppointment)
  }

  @GetMapping("/action-plan/{id}/appointments")
  fun getAppointments(
    @PathVariable(name = "id") actionPlanId: UUID
  ): List<SupplierAssessmentAppointmentDTO> {

    val actionPlanAppointments = appointmentsService.getAppointments(actionPlanId)
    return SupplierAssessmentAppointmentDTO.from(actionPlanAppointments)
  }

  @GetMapping("/action-plan/{id}/appointments/{sessionNumber}")
  fun getAppointment(
    @PathVariable(name = "id") actionPlanId: UUID,
    @PathVariable sessionNumber: Int,
  ): SupplierAssessmentAppointmentDTO {

    val actionPlanAppointment = appointmentsService.getAppointment(actionPlanId, sessionNumber)
    return SupplierAssessmentAppointmentDTO.from(actionPlanAppointment)
  }

  @PostMapping("/action-plan/{id}/appointment/{sessionNumber}/record-attendance")
  fun recordAttendance(
    @PathVariable(name = "id") actionPlanId: UUID,
    @PathVariable sessionNumber: Int,
    @RequestBody update: UpdateAppointmentAttendanceDTO,
  ): SupplierAssessmentAppointmentDTO {
    val updatedAppointment = appointmentsService.recordAttendance(
      actionPlanId, sessionNumber, update.attended, update.additionalAttendanceInformation
    )

    return SupplierAssessmentAppointmentDTO.from(updatedAppointment)
  }

  @PostMapping("/action-plan/{actionPlanId}/appointment/{sessionNumber}/record-behaviour")
  fun recordBehaviour(@PathVariable actionPlanId: UUID, @PathVariable sessionNumber: Int, @RequestBody update: UpdateAppointmentBehaviourDTO): SupplierAssessmentAppointmentDTO {
    return SupplierAssessmentAppointmentDTO.from(appointmentsService.recordBehaviour(actionPlanId, sessionNumber, update.behaviourDescription, update.notifyProbationPractitioner))
  }

  @PostMapping("/action-plan/{actionPlanId}/appointment/{sessionNumber}/submit")
  fun submitSessionFeedback(@PathVariable actionPlanId: UUID, @PathVariable sessionNumber: Int): SupplierAssessmentAppointmentDTO {
    return SupplierAssessmentAppointmentDTO.from(appointmentsService.submitSessionFeedback(actionPlanId, sessionNumber))
  }
}
