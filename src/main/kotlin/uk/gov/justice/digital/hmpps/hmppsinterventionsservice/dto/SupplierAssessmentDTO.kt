package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto

import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.SupplierAssessment
import java.time.OffsetDateTime
import java.util.UUID

class SupplierAssessmentDTO(
  val id: UUID,
  override val appointmentTime: OffsetDateTime?,
  override val durationInMinutes: Int?,
  val createdAt: OffsetDateTime,
  val createdBy: AuthUserDTO,
) : BaseAppointmentDTO(appointmentTime, durationInMinutes) {
  companion object {
    fun from(supplierAssessment: SupplierAssessment): SupplierAssessmentDTO {
      return SupplierAssessmentDTO(
        id = supplierAssessment.id,
        appointmentTime = supplierAssessment.appointment.appointmentTime,
        durationInMinutes = supplierAssessment.appointment.durationInMinutes,
        createdAt = supplierAssessment.appointment.createdAt,
        createdBy = AuthUserDTO.from(supplierAssessment.appointment.createdBy),
      )
    }
    fun from(supplierAssessment: List<SupplierAssessment>): List<SupplierAssessmentDTO> {
      return supplierAssessment.map { from(it) }
    }
  }
}
