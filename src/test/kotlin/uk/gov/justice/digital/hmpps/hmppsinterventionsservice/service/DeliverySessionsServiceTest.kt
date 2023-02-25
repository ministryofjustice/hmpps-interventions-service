package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNotNull
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AppointmentDeliveryType
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AppointmentSessionType
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AppointmentType.SERVICE_DELIVERY
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Attended
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AuthUser
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.DeliverySession
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ActionPlanRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.AppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.AuthUserRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.DeliverySessionRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ReferralRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ActionPlanFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.AuthUserFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.DeliverySessionFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ReferralFactory
import java.time.OffsetDateTime
import java.util.UUID
import jakarta.persistence.EntityExistsException
import jakarta.persistence.EntityNotFoundException

internal class DeliverySessionsServiceTest {

  private val actionPlanRepository: ActionPlanRepository = mock()
  private val deliverySessionRepository: DeliverySessionRepository = mock()
  private val authUserRepository: AuthUserRepository = mock()
  private val communityAPIBookingService: CommunityAPIBookingService = mock()
  private val appointmentRepository: AppointmentRepository = mock()
  private val appointmentService: AppointmentService = mock()
  private val referralRepository: ReferralRepository = mock()
  private val actionPlanFactory = ActionPlanFactory()
  private val deliverySessionFactory = DeliverySessionFactory()
  private val authUserFactory = AuthUserFactory()
  private val referralFactory = ReferralFactory()

  private val deliverySessionsService = DeliverySessionService(
    deliverySessionRepository,
    actionPlanRepository,
    authUserRepository,
    communityAPIBookingService,
    appointmentService,
    appointmentRepository,
    referralRepository,
  )

  private fun createActor(userName: String = "action_plan_session_test"): AuthUser =
    authUserFactory.create(userName = userName)
      .also { whenever(authUserRepository.save(it)).thenReturn(it) }

  @Test
  fun `create unscheduled sessions creates one for each action plan session`() {
    val actionPlan = actionPlanFactory.create(numberOfSessions = 3)
    whenever(deliverySessionRepository.findByReferralIdAndSessionNumber(eq(actionPlan.referral.id), any())).thenReturn(null)
    whenever(authUserRepository.save(actionPlan.createdBy)).thenReturn(actionPlan.createdBy)
    whenever(deliverySessionRepository.save(any())).thenAnswer { it.arguments[0] }

    deliverySessionsService.createUnscheduledSessionsForActionPlan(actionPlan)
    verify(deliverySessionRepository, times(3)).save(any())
  }

  @Test
  fun `create unscheduled sessions where there is a previously approved action plan`() {
    val newActionPlanId = UUID.randomUUID()
    val referral = referralFactory.createSent()
    val previouslyApprovedActionPlan = actionPlanFactory.createApproved(numberOfSessions = 2, referral = referral)
    val newActionPlan = actionPlanFactory.createSubmitted(id = newActionPlanId, numberOfSessions = 3, referral = referral)
    referral.actionPlans = mutableListOf(previouslyApprovedActionPlan, newActionPlan)

    whenever(deliverySessionRepository.findAllByActionPlanId(any())).thenReturn(listOf(deliverySessionFactory.createAttended(), deliverySessionFactory.createAttended()))
    whenever(deliverySessionRepository.findAllByActionPlanIdAndSessionNumber(eq(newActionPlan.id), any())).thenReturn(null)
    whenever(authUserRepository.save(newActionPlan.createdBy)).thenReturn(newActionPlan.createdBy)
    whenever(deliverySessionRepository.save(any())).thenAnswer { it.arguments[0] }

    deliverySessionsService.createUnscheduledSessionsForActionPlan(newActionPlan)
    verify(deliverySessionRepository, times(1)).save(any())
  }

  @Test
  fun `create unscheduled sessions throws exception if session already exists`() {
    val actionPlan = actionPlanFactory.create(numberOfSessions = 1)

    whenever(deliverySessionRepository.findAllByActionPlanIdAndSessionNumber(actionPlan.id, 1))
      .thenReturn(deliverySessionFactory.createUnscheduled(sessionNumber = 1))

    assertThrows(EntityExistsException::class.java) {
      deliverySessionsService.createUnscheduledSessionsForActionPlan(actionPlan)
    }
  }

  @Test
  fun `updates a session appointment for an unscheduled session`() {
    val session = deliverySessionFactory.createUnscheduled()
    val actionPlanId = UUID.randomUUID()
    val sessionNumber = session.sessionNumber
    val user = createActor("scheduler")
    val appointmentTime = OffsetDateTime.now().plusHours(1)
    val durationInMinutes = 200
    val appointment = session.currentAppointment

    whenever(deliverySessionRepository.findAllByActionPlanIdAndSessionNumber(actionPlanId, sessionNumber)).thenReturn(session)
    whenever(deliverySessionRepository.saveAndFlush(any())).thenReturn(session)

    whenever(communityAPIBookingService.book(session.referral, appointment, appointmentTime, durationInMinutes, SERVICE_DELIVERY, null, null, null))
      .thenReturn(Pair(73457252L, UUID.randomUUID()))

    val updatedSession = deliverySessionsService.updateSessionAppointment(
      actionPlanId,
      sessionNumber,
      appointmentTime,
      durationInMinutes,
      user,
      AppointmentDeliveryType.PHONE_CALL,
      AppointmentSessionType.ONE_TO_ONE,
      null,
    )

    verify(appointmentService, times(1)).createOrUpdateAppointmentDeliveryDetails(any(), eq(AppointmentDeliveryType.PHONE_CALL), eq(AppointmentSessionType.ONE_TO_ONE), isNull(), isNull())
    assertThat(updatedSession.currentAppointment?.appointmentTime).isEqualTo(appointmentTime)
    assertThat(updatedSession.currentAppointment?.durationInMinutes).isEqualTo(durationInMinutes)
    assertThat(updatedSession.currentAppointment?.createdBy?.userName).isEqualTo("scheduler")
    assertThat(updatedSession.currentAppointment?.attended).isNull()
    assertThat(updatedSession.currentAppointment?.additionalAttendanceInformation).isNull()
    assertThat(updatedSession.currentAppointment?.notifyPPOfAttendanceBehaviour).isNull()
    assertThat(updatedSession.currentAppointment?.sessionSummary).isNull()
    assertThat(updatedSession.currentAppointment?.sessionResponse).isNull()
    assertThat(updatedSession.currentAppointment?.attendanceSubmittedAt).isNull()
    assertThat(updatedSession.currentAppointment?.attendanceSubmittedBy).isNull()
    assertThat(updatedSession.currentAppointment?.sessionFeedbackSubmittedAt).isNull()
    assertThat(updatedSession.currentAppointment?.sessionFeedbackSubmittedBy).isNull()
    assertThat(updatedSession.currentAppointment?.appointmentFeedbackSubmittedAt).isNull()
    assertThat(updatedSession.currentAppointment?.appointmentFeedbackSubmittedBy).isNull()
  }

  @Test
  fun `create session appointment for a historic appointment`() {
    val session = deliverySessionFactory.createUnscheduled()
    val actionPlanId = UUID.randomUUID()
    val sessionNumber = session.sessionNumber
    val user = createActor("scheduler")

    whenever(deliverySessionRepository.findAllByActionPlanIdAndSessionNumber(actionPlanId, sessionNumber)).thenReturn(session)
    whenever(deliverySessionRepository.saveAndFlush(any())).thenReturn(session)

    val appointmentTime = OffsetDateTime.now()
    val durationInMinutes = 200
    val appointment = session.currentAppointment

    whenever(communityAPIBookingService.book(session.referral, appointment, appointmentTime, durationInMinutes, SERVICE_DELIVERY, null, Attended.YES, false, true, null))
      .thenReturn(Pair(929478456763L, UUID.randomUUID()))

    val updatedSession = deliverySessionsService.updateSessionAppointment(
      actionPlanId,
      sessionNumber,
      appointmentTime,
      durationInMinutes,
      user,
      AppointmentDeliveryType.PHONE_CALL,
      AppointmentSessionType.ONE_TO_ONE,
      attended = Attended.YES,
      didSessionHappen = true,
      notifyProbationPractitioner = false,
      late = false,
      sessionSummary = "summary",
      sessionResponse = "response",
    )

    verify(appointmentService, times(1)).createOrUpdateAppointmentDeliveryDetails(any(), eq(AppointmentDeliveryType.PHONE_CALL), eq(AppointmentSessionType.ONE_TO_ONE), isNull(), isNull())
    verify(appointmentService).submitAppointmentFeedback(session.currentAppointment!!, user, SERVICE_DELIVERY, session)
    assertThat(updatedSession.currentAppointment?.appointmentTime).isEqualTo(appointmentTime)
    assertThat(updatedSession.currentAppointment?.durationInMinutes).isEqualTo(durationInMinutes)
    assertThat(updatedSession.currentAppointment?.createdBy?.userName).isEqualTo("scheduler")
    assertThat(updatedSession.currentAppointment?.attended).isEqualTo(Attended.YES)
    assertThat(updatedSession.currentAppointment?.didSessionHappen).isEqualTo(true)
    assertThat(updatedSession.currentAppointment?.notifyPPOfAttendanceBehaviour).isEqualTo(false)
    assertThat(updatedSession.currentAppointment?.sessionSummary).isEqualTo("summary")
    assertThat(updatedSession.currentAppointment?.sessionResponse).isEqualTo("response")
    assertThat(updatedSession.currentAppointment?.attendanceSubmittedAt).isNotNull
    assertThat(updatedSession.currentAppointment?.attendanceSubmittedBy).isNotNull
    assertThat(updatedSession.currentAppointment?.sessionFeedbackSubmittedAt).isNotNull
    assertThat(updatedSession.currentAppointment?.sessionFeedbackSubmittedBy).isNotNull
  }

  @Test
  fun `updates session appointment for a historic appointment non attended`() {
    val session = deliverySessionFactory.createUnscheduled()
    val actionPlanId = UUID.randomUUID()
    val sessionNumber = session.sessionNumber
    val user = createActor("scheduler")

    whenever(deliverySessionRepository.findAllByActionPlanIdAndSessionNumber(actionPlanId, sessionNumber)).thenReturn(session)
    whenever(deliverySessionRepository.saveAndFlush(any())).thenReturn(session)

    val appointmentTime = OffsetDateTime.now()
    val durationInMinutes = 200

    whenever(communityAPIBookingService.book(any(), isNull(), any(), any(), eq(SERVICE_DELIVERY), isNull(), eq(Attended.NO), eq(false), eq(true), isNull()))
      .thenReturn(Pair(46298523523L, UUID.randomUUID()))

    val updatedSession = deliverySessionsService.updateSessionAppointment(
      actionPlanId,
      sessionNumber,
      appointmentTime,
      durationInMinutes,
      user,
      AppointmentDeliveryType.PHONE_CALL,
      AppointmentSessionType.ONE_TO_ONE,
      attended = Attended.NO,
      didSessionHappen = true,
      notifyProbationPractitioner = false,
    )

    verify(appointmentService, times(1)).createOrUpdateAppointmentDeliveryDetails(any(), eq(AppointmentDeliveryType.PHONE_CALL), eq(AppointmentSessionType.ONE_TO_ONE), isNull(), isNull())
    assertThat(updatedSession.currentAppointment?.appointmentTime).isEqualTo(appointmentTime)
    assertThat(updatedSession.currentAppointment?.durationInMinutes).isEqualTo(durationInMinutes)
    assertThat(updatedSession.currentAppointment?.createdBy?.userName).isEqualTo("scheduler")
    assertThat(updatedSession.currentAppointment?.attended).isEqualTo(Attended.NO)
    assertThat(updatedSession.currentAppointment?.didSessionHappen).isEqualTo(true)
    assertThat(updatedSession.currentAppointment?.notifyPPOfAttendanceBehaviour).isFalse()
    assertThat(updatedSession.currentAppointment?.sessionSummary).isNull()
    assertThat(updatedSession.currentAppointment?.sessionResponse).isNull()
    assertThat(updatedSession.currentAppointment?.attendanceSubmittedAt).isNotNull
    assertThat(updatedSession.currentAppointment?.attendanceSubmittedBy).isEqualTo(user)
  }

  @Test
  fun `updates a session appointment for a scheduled session`() {
    val originalTime = OffsetDateTime.now()
    val originalDuration = 60
    val session = deliverySessionFactory.createScheduled(appointmentTime = originalTime, durationInMinutes = originalDuration)
    val actionPlanId = UUID.randomUUID()
    val sessionNumber = session.sessionNumber
    val user = createActor("re-scheduler")

    val newTime = OffsetDateTime.now()
    val newDuration = 200

    whenever(communityAPIBookingService.book(any(), isNotNull(), eq(newTime), eq(newDuration), eq(SERVICE_DELIVERY), isNull(), isNull(), isNull(), isNull(), isNull()))
      .thenReturn(Pair(23523541087L, UUID.randomUUID()))
    whenever(deliverySessionRepository.findAllByActionPlanIdAndSessionNumber(actionPlanId, sessionNumber)).thenReturn(session)
    whenever(deliverySessionRepository.saveAndFlush(any())).thenAnswer { it.arguments[0] }

    val updatedSession = deliverySessionsService.updateSessionAppointment(
      actionPlanId,
      sessionNumber,
      newTime,
      newDuration,
      user,
      AppointmentDeliveryType.PHONE_CALL,
      AppointmentSessionType.ONE_TO_ONE,
      null,
    )

    verify(appointmentService, times(1)).createOrUpdateAppointmentDeliveryDetails(any(), eq(AppointmentDeliveryType.PHONE_CALL), eq(AppointmentSessionType.ONE_TO_ONE), isNull(), isNull())
    assertThat(updatedSession.currentAppointment?.appointmentTime).isEqualTo(newTime)
    assertThat(updatedSession.currentAppointment?.durationInMinutes).isEqualTo(newDuration)
    assertThat(updatedSession.currentAppointment?.createdBy?.userName).isNotEqualTo("re-scheduler")
    assertThat(updatedSession.currentAppointment?.attended).isNull()
    assertThat(updatedSession.currentAppointment?.additionalAttendanceInformation).isNull()
    assertThat(updatedSession.currentAppointment?.notifyPPOfAttendanceBehaviour).isNull()
    assertThat(updatedSession.currentAppointment?.sessionSummary).isNull()
    assertThat(updatedSession.currentAppointment?.sessionResponse).isNull()
    assertThat(updatedSession.currentAppointment?.attendanceSubmittedAt).isNull()
    assertThat(updatedSession.currentAppointment?.attendanceSubmittedBy).isNull()
    assertThat(updatedSession.currentAppointment?.sessionFeedbackSubmittedAt).isNull()
    assertThat(updatedSession.currentAppointment?.sessionFeedbackSubmittedBy).isNull()
    assertThat(updatedSession.currentAppointment?.appointmentFeedbackSubmittedAt).isNull()
  }

  @Test
  fun `makes a booking when a session is updated`() {
    val session = deliverySessionFactory.createScheduled()
    val appointment = session.currentAppointment
    val actionPlanId = UUID.randomUUID()
    val sessionNumber = session.sessionNumber
    val referral = session.referral
    val createdByUser = createActor("scheduler")
    val appointmentTime = OffsetDateTime.now()
    val durationInMinutes = 15

    whenever(
      communityAPIBookingService.book(
        referral,
        appointment,
        appointmentTime,
        durationInMinutes,
        SERVICE_DELIVERY,
        null,
      ),
    ).thenReturn(Pair(999L, appointment?.id))
    whenever(deliverySessionRepository.findAllByActionPlanIdAndSessionNumber(actionPlanId, sessionNumber))
      .thenReturn(session)
    whenever(deliverySessionRepository.saveAndFlush(any())).thenReturn(session)

    val updatedSession = deliverySessionsService.updateSessionAppointment(
      actionPlanId,
      sessionNumber,
      appointmentTime,
      durationInMinutes,
      createdByUser,
      AppointmentDeliveryType.PHONE_CALL,
      AppointmentSessionType.ONE_TO_ONE,
      null,
    )

    assertThat(updatedSession).isEqualTo(session)
    verify(appointmentService, times(1)).createOrUpdateAppointmentDeliveryDetails(any(), eq(AppointmentDeliveryType.PHONE_CALL), eq(AppointmentSessionType.ONE_TO_ONE), isNull(), isNull())
    verify(communityAPIBookingService).book(
      referral,
      appointment,
      appointmentTime,
      durationInMinutes,
      SERVICE_DELIVERY,
      null,
    )
    verify(appointmentRepository, times(1)).saveAndFlush(
      ArgumentMatchers.argThat {
        it.deliusAppointmentId == 999L
      },
    )
  }

  @Test
  fun `does not make a booking when a session is updated because timings aren't present`() {
    val session = deliverySessionFactory.createScheduled()
    val actionPlanId = UUID.randomUUID()
    val sessionNumber = session.sessionNumber
    val referral = session.referral
    val createdByUser = createActor("scheduler")
    val appointmentTime = OffsetDateTime.now()
    val durationInMinutes = 15

    whenever(
      communityAPIBookingService.book(
        referral,
        session.currentAppointment,
        appointmentTime,
        durationInMinutes,
        SERVICE_DELIVERY,
        null,
      ),
    ).thenReturn(Pair(null, null))
    whenever(deliverySessionRepository.findAllByActionPlanIdAndSessionNumber(actionPlanId, sessionNumber)).thenReturn(session)
    whenever(deliverySessionRepository.saveAndFlush(any())).thenReturn(session)

    deliverySessionsService.updateSessionAppointment(
      actionPlanId,
      sessionNumber,
      appointmentTime,
      durationInMinutes,
      createdByUser,
      AppointmentDeliveryType.PHONE_CALL,
      AppointmentSessionType.ONE_TO_ONE,
      null,
    )

    verify(appointmentService, times(1)).createOrUpdateAppointmentDeliveryDetails(any(), eq(AppointmentDeliveryType.PHONE_CALL), eq(AppointmentSessionType.ONE_TO_ONE), isNull(), isNull())
    verify(appointmentRepository, times(2)).saveAndFlush(
      ArgumentMatchers.argThat {
        it.deliusAppointmentId == null
      },
    )
  }

  @Test
  fun `updates a session and throws exception if it not exists`() {
    val actionPlanId = UUID.randomUUID()
    val sessionNumber = 1
    val appointmentTime = OffsetDateTime.now()
    val durationInMinutes = 15

    whenever(deliverySessionRepository.findByReferralIdAndSessionNumber(actionPlanId, sessionNumber)).thenReturn(null)

    val exception = assertThrows(EntityNotFoundException::class.java) {
      deliverySessionsService.updateSessionAppointment(
        actionPlanId,
        sessionNumber,
        appointmentTime,
        durationInMinutes,
        authUserFactory.create(),
        AppointmentDeliveryType.PHONE_CALL,
        AppointmentSessionType.ONE_TO_ONE,
        null,
      )
    }
    assertThat(exception.message).isEqualTo("Action plan session not found [actionPlanId=$actionPlanId, sessionNumber=$sessionNumber]")
  }

  @Test
  fun `gets a session`() {
    val time = OffsetDateTime.now()
    val duration = 500
    val session = deliverySessionFactory.createScheduled(sessionNumber = 1, appointmentTime = time, durationInMinutes = duration)

    whenever(deliverySessionRepository.findByReferralIdAndSessionNumber(session.referral.id, 1)).thenReturn(session)

    val actualSession = deliverySessionsService.getSession(session.referral.id, 1)

    assertThat(actualSession.sessionNumber).isEqualTo(1)
    assertThat(actualSession.currentAppointment?.appointmentTime).isEqualTo(time)
    assertThat(actualSession.currentAppointment?.durationInMinutes).isEqualTo(duration)
  }

  @Test
  fun `gets a session and throws exception if it not exists`() {
    val referralId = UUID.randomUUID()
    val sessionNumber = 1

    whenever(deliverySessionRepository.findByReferralIdAndSessionNumber(referralId, sessionNumber)).thenReturn(null)

    val exception = assertThrows(EntityNotFoundException::class.java) {
      deliverySessionsService.getSession(referralId, sessionNumber)
    }
    assertThat(exception.message).isEqualTo("Action plan session not found [referralId=$referralId, sessionNumber=$sessionNumber]")
  }

  @Test
  fun `gets all sessions for an action plan`() {
    val time = OffsetDateTime.now()
    val duration = 500
    val session = deliverySessionFactory.createScheduled(sessionNumber = 1, appointmentTime = time, durationInMinutes = duration)
    val referralId = UUID.randomUUID()

    whenever(deliverySessionRepository.findAllByReferralId(referralId)).thenReturn(listOf(session))

    val sessions = deliverySessionsService.getSessions(referralId)

    assertThat(sessions.first().sessionNumber).isEqualTo(1)
    assertThat(sessions.first().currentAppointment?.appointmentTime).isEqualTo(time)
    assertThat(sessions.first().currentAppointment?.durationInMinutes).isEqualTo(duration)
  }

  @Test
  fun `update session with attendance`() {
    val attended = Attended.YES
    val didSessionHappen = true

    val existingSession = deliverySessionFactory.createScheduled(sessionNumber = 1, appointmentTime = OffsetDateTime.now())
    val referralId = existingSession.referral.id
    val appointmentId = existingSession.currentAppointment!!.id

    whenever(deliverySessionRepository.findAllByReferralId(referralId)).thenReturn(listOf(existingSession))
    whenever(deliverySessionRepository.save(any())).thenReturn(existingSession)

    val actor = createActor("attendance_submitter")
    val savedSession = deliverySessionsService.recordAttendanceFeedback(referralId, appointmentId, actor, attended, didSessionHappen)

    verify(appointmentService).recordAppointmentAttendance(existingSession.currentAppointment!!, attended, didSessionHappen, actor)
    assertThat(savedSession).isNotNull
  }

  @Test
  fun `update session with attendance - no session found`() {
    val attended = Attended.YES
    val didSessionHappen = true
    val referralId = UUID.randomUUID()
    val appointmentId = UUID.randomUUID()

    whenever(deliverySessionRepository.findAllByReferralId(referralId)).thenReturn(listOf())

    val actor = createActor()
    val exception = assertThrows(EntityNotFoundException::class.java) {
      deliverySessionsService.recordAttendanceFeedback(referralId, appointmentId, actor, attended, didSessionHappen)
    }

    assertThat(exception.message).isEqualTo("No Delivery Session Appointment found [referralId=$referralId, appointmentId=$appointmentId]")
  }

  @Test
  fun `updating session feedback sets relevant fields`() {
    val session = deliverySessionFactory.createScheduled(appointmentTime = OffsetDateTime.now())
    val actor = createActor("behaviour_submitter")
    val referral = session.referral
    val appointment = session.currentAppointment!!

    whenever(deliverySessionRepository.findAllByReferralId(any())).thenReturn(listOf(session))
    whenever(deliverySessionRepository.save(any())).thenReturn(session)
    whenever(
      appointmentService.recordSessionFeedback(
        appointment,
        false,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        "activities",
        "not good",
        "concerns",
        false,
        actor,
      ),
    ).thenReturn(appointment.copy(sessionSummary = "activities", sessionResponse = "not good", sessionConcerns = "concerns", notifyPPOfAttendanceBehaviour = false, sessionFeedbackSubmittedAt = OffsetDateTime.now(), sessionFeedbackSubmittedBy = actor))

    val sessionAndAppointment = deliverySessionsService.recordSessionFeedback(
      referral.id,
      appointment.id,
      actor,
      false,
      null,
      null,
      null,
      null,
      null,
      null,
      null,
      "activities",
      "not good",
      "concerns",
      false,
    )

    verify(appointmentService).recordSessionFeedback(
      appointment,
      false,
      null,
      null,
      null,
      null,
      null,
      null,
      null,
      "activities",
      "not good",
      "concerns",
      false,
      actor,
    )
    assertThat(sessionAndAppointment.first).isSameAs(session)
    assertThat(sessionAndAppointment.second.sessionResponse).isEqualTo("not good")
    assertThat(sessionAndAppointment.second.sessionConcerns).isEqualTo("concerns")
    assertThat(sessionAndAppointment.second.notifyPPOfAttendanceBehaviour).isFalse
    assertThat(sessionAndAppointment.second.sessionFeedbackSubmittedAt).isNotNull
    assertThat(sessionAndAppointment.second.sessionFeedbackSubmittedBy?.userName).isEqualTo("behaviour_submitter")
  }

  @Test
  fun `updating session behaviour for missing session throws error`() {
    val actor = createActor()
    whenever(deliverySessionRepository.findByReferralIdAndSessionNumber(any(), any())).thenReturn(null)

    assertThrows(EntityNotFoundException::class.java) {
      deliverySessionsService.recordSessionFeedback(
        UUID.randomUUID(),
        UUID.randomUUID(),
        actor,
        false,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        "activities",
        "not good",
        null,
        false,
      )
    }
  }

  @Test
  fun `appointment feedback cant be submitted more than once`() {
    val session = deliverySessionFactory.createScheduled(appointmentTime = OffsetDateTime.now())
    val referralId = session.referral.id
    val appointmentId = session.currentAppointment!!.id
    val actor = createActor()

    whenever(deliverySessionRepository.findAllByReferralId(referralId)).thenReturn(listOf(session))
    whenever(deliverySessionRepository.save(any())).thenReturn(session)
    whenever(appointmentService.submitAppointmentFeedback(session.currentAppointment!!, actor, SERVICE_DELIVERY, session)).thenThrow(ResponseStatusException(HttpStatus.CONFLICT, "appointment feedback has already been submitted"))

    deliverySessionsService.recordAttendanceFeedback(referralId, appointmentId, actor, Attended.YES, true)
    deliverySessionsService.recordSessionFeedback(
      referralId,
      appointmentId,
      actor,
      false,
      null,
      null,
      null,
      null,
      null,
      null,
      null,
      "activities",
      "bad",
      null,
      false,
    )

    val exception = assertThrows(ResponseStatusException::class.java) {
      deliverySessionsService.submitAppointmentFeedback(referralId, appointmentId, actor)
    }
    assertThat(exception.status).isEqualTo(HttpStatus.CONFLICT)
  }

  @Test
  fun `appointment feedback can't be submitted without attendance`() {
    val session = deliverySessionFactory.createScheduled(appointmentTime = OffsetDateTime.now())
    val referralId = session.referral.id
    val appointmentId = session.currentAppointment!!.id

    val actor = createActor()
    whenever(deliverySessionRepository.findAllByReferralId(referralId)).thenReturn(listOf(session))
    whenever(deliverySessionRepository.save(any())).thenReturn(session)
    whenever(appointmentService.submitAppointmentFeedback(session.currentAppointment!!, actor, SERVICE_DELIVERY, session)).thenThrow(
      ResponseStatusException(
        HttpStatus.UNPROCESSABLE_ENTITY,
        "can't submit feedback unless attendance has been recorded",
      ),
    )

    deliverySessionsService.recordSessionFeedback(
      referralId,
      appointmentId,
      actor,
      false,
      null,
      null,
      null,
      null,
      null,
      null,
      null,
      "activities",
      "bad",
      null,
      false,
    )

    val exception = assertThrows(ResponseStatusException::class.java) {
      deliverySessionsService.submitAppointmentFeedback(referralId, appointmentId, actor)
    }
    assertThat(exception.status).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY)
  }

  @Test
  fun `appointment feedback can be submitted and stores time and actor`() {
    val session = deliverySessionFactory.createScheduled(appointmentTime = OffsetDateTime.now())
    val referralId = session.referral.id
    val appointmentId = session.currentAppointment!!.id
    whenever(deliverySessionRepository.findAllByReferralId(referralId)).thenReturn(listOf(session))
    whenever(deliverySessionRepository.save(any())).thenReturn(session)

    val actor = createActor()
    deliverySessionsService.recordAttendanceFeedback(referralId, appointmentId, actor, Attended.YES, true)
    deliverySessionsService.recordSessionFeedback(
      referralId,
      appointmentId,
      actor,
      false,
      null,
      null,
      null,
      null,
      null,
      null,
      null,
      "activities",
      "bad",
      null,
      false,
    )

    val submitter = createActor("test-submitter")
    deliverySessionsService.submitAppointmentFeedback(referralId, appointmentId, submitter)

    verify(appointmentService).submitAppointmentFeedback(session.currentAppointment!!, submitter, SERVICE_DELIVERY, session)
    val sessionCaptor = argumentCaptor<DeliverySession>()
    sessionCaptor.allValues.forEach {
      if (it == sessionCaptor.lastValue) {
        assertThat(it.currentAppointment?.appointmentFeedbackSubmittedAt != null)
        assertThat(it.currentAppointment?.appointmentFeedbackSubmittedBy?.userName).isEqualTo("test-submitter")
      } else {
        assertThat(it.currentAppointment?.appointmentFeedbackSubmittedAt == null)
        assertThat(it.currentAppointment?.appointmentFeedbackSubmittedBy == null)
      }
    }
  }

  @Test
  fun `appointment feedback emits application events`() {
    val actor = createActor()
    val session = deliverySessionFactory.createScheduled(appointmentTime = OffsetDateTime.now())
    val referral = session.referral
    val appointment = session.currentAppointment!!
    whenever(deliverySessionRepository.findAllByReferralId(referral.id)).thenReturn(
      listOf(session),
    )
    whenever(deliverySessionRepository.save(any())).thenReturn(session)

    deliverySessionsService.recordAttendanceFeedback(referral.id, appointment.id, actor, Attended.YES, true)
    deliverySessionsService.recordSessionFeedback(
      referral.id,
      appointment.id,
      actor,
      false,
      null,
      null,
      null,
      null,
      null,
      null,
      null,
      "activities",
      "bad",
      null,
      false,
    )
    deliverySessionsService.submitAppointmentFeedback(referral.id, appointment.id, actor)

    verify(appointmentService).recordAppointmentAttendance(appointment, Attended.YES, true, actor)
    verify(appointmentService).recordSessionFeedback(
      appointment, false,
      null,
      null,
      null,
      null,
      null,
      null,
      null,
      "activities",
      "bad",
      null,
      false,
      actor,
    )
    verify(appointmentService).submitAppointmentFeedback(appointment, actor, SERVICE_DELIVERY, session)
  }

  @Test
  fun `attendance can't be updated once appointment feedback has been submitted`() {
    val user = createActor()
    val session = deliverySessionFactory.createAttended()
    val referralId = session.referral.id
    val appointmentId = session.currentAppointment!!.id
    whenever(deliverySessionRepository.findAllByReferralId(referralId)).thenReturn(listOf(session))
    whenever(appointmentService.recordAppointmentAttendance(session.currentAppointment!!, Attended.YES, true, user)).thenThrow(ResponseStatusException::class.java)

    assertThrows(ResponseStatusException::class.java) {
      deliverySessionsService.recordAttendanceFeedback(referralId, appointmentId, user, Attended.YES, true)
    }
  }

  @Test
  fun `session feedback can't be updated once appointment feedback has been submitted`() {
    val actor = createActor()
    val session = deliverySessionFactory.createAttended()
    val referralId = session.referral.id
    val appointmentId = session.currentAppointment!!.id
    whenever(deliverySessionRepository.findAllByReferralId(referralId)).thenReturn(listOf(session))
    whenever(
      appointmentService.recordSessionFeedback(
        session.currentAppointment!!,
        false,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        "activities",
        "bad",
        null,
        false,
        actor,
      ),
    ).thenThrow(ResponseStatusException::class.java)

    assertThrows(ResponseStatusException::class.java) {
      deliverySessionsService.recordSessionFeedback(
        referralId,
        appointmentId,
        actor, false,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        "activities",
        "bad",
        null,
        false,
      )
    }
  }

  @Test
  fun `appointment feedback can be submitted when session not attended`() {
    val session = deliverySessionFactory.createScheduled(appointmentTime = OffsetDateTime.now())
    val referralId = session.referral.id
    val appointmentId = session.currentAppointment!!.id
    whenever(deliverySessionRepository.findAllByReferralId(referralId)).thenReturn(listOf(session))
    whenever(deliverySessionRepository.save(any())).thenReturn(session)

    val actor = createActor()
    deliverySessionsService.recordAttendanceFeedback(referralId, appointmentId, actor, Attended.NO, true)
    deliverySessionsService.submitAppointmentFeedback(referralId, appointmentId, actor)

    verify(appointmentService).submitAppointmentFeedback(session.currentAppointment!!, actor, SERVICE_DELIVERY, session)
  }

  @Test
  fun `makes a booking with delius office location`() {
    val session = deliverySessionFactory.createScheduled()
    val appointment = session.currentAppointment
    val actionPlanId = UUID.randomUUID()
    val sessionNumber = session.sessionNumber
    val referral = session.referral
    val createdByUser = session.referral.createdBy
    val appointmentTime = OffsetDateTime.now()
    val durationInMinutes = 15
    val npsOfficeCode = "CRS0001"

    whenever(
      communityAPIBookingService.book(
        referral,
        appointment,
        appointmentTime,
        durationInMinutes,
        SERVICE_DELIVERY,
        npsOfficeCode,
      ),
    ).thenReturn(Pair(999L, appointment?.id))
    whenever(deliverySessionRepository.findAllByActionPlanIdAndSessionNumber(actionPlanId, sessionNumber)).thenReturn(
      session,
    )
    whenever(authUserRepository.save(createdByUser)).thenReturn(createdByUser)
    whenever(deliverySessionRepository.saveAndFlush(any())).thenReturn(session)

    val updatedSession = deliverySessionsService.updateSessionAppointment(
      actionPlanId,
      sessionNumber,
      appointmentTime,
      durationInMinutes,
      createdByUser,
      AppointmentDeliveryType.IN_PERSON_MEETING_PROBATION_OFFICE,
      AppointmentSessionType.ONE_TO_ONE,
      null,
      npsOfficeCode,
    )

    assertThat(updatedSession).isEqualTo(session)
    verify(appointmentService, times(1)).createOrUpdateAppointmentDeliveryDetails(any(), eq(AppointmentDeliveryType.IN_PERSON_MEETING_PROBATION_OFFICE), eq(AppointmentSessionType.ONE_TO_ONE), isNull(), eq(npsOfficeCode))
    verify(communityAPIBookingService).book(
      referral,
      appointment,
      appointmentTime,
      durationInMinutes,
      SERVICE_DELIVERY,
      npsOfficeCode,
    )
    verify(appointmentRepository, times(1)).saveAndFlush(
      ArgumentMatchers.argThat {
        it.deliusAppointmentId == 999L
      },
    )
  }
}
