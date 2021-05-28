package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.SupplierAssessmentAppointment
import java.util.UUID

interface SupplierAssessmentAppointmentRepository : JpaRepository<SupplierAssessmentAppointment, UUID> {
  fun findAllByActionPlanId(actionPlanId: UUID): List<SupplierAssessmentAppointment>
  fun findByActionPlanIdAndSessionNumber(actionPlanId: UUID, sessionNumber: Int): SupplierAssessmentAppointment?
//  fun countByActionPlanIdAndAppointmentIDAttendedIsNotNull(actionPlanId: UUID): Int
  fun countByActionPlanIdAndAppointmentAttendedIsNotNull(actionPlanId: UUID): Int
}
