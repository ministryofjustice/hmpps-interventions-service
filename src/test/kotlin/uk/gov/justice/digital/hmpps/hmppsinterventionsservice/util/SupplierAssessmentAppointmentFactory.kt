package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util

import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ActionPlan
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Appointment
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.SupplierAssessmentAppointment
import java.util.UUID

class SupplierAssessmentAppointmentFactory(em: TestEntityManager? = null) : EntityFactory(em) {
  private val actionPlanFactory = ActionPlanFactory(em)
  private val appointmentFactory = AppointmentFactory(em)

  fun create(
    id: UUID = UUID.randomUUID(),
    actionPlan: ActionPlan = actionPlanFactory.create(),
    sessionNumber: Int = 1,
    appointment: Appointment = appointmentFactory.create()
  ): SupplierAssessmentAppointment {
    return save(
      SupplierAssessmentAppointment(
        id = id,
        actionPlan = actionPlan,
        sessionNumber = sessionNumber,
        appointment = appointment,
      )
    )
  }

  fun createAttended(
    id: UUID = UUID.randomUUID(),
    actionPlan: ActionPlan = actionPlanFactory.create(),
    sessionNumber: Int = 1,
    appointment: Appointment = appointmentFactory.createAttended()
  ): SupplierAssessmentAppointment {
    return save(
      SupplierAssessmentAppointment(
        id = id,
        actionPlan = actionPlan,
        sessionNumber = sessionNumber,
        appointment = appointment,
      )
    )
  }
}
