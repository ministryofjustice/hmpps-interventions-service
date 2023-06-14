package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.controller

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.web.server.ResponseStatusException
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.authorization.ReferralAccessChecker
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.authorization.UserMapper
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.component.LocationMapper
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.AttendanceFeedbackRequestDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.DeliverySessionAppointmentDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.DeliverySessionDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.SessionFeedbackRequestDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.UpdateAppointmentDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Appointment
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AppointmentDeliveryType
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AppointmentSessionType
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Attended
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Attended.NO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Attended.YES
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.AuthUserRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.ActionPlanService
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.AppointmentService
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.DeliverySessionService
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.ReferralService
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ActionPlanFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.AuthUserFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.DeliverySessionFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.JwtTokenFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.validator.AppointmentValidator
import java.time.OffsetDateTime
import java.util.UUID
import javax.persistence.EntityNotFoundException

internal class DeliverySessionControllerTest {
  private val sessionsService = mock<DeliverySessionService>()
  private val locationMapper = mock<LocationMapper>()
  private val appointmentValidator = mock<AppointmentValidator>()
  private val authUserRepository = mock<AuthUserRepository>()
  private val actionPlanService = mock<ActionPlanService>()
  private val userMapper = UserMapper(authUserRepository)
  private val referralService = mock<ReferralService>()
  private val referralAccessChecker = mock<ReferralAccessChecker>()
  private val appointmentService = mock<AppointmentService>()

  private val sessionsController = DeliverySessionController(actionPlanService, sessionsService, locationMapper, userMapper, appointmentValidator, referralAccessChecker, referralService, appointmentService)
  private val actionPlanFactory = ActionPlanFactory()
  private val deliverySessionFactory = DeliverySessionFactory()
  private val jwtTokenFactory = JwtTokenFactory()
  private val authUserFactory = AuthUserFactory()

  @Nested
  inner class UpdateSession {

    @Test
    fun `updates a session`() {
      val user = authUserFactory.create()
      val userToken = jwtTokenFactory.create(user)
      val deliverySession = deliverySessionFactory.createScheduled(createdBy = user)
      val actionPlanId = UUID.randomUUID()
      val sessionNumber = deliverySession.sessionNumber

      val updateAppointmentDTO = UpdateAppointmentDTO(OffsetDateTime.now(), 10, AppointmentDeliveryType.PHONE_CALL, AppointmentSessionType.ONE_TO_ONE, null, null)

      whenever(authUserRepository.save(any())).thenReturn(authUserFactory.create())
      whenever(
        sessionsService.getDeliverySessionByActionPlanIdOrThrowException(
          actionPlanId,
          sessionNumber,
        ),
      ).thenReturn(deliverySession)

      whenever(
        sessionsService.updateSessionAppointment(
          actionPlanId,
          sessionNumber,
          updateAppointmentDTO.appointmentTime,
          updateAppointmentDTO.durationInMinutes,
          user,
          AppointmentDeliveryType.PHONE_CALL,
          AppointmentSessionType.ONE_TO_ONE,
          null,
          null,
          null,
          null,
          null,
          null,
        ),
      ).thenReturn(deliverySession)

      val sessionResponse = sessionsController.updateSessionAppointment(actionPlanId, sessionNumber, updateAppointmentDTO, userToken)

      assertThat(sessionResponse).isEqualTo(DeliverySessionDTO.from(deliverySession))
    }

    @Test
    fun `updates a session with new historic appointment`() {
      val user = authUserFactory.create()
      val userToken = jwtTokenFactory.create(user)
      val deliverySession = deliverySessionFactory.createScheduled(createdBy = user)
      val actionPlanId = UUID.randomUUID()
      val sessionNumber = deliverySession.sessionNumber

      val attendanceFeedbackRequestDTO = AttendanceFeedbackRequestDTO(YES, "attended")
      val sessionFeedbackRequestDTO = SessionFeedbackRequestDTO("summary", "response", null, false)
      val updateAppointmentDTO = UpdateAppointmentDTO(OffsetDateTime.now(), 10, AppointmentDeliveryType.PHONE_CALL, AppointmentSessionType.ONE_TO_ONE, null, null, attendanceFeedbackRequestDTO, sessionFeedbackRequestDTO)

      whenever(authUserRepository.save(any())).thenReturn(authUserFactory.create())
      whenever(
        sessionsService.getDeliverySessionByActionPlanIdOrThrowException(
          actionPlanId,
          sessionNumber,
        ),
      ).thenReturn(deliverySession)

      whenever(
        sessionsService.updateSessionAppointment(
          actionPlanId,
          sessionNumber,
          updateAppointmentDTO.appointmentTime,
          updateAppointmentDTO.durationInMinutes,
          user,
          AppointmentDeliveryType.PHONE_CALL,
          AppointmentSessionType.ONE_TO_ONE,
          null,
          null,
          YES,
          "attended",
          false,
          "summary",
          "response",
        ),
      ).thenReturn(deliverySession)

      val sessionResponse = sessionsController.updateSessionAppointment(actionPlanId, sessionNumber, updateAppointmentDTO, userToken)

      assertThat(sessionResponse).isEqualTo(DeliverySessionDTO.from(deliverySession))
    }

    @Test
    fun `updates a session with new appointment when a PoP did not attend`() {
      val user = authUserFactory.create()
      val userToken = jwtTokenFactory.create(user)
      val deliverySession = deliverySessionFactory.createScheduled(createdBy = user)
      val actionPlanId = UUID.randomUUID()
      val sessionNumber = deliverySession.sessionNumber

      val attendanceFeedbackRequestDTO = AttendanceFeedbackRequestDTO(NO, "attended")
      val sessionFeedbackRequestDTO = SessionFeedbackRequestDTO("summary", "response", null, false)
      val updateAppointmentDTO = UpdateAppointmentDTO(OffsetDateTime.now(), 10, AppointmentDeliveryType.PHONE_CALL, AppointmentSessionType.ONE_TO_ONE, null, null, attendanceFeedbackRequestDTO, sessionFeedbackRequestDTO)
      val newAppointment = Appointment(
        id = UUID.randomUUID(),
        createdBy = user,
        createdAt = OffsetDateTime.now(),
        appointmentTime = updateAppointmentDTO.appointmentTime,
        durationInMinutes = updateAppointmentDTO.durationInMinutes,
        deliusAppointmentId = 123L,
        referral = deliverySession.referral,
      )
      deliverySession.appointments.add(newAppointment)
      whenever(authUserRepository.save(any())).thenReturn(authUserFactory.create())
      whenever(sessionsService.getDeliverySessionByActionPlanIdOrThrowException(actionPlanId, sessionNumber)).thenReturn(deliverySession)
      whenever(
        sessionsService.updateSessionAppointment(
          actionPlanId,
          sessionNumber,
          updateAppointmentDTO.appointmentTime,
          updateAppointmentDTO.durationInMinutes,
          user,
          AppointmentDeliveryType.PHONE_CALL,
          AppointmentSessionType.ONE_TO_ONE,
          null,
          null,
          NO,
          "attended",
          false,
          "summary",
          "response",
        ),
      ).thenReturn(deliverySession)

      val sessionResponse = sessionsController.updateSessionAppointment(actionPlanId, sessionNumber, updateAppointmentDTO, userToken)

      assertThat(sessionResponse).isEqualTo(DeliverySessionDTO.from(deliverySession))
    }
  }

  @Nested
  inner class GetSession {
    @Test
    fun `gets a session`() {
      val deliverySession = deliverySessionFactory.createScheduled()
      val sessionNumber = deliverySession.sessionNumber
      val actionPlanId = UUID.randomUUID()
      val actionPlan = actionPlanFactory.create(referral = deliverySession.referral)

      whenever(actionPlanService.getActionPlan(actionPlanId)).thenReturn(actionPlan)
      whenever(sessionsService.getSession(deliverySession.referral.id, sessionNumber)).thenReturn(deliverySession)

      val sessionResponse = sessionsController.getSessionForActionPlanId(actionPlanId, sessionNumber)

      assertThat(sessionResponse).isEqualTo(DeliverySessionDTO.from(deliverySession))
    }
  }

  @Nested
  inner class GetDeliverySessionAppointmentForReferral {
    val user = authUserFactory.create()
    val userToken = jwtTokenFactory.create(user)

    @Test
    fun `can get a session by referralId and appointmentId`() {
      val deliverySession = deliverySessionFactory.createScheduled()
      val referralId = deliverySession.referral.id
      val appointmentId = deliverySession.appointments.first().id

      whenever(referralService.getSentReferral(referralId)).thenReturn(deliverySession.referral)
      whenever(sessionsService.getSessions(referralId)).thenReturn(listOf(deliverySession))
      whenever(authUserRepository.save(any())).thenReturn(user)

      val sessionResponse = sessionsController.getDeliverySessionAppointment(referralId, appointmentId, userToken)

      assertThat(sessionResponse).isEqualTo(DeliverySessionAppointmentDTO.from(deliverySession.sessionNumber, deliverySession.appointments.first()))
    }

    @Test
    fun `expect bad request when referral does not exist`() {
      val deliverySession = deliverySessionFactory.createScheduled()
      val referralId = UUID.randomUUID()
      val appointmentId = deliverySession.id

      whenever(referralService.getSentReferral(referralId)).thenReturn(null)
      whenever(authUserRepository.save(any())).thenReturn(user)

      val e = assertThrows<ResponseStatusException> { sessionsController.getDeliverySessionAppointment(referralId, appointmentId, userToken) }
      assertThat(e.reason).contains("sent referral not found")
    }

    @Test
    fun `expect bad request when appointment does not exist on referral`() {
      val deliverySession = deliverySessionFactory.createScheduled()
      val referralId = deliverySession.referral.id
      val otherDeliverySession = deliverySessionFactory.createScheduled()
      val appointmentId = otherDeliverySession.appointments.first().id

      whenever(referralService.getSentReferral(referralId)).thenReturn(deliverySession.referral)
      whenever(sessionsService.getSessions(referralId)).thenReturn(listOf(deliverySession))
      whenever(authUserRepository.save(any())).thenReturn(user)

      val e = assertThrows<EntityNotFoundException> { sessionsController.getDeliverySessionAppointment(referralId, appointmentId, userToken) }
      assertThat(e.message).contains("Delivery session appointment not found")
    }
  }

  @Nested
  inner class GetSessionsForReferral {
    val user = authUserFactory.create()
    val userToken = jwtTokenFactory.create(user)

    @Test
    fun `can get sessions by referralId`() {
      val deliverySession = deliverySessionFactory.createScheduled()
      val referralId = deliverySession.referral.id

      whenever(referralService.getSentReferral(referralId)).thenReturn(deliverySession.referral)
      whenever(sessionsService.getSessions(referralId)).thenReturn(listOf(deliverySession))
      whenever(authUserRepository.save(any())).thenReturn(user)

      val sessionsResponse = sessionsController.getDeliverySessionAppointments(referralId, userToken)

      assertThat(sessionsResponse.size).isEqualTo(1)
      assertThat(sessionsResponse.first()).isEqualTo(DeliverySessionAppointmentDTO.from(deliverySession.sessionNumber, deliverySession.appointments.first()))
    }

    @Test
    fun `expect bad request when referral does not exist`() {
      val referralId = UUID.randomUUID()

      whenever(referralService.getSentReferral(referralId)).thenReturn(null)
      whenever(authUserRepository.save(any())).thenReturn(user)

      val e = assertThrows<ResponseStatusException> { sessionsController.getDeliverySessionAppointments(referralId, userToken) }
      assertThat(e.reason).contains("sent referral not found")
    }
  }

  @Test
  fun `gets a list of sessions`() {
    val deliverySession = deliverySessionFactory.createScheduled()
    val actionPlanId = UUID.randomUUID()
    val actionPlan = actionPlanFactory.create(referral = deliverySession.referral)

    whenever(actionPlanService.getActionPlan(actionPlanId)).thenReturn(actionPlan)
    whenever(sessionsService.getSessions(deliverySession.referral.id)).thenReturn(listOf(deliverySession))

    val sessionsResponse = sessionsController.getSessionsForActionPlan(actionPlanId)

    assertThat(sessionsResponse.size).isEqualTo(1)
    assertThat(sessionsResponse.first()).isEqualTo(DeliverySessionDTO.from(deliverySession))
  }

  @Test
  fun `updates session appointment with attendance details`() {
    val user = authUserFactory.create()
    val userToken = jwtTokenFactory.create(user)
    val request = AttendanceFeedbackRequestDTO(Attended.YES, "more info")
    val actionPlan = actionPlanFactory.create()
    val sessionNumber = 1

    val updatedSession = deliverySessionFactory.createAttended(
      sessionNumber = sessionNumber,
      attended = Attended.YES,
      attendanceFailureInformation = "more info",
    )

    whenever(
      sessionsService.recordAttendanceFeedback(
        user,
        actionPlan.id,
        sessionNumber,
        request.attended,
        request.attendanceFailureInformation,
      ),
    ).thenReturn(updatedSession)

    whenever(authUserRepository.save(any())).thenReturn(user)

    val sessionResponse = sessionsController.recordAttendance(actionPlan.id, sessionNumber, request, userToken)

    assertThat(sessionResponse.appointmentFeedback.attendanceFeedback.attendanceFailureInformation).isEqualTo("more info")
  }
}
