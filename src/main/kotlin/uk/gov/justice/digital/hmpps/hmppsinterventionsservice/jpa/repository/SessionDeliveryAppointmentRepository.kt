package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.SessionDeliveryAppointment
import java.util.UUID

interface SessionDeliveryAppointmentRepository : JpaRepository<SessionDeliveryAppointment, UUID> {
  fun findAllByActionPlanId(actionPlanId: UUID): List<SessionDeliveryAppointment>
  fun findByActionPlanIdAndSessionNumber(actionPlanId: UUID, sessionNumber: Int): SessionDeliveryAppointment?
//  fun countByActionPlanIdAndAppointmentIDAttendedIsNotNull(actionPlanId: UUID): Int
  fun countByActionPlanIdAndAppointmentAttendedIsNotNull(actionPlanId: UUID): Int
}
