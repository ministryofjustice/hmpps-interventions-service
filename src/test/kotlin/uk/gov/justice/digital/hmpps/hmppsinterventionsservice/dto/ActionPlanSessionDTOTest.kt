package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.SampleData

internal class ActionPlanSessionDTOTest {

  @Test
  fun `Maps from an appointment`() {
    val actionPlan = SampleData.sampleActionPlan()
    val actionPlanAppointment = SampleData.sampleActionPlanSession(actionPlan = actionPlan)

    val appointmentDTO = ActionPlanSessionDTO.from(actionPlanAppointment)

    assertThat(appointmentDTO.id).isEqualTo(actionPlanAppointment.id)
    assertThat(appointmentDTO.sessionNumber).isEqualTo(actionPlanAppointment.sessionNumber)
    assertThat(appointmentDTO.appointmentTime).isEqualTo(actionPlanAppointment.appointment.appointmentTime)
    assertThat(appointmentDTO.durationInMinutes).isEqualTo(actionPlanAppointment.appointment.durationInMinutes)
    assertThat(appointmentDTO.createdAt).isEqualTo(actionPlanAppointment.appointment.createdAt)
    assertThat(appointmentDTO.createdBy.userId).isEqualTo(actionPlanAppointment.appointment.createdBy.id)
  }

  @Test
  fun `Maps from a list of appointments`() {
    val actionPlan = SampleData.sampleActionPlan()
    val actionPlanAppointment = SampleData.sampleActionPlanSession(actionPlan = actionPlan)

    val appointmentsDTO = ActionPlanSessionDTO.from(listOf(actionPlanAppointment))

    assertThat(appointmentsDTO.size).isEqualTo(1)
    assertThat(appointmentsDTO.first().id).isEqualTo(actionPlanAppointment.id)
    assertThat(appointmentsDTO.first().sessionNumber).isEqualTo(actionPlanAppointment.sessionNumber)
    assertThat(appointmentsDTO.first().appointmentTime).isEqualTo(actionPlanAppointment.appointment.appointmentTime)
    assertThat(appointmentsDTO.first().durationInMinutes).isEqualTo(actionPlanAppointment.appointment.durationInMinutes)
    assertThat(appointmentsDTO.first().createdAt).isEqualTo(actionPlanAppointment.appointment.createdAt)
    assertThat(appointmentsDTO.first().createdBy.userId).isEqualTo(actionPlanAppointment.appointment.createdBy.id)
  }
}
