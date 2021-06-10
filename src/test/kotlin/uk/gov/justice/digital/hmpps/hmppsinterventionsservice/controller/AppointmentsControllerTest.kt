package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.controller

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.apache.http.HttpStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.web.server.ResponseStatusException
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.authorization.UserMapper
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.component.LocationMapper
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.ActionPlanSessionDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.UpdateAppointmentAttendanceDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.UpdateAppointmentDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Attended
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.SampleData
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.AppointmentsService
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.ReferralService
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.AuthUserFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.JwtTokenFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ReferralFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.SupplierAssessmentFactory
import java.time.OffsetDateTime
import java.util.UUID

internal class AppointmentsControllerTest {
  private val appointmentsService = mock<AppointmentsService>()
  private val locationMapper = mock<LocationMapper>()
  private val referralService = mock<ReferralService>()
  private val userMapper = mock<UserMapper>()
  private val referralFactory = ReferralFactory()
  private val tokenFactory = JwtTokenFactory()
  private val supplierAssessmentFactory = SupplierAssessmentFactory()
  private val authUserFactory = AuthUserFactory()

  private val appointmentsController = AppointmentsController(appointmentsService, referralService, userMapper, locationMapper)

  @Test
  fun `updates an appointment`() {
    val actionPlan = SampleData.sampleActionPlan()
    val actionPlanAppointment = SampleData.sampleActionPlanSession(actionPlan = actionPlan)
    val sessionNumber = 1

    val updateAppointmentDTO = UpdateAppointmentDTO(OffsetDateTime.now(), 10)

    whenever(
      appointmentsService.updateActionPlanSessionAppointment(
        actionPlan.id,
        sessionNumber,
        updateAppointmentDTO.appointmentTime,
        updateAppointmentDTO.durationInMinutes
      )
    ).thenReturn(actionPlanAppointment)

    val appointmentResponse = appointmentsController.updateAppointment(actionPlan.id, sessionNumber, updateAppointmentDTO)

    assertThat(appointmentResponse).isEqualTo(ActionPlanSessionDTO.from(actionPlanAppointment))
  }

  @Test
  fun `gets an appointment`() {
    val actionPlan = SampleData.sampleActionPlan()
    val actionPlanAppointment = SampleData.sampleActionPlanSession(actionPlan = actionPlan)
    val sessionNumber = 1

    whenever(appointmentsService.getActionPlanSessionAppointment(actionPlan.id, sessionNumber)).thenReturn(actionPlanAppointment)

    val appointmentResponse = appointmentsController.getAppointment(actionPlan.id, sessionNumber)

    assertThat(appointmentResponse).isEqualTo(ActionPlanSessionDTO.from(actionPlanAppointment))
  }

  @Test
  fun `gets a list of appointment`() {
    val actionPlan = SampleData.sampleActionPlan()
    val actionPlanAppointment = SampleData.sampleActionPlanSession(actionPlan = actionPlan)

    whenever(appointmentsService.getActionPlanSessionAppointments(actionPlan.id)).thenReturn(listOf(actionPlanAppointment))

    val appointmentsResponse = appointmentsController.getAppointments(actionPlan.id)

    assertThat(appointmentsResponse.size).isEqualTo(1)
    assertThat(appointmentsResponse.first()).isEqualTo(ActionPlanSessionDTO.from(actionPlanAppointment))
  }

  @Test
  fun `updates appointment with attendance details`() {
    val actionPlanId = UUID.randomUUID()
    val sessionNumber = 1
    val update = UpdateAppointmentAttendanceDTO(Attended.YES, "more info")
    val actionPlan = SampleData.sampleActionPlan()

    val updatedAppointment = SampleData.sampleActionPlanSession(
      actionPlan = actionPlan,
      appointment = SampleData.sampleAppointment(attended = Attended.YES, additionalAttendanceInformation = "more info")
    )

    whenever(
      appointmentsService.recordActionPlanSessionAttendance(
        actionPlanId, sessionNumber, update.attended,
        update.additionalAttendanceInformation
      )
    ).thenReturn(updatedAppointment)

    val appointmentResponse = appointmentsController.recordAttendance(actionPlanId, sessionNumber, update)

    assertThat(appointmentResponse).isNotNull
  }

  @Test
  fun `get supplier assessment appointment`() {
    val referral = referralFactory.createSent()
    referral.supplierAssessment = supplierAssessmentFactory.create()
    val user = authUserFactory.create()
    val token = tokenFactory.create()

    whenever(userMapper.fromToken(token)).thenReturn(user)
    whenever(referralService.getSentReferralForUser(referral.id, user)).thenReturn(referral)

    val response = appointmentsController.getSupplierAssessmentAppointment(referral.id, token)

    assertThat(response).isNotNull
  }

  @Test
  fun `no sent referral found for get supplier assessment appointment `() {
    val referralId = UUID.randomUUID()
    val user = authUserFactory.create()
    val token = tokenFactory.create()

    whenever(userMapper.fromToken(token)).thenReturn(user)
    whenever(referralService.getSentReferralForUser(referralId, user)).thenReturn(null)

    val e = assertThrows<ResponseStatusException> {
      appointmentsController.getSupplierAssessmentAppointment(referralId, token)
    }
    assertThat(e.status).isEqualTo(org.springframework.http.HttpStatus.NOT_FOUND)
    assertThat(e.message).contains("sent referral not found [id=$referralId]")
  }
}
