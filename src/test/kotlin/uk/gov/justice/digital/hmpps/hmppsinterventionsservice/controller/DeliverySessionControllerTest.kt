package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.controller

import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
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
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Attended.NO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Attended.YES
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Status
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.AuthUserRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.ActionPlanService
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.AppointmentService
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.DeliverySessionService
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.ReferralService
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ActionPlanFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.AppointmentFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.AuthUserFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.DeliverySessionFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.JwtTokenFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.validator.AppointmentValidator
import java.time.OffsetDateTime
import java.util.UUID

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
  private val appointmentFactory = AppointmentFactory()
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

      val attendanceFeedbackRequestDTO = AttendanceFeedbackRequestDTO(YES, true)
      val sessionFeedbackRequestDTO = SessionFeedbackRequestDTO(
        late = false,
        sessionSummary = "summary",
        sessionResponse = "response",
        notifyProbationPractitionerOfBehaviour = false,
        notifyProbationPractitionerOfConcerns = false,
      )
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
          attended = YES,
          didSessionHappen = true,
          notifyProbationPractitionerOfBehaviour = false,
          notifyProbationPractitionerOfConcerns = false,
          late = false,
          sessionSummary = "summary",
          sessionResponse = "response",
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

      val attendanceFeedbackRequestDTO = AttendanceFeedbackRequestDTO(NO, true)
      val sessionFeedbackRequestDTO = SessionFeedbackRequestDTO(
        late = false,
        sessionSummary = "summary",
        sessionResponse = "response",
        notifyProbationPractitionerOfBehaviour = false,
        notifyProbationPractitionerOfConcerns = false,
      )
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
          attended = NO,
          didSessionHappen = true,
          notifyProbationPractitionerOfBehaviour = false,
          notifyProbationPractitionerOfConcerns = false,
          late = false,
          sessionSummary = "summary",
          sessionResponse = "response",
        ),
      ).thenReturn(deliverySession)

      val sessionResponse = sessionsController.updateSessionAppointment(actionPlanId, sessionNumber, updateAppointmentDTO, userToken)

      assertThat(sessionResponse).isEqualTo(DeliverySessionDTO.from(deliverySession))
    }

    @Test
    fun `updates a session and sets referral status to post ica when the session was attended`() {
      val user = authUserFactory.create()
      val userToken = jwtTokenFactory.create(user)
      val deliverySession = deliverySessionFactory.createScheduled(createdBy = user)
      val actionPlanId = UUID.randomUUID()
      val sessionNumber = deliverySession.sessionNumber

      val attendanceFeedbackRequestDTO = AttendanceFeedbackRequestDTO(attended = YES, didSessionHappen = false)
      val updateAppointmentDTO = UpdateAppointmentDTO(OffsetDateTime.now(), 10, AppointmentDeliveryType.PHONE_CALL, AppointmentSessionType.ONE_TO_ONE, null, null, attendanceFeedbackRequestDTO)

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
          attended = YES,
          didSessionHappen = false,
        ),
      ).thenReturn(deliverySession)

      val sessionResponse = sessionsController.updateSessionAppointment(actionPlanId, sessionNumber, updateAppointmentDTO, userToken)

      verify(referralService, times(1)).setReferralStatus(deliverySession.currentAppointment!!.referral, Status.POST_ICA)

      assertThat(sessionResponse).isEqualTo(DeliverySessionDTO.from(deliverySession))
    }

    @Test
    fun `updates a session and does not set referral status to post ica when the session was not attended`() {
      val user = authUserFactory.create()
      val userToken = jwtTokenFactory.create(user)
      val deliverySession = deliverySessionFactory.createScheduled(createdBy = user)
      val actionPlanId = UUID.randomUUID()
      val sessionNumber = deliverySession.sessionNumber

      val attendanceFeedbackRequestDTO = AttendanceFeedbackRequestDTO(attended = NO, didSessionHappen = false)
      val updateAppointmentDTO = UpdateAppointmentDTO(OffsetDateTime.now(), 10, AppointmentDeliveryType.PHONE_CALL, AppointmentSessionType.ONE_TO_ONE, null, null, attendanceFeedbackRequestDTO)

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
          attended = NO,
          didSessionHappen = false,
        ),
      ).thenReturn(deliverySession)

      val sessionResponse = sessionsController.updateSessionAppointment(actionPlanId, sessionNumber, updateAppointmentDTO, userToken)

      verify(referralService, times(0)).setReferralStatus(deliverySession.currentAppointment!!.referral, Status.POST_ICA)

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
    val request = AttendanceFeedbackRequestDTO(YES, true)
    val deliverySession = deliverySessionFactory.createScheduled()
    val referral = deliverySession.referral
    val appointment = deliverySession.currentAppointment!!

    val updateAppointment = appointmentFactory.create(
      attended = YES,
      didSessionHappen = true,
    )

    whenever(
      sessionsService.recordAttendanceFeedback(
        referral.id,
        appointment.id,
        user,
        request.attended,
        request.didSessionHappen,
      ),
    ).thenReturn(Pair(deliverySession, updateAppointment))

    whenever(
      referralService.getSentReferral(referral.id),
    ).thenReturn(deliverySession.referral)

    whenever(authUserRepository.save(any())).thenReturn(user)

    val sessionResponse = sessionsController.recordAttendanceFeedback(referral.id, appointment.id, request, userToken)

    assertThat(sessionResponse.appointmentFeedback.attendanceFeedback.didSessionHappen).isEqualTo(true)
    assertThat(sessionResponse.appointmentFeedback.attendanceFeedback.attended).isEqualTo(YES)
  }
}
