package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service

import jakarta.persistence.EntityExistsException
import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.eq
import org.mockito.kotlin.firstValue
import org.mockito.kotlin.isNotNull
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.ActionPlanAppointmentEventPublisher
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

internal class DeliverySessionsServiceTest {

  private val actionPlanRepository: ActionPlanRepository = mock()
  private val deliverySessionRepository: DeliverySessionRepository = mock()
  private val authUserRepository: AuthUserRepository = mock()
  private val actionPlanAppointmentEventPublisher: ActionPlanAppointmentEventPublisher = mock()
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
    actionPlanAppointmentEventPublisher,
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

    whenever(communityAPIBookingService.book(session.referral, appointment, appointmentTime, durationInMinutes, SERVICE_DELIVERY, null, Attended.YES, false))
      .thenReturn(Pair(929478456763L, UUID.randomUUID()))

    val updatedSession = deliverySessionsService.updateSessionAppointment(
      actionPlanId,
      sessionNumber,
      appointmentTime,
      durationInMinutes,
      user,
      AppointmentDeliveryType.PHONE_CALL,
      AppointmentSessionType.ONE_TO_ONE,
      null,
      null,
      Attended.YES,
      "attendance failure information",
      false,
      "summary",
      "response",
    )

    verify(appointmentService, times(1)).createOrUpdateAppointmentDeliveryDetails(any(), eq(AppointmentDeliveryType.PHONE_CALL), eq(AppointmentSessionType.ONE_TO_ONE), isNull(), isNull())
    verify(actionPlanAppointmentEventPublisher).attendanceRecordedEvent(updatedSession)
    verify(actionPlanAppointmentEventPublisher).sessionFeedbackRecordedEvent(updatedSession)
    verify(actionPlanAppointmentEventPublisher).appointmentFeedbackRecordedEvent(updatedSession)
    assertThat(updatedSession.currentAppointment?.appointmentTime).isEqualTo(appointmentTime)
    assertThat(updatedSession.currentAppointment?.durationInMinutes).isEqualTo(durationInMinutes)
    assertThat(updatedSession.currentAppointment?.createdBy?.userName).isEqualTo("scheduler")
    assertThat(updatedSession.currentAppointment?.attended).isEqualTo(Attended.YES)
    assertThat(updatedSession.currentAppointment?.attendanceFailureInformation).isEqualTo("attendance failure information")
    assertThat(updatedSession.currentAppointment?.notifyPPOfAttendanceBehaviour).isEqualTo(false)
    assertThat(updatedSession.currentAppointment?.sessionSummary).isEqualTo("summary")
    assertThat(updatedSession.currentAppointment?.sessionResponse).isEqualTo("response")
    assertThat(updatedSession.currentAppointment?.attendanceSubmittedAt).isNotNull
    assertThat(updatedSession.currentAppointment?.attendanceSubmittedBy).isNotNull
    assertThat(updatedSession.currentAppointment?.sessionFeedbackSubmittedAt).isNotNull
    assertThat(updatedSession.currentAppointment?.sessionFeedbackSubmittedBy).isNotNull
    assertThat(updatedSession.currentAppointment?.appointmentFeedbackSubmittedAt).isNotNull
    assertThat(updatedSession.currentAppointment?.appointmentFeedbackSubmittedBy).isNotNull
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

    whenever(communityAPIBookingService.book(any(), isNull(), any(), any(), eq(SERVICE_DELIVERY), isNull(), eq(Attended.NO), isNull()))
      .thenReturn(Pair(46298523523L, UUID.randomUUID()))

    val updatedSession = deliverySessionsService.updateSessionAppointment(
      actionPlanId,
      sessionNumber,
      appointmentTime,
      durationInMinutes,
      user,
      AppointmentDeliveryType.PHONE_CALL,
      AppointmentSessionType.ONE_TO_ONE,
      null,
      null,
      Attended.NO,
      "attendance failure information",
    )

    verify(appointmentService, times(1)).createOrUpdateAppointmentDeliveryDetails(any(), eq(AppointmentDeliveryType.PHONE_CALL), eq(AppointmentSessionType.ONE_TO_ONE), isNull(), isNull())
    assertThat(updatedSession.currentAppointment?.appointmentTime).isEqualTo(appointmentTime)
    assertThat(updatedSession.currentAppointment?.durationInMinutes).isEqualTo(durationInMinutes)
    assertThat(updatedSession.currentAppointment?.createdBy?.userName).isEqualTo("scheduler")
    assertThat(updatedSession.currentAppointment?.attended).isEqualTo(Attended.NO)
    assertThat(updatedSession.currentAppointment?.attendanceFailureInformation).isEqualTo("attendance failure information")
    assertThat(updatedSession.currentAppointment?.notifyPPOfAttendanceBehaviour).isNull()
    assertThat(updatedSession.currentAppointment?.sessionSummary).isNull()
    assertThat(updatedSession.currentAppointment?.sessionResponse).isNull()
    assertThat(updatedSession.currentAppointment?.attendanceSubmittedAt).isNotNull
    assertThat(updatedSession.currentAppointment?.attendanceSubmittedBy).isEqualTo(user)
    assertThat(updatedSession.currentAppointment?.sessionFeedbackSubmittedAt).isNull()
    assertThat(updatedSession.currentAppointment?.sessionFeedbackSubmittedBy).isNull()
    assertThat(updatedSession.currentAppointment?.appointmentFeedbackSubmittedAt).isNotNull
    assertThat(updatedSession.currentAppointment?.appointmentFeedbackSubmittedBy).isEqualTo(user)
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

    whenever(communityAPIBookingService.book(any(), isNotNull(), eq(newTime), eq(newDuration), eq(SERVICE_DELIVERY), isNull(), isNull(), isNull()))
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
    val attendanceFailureInformation = "extra info"

    val existingSession = deliverySessionFactory.createScheduled(sessionNumber = 1)
    val actionPlanId = UUID.randomUUID()

    whenever(deliverySessionRepository.findAllByActionPlanIdAndSessionNumber(actionPlanId, 1))
      .thenReturn(existingSession)
    whenever(deliverySessionRepository.save(any())).thenReturn(existingSession)

    val actor = createActor("attendance_submitter")
    val savedSession = deliverySessionsService.recordAttendanceFeedback(actor, actionPlanId, 1, attended, attendanceFailureInformation)
    val argumentCaptor: ArgumentCaptor<DeliverySession> = ArgumentCaptor.forClass(DeliverySession::class.java)

//    verify(appointmentEventPublisher).appointmentNotAttendedEvent(existingSession)
    verify(deliverySessionRepository).save(argumentCaptor.capture())
    assertThat(argumentCaptor.firstValue.currentAppointment?.attended).isEqualTo(attended)
    assertThat(argumentCaptor.firstValue.currentAppointment?.attendanceFailureInformation).isEqualTo(attendanceFailureInformation)
    assertThat(argumentCaptor.firstValue.currentAppointment?.attendanceSubmittedAt).isNotNull
    assertThat(argumentCaptor.firstValue.currentAppointment?.attendanceSubmittedBy?.userName).isEqualTo("attendance_submitter")
    assertThat(savedSession).isNotNull
  }

  @Test
  fun `update session with attendance - no session found`() {
    val actionPlanId = UUID.randomUUID()
    val sessionNumber = 1
    val attended = Attended.YES
    val attendanceFailureInformation = "extra info"

    whenever(deliverySessionRepository.findAllByActionPlanIdAndSessionNumber(actionPlanId, sessionNumber))
      .thenReturn(null)

    val actor = createActor()
    val exception = assertThrows(EntityNotFoundException::class.java) {
      deliverySessionsService.recordAttendanceFeedback(actor, actionPlanId, 1, attended, attendanceFailureInformation)
    }

    assertThat(exception.message).isEqualTo("Action plan session not found [actionPlanId=$actionPlanId, sessionNumber=$sessionNumber]")
  }

  @Test
  fun `updating session feedback sets relevant fields`() {
    val actionPlanId = UUID.randomUUID()
    val session = deliverySessionFactory.createScheduled(appointmentTime = OffsetDateTime.now())
    whenever(deliverySessionRepository.findAllByActionPlanIdAndSessionNumber(any(), any())).thenReturn(session)
    whenever(deliverySessionRepository.save(any())).thenReturn(session)

    val actor = createActor("behaviour_submitter")
    val updatedSession = deliverySessionsService.recordSessionFeedback(actor, actionPlanId, 1, "activities", "not good", "concerns", false)

    verify(deliverySessionRepository, times(1)).save(session)
    assertThat(updatedSession).isSameAs(session)
    assertThat(session.currentAppointment?.sessionResponse).isEqualTo("not good")
    assertThat(session.currentAppointment?.sessionConcerns).isEqualTo("concerns")
    assertThat(session.currentAppointment?.notifyPPOfAttendanceBehaviour).isFalse
    assertThat(session.currentAppointment?.sessionFeedbackSubmittedAt).isNotNull
    assertThat(session.currentAppointment?.sessionFeedbackSubmittedBy?.userName).isEqualTo("behaviour_submitter")
  }

  @Test
  fun `updating session behaviour for missing session throws error`() {
    val actor = createActor()
    whenever(deliverySessionRepository.findByReferralIdAndSessionNumber(any(), any())).thenReturn(null)

    assertThrows(EntityNotFoundException::class.java) {
      deliverySessionsService.recordSessionFeedback(actor, UUID.randomUUID(), 1, "activities", "not good", null, false)
    }
  }

  @Test
  fun `appointment feedback cant be submitted more than once`() {
    val session = deliverySessionFactory.createScheduled(appointmentTime = OffsetDateTime.now())
    val actionPlanId = UUID.randomUUID()

    whenever(deliverySessionRepository.findAllByActionPlanIdAndSessionNumber(actionPlanId, 1)).thenReturn(session)
    whenever(deliverySessionRepository.save(any())).thenReturn(session)

    val actor = createActor()
    deliverySessionsService.recordAttendanceFeedback(actor, actionPlanId, 1, Attended.YES, "")
    deliverySessionsService.recordSessionFeedback(actor, actionPlanId, 1, "activities", "bad", null, false)

    deliverySessionsService.submitAppointmentFeedback(actionPlanId, 1, actor)
    val exception = assertThrows(ResponseStatusException::class.java) {
      deliverySessionsService.submitAppointmentFeedback(actionPlanId, 1, actor)
    }
    assertThat(exception.statusCode).isEqualTo(HttpStatus.CONFLICT)
  }

  @Test
  fun `appointment feedback can't be submitted without attendance`() {
    val session = deliverySessionFactory.createScheduled()
    val actionPlanId = UUID.randomUUID()

    val actor = createActor()
    whenever(deliverySessionRepository.findAllByActionPlanIdAndSessionNumber(actionPlanId, 1)).thenReturn(session)
    whenever(deliverySessionRepository.save(any())).thenReturn(session)

    deliverySessionsService.recordSessionFeedback(actor, actionPlanId, 1, "activities", "bad", null, false)

    val exception = assertThrows(ResponseStatusException::class.java) {
      deliverySessionsService.submitAppointmentFeedback(actionPlanId, 1, actor)
    }
    assertThat(exception.statusCode).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY)
  }

  @Test
  fun `appointment feedback can be submitted and stores time and actor`() {
    val session = deliverySessionFactory.createScheduled(appointmentTime = OffsetDateTime.now())
    val actionPlanId = UUID.randomUUID()
    whenever(deliverySessionRepository.findAllByActionPlanIdAndSessionNumber(actionPlanId, 1)).thenReturn(
      session,
    )
    whenever(deliverySessionRepository.save(any())).thenReturn(session)

    val user = createActor()
    deliverySessionsService.recordAttendanceFeedback(user, actionPlanId, 1, Attended.YES, "")
    deliverySessionsService.recordSessionFeedback(user, actionPlanId, 1, "activities", "bad", null, true)

    val submitter = createActor("test-submitter")
    deliverySessionsService.submitAppointmentFeedback(actionPlanId, 1, submitter)

    val sessionCaptor = argumentCaptor<DeliverySession>()
    verify(deliverySessionRepository, atLeastOnce()).save(sessionCaptor.capture())
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
    val user = createActor()
    val session = deliverySessionFactory.createScheduled(appointmentTime = OffsetDateTime.now())
    val actionPlanId = UUID.randomUUID()
    whenever(deliverySessionRepository.findAllByActionPlanIdAndSessionNumber(actionPlanId, 1)).thenReturn(
      session,
    )
    whenever(deliverySessionRepository.save(any())).thenReturn(session)
    deliverySessionsService.recordAttendanceFeedback(user, actionPlanId, 1, Attended.YES, "")
    deliverySessionsService.recordSessionFeedback(user, actionPlanId, 1, "activities", "bad", null, true)

    deliverySessionsService.submitAppointmentFeedback(actionPlanId, 1, session.referral.createdBy)
    verify(actionPlanAppointmentEventPublisher).attendanceRecordedEvent(session)
    verify(actionPlanAppointmentEventPublisher).sessionFeedbackRecordedEvent(session)
    verify(actionPlanAppointmentEventPublisher).sessionFeedbackRecordedEvent(session)
  }

  @Test
  fun `attendance can't be updated once appointment feedback has been submitted`() {
    val user = createActor()
    val session = deliverySessionFactory.createAttended()
    val actionPlanId = UUID.randomUUID()
    whenever(deliverySessionRepository.findAllByActionPlanIdAndSessionNumber(actionPlanId, 1)).thenReturn(session)

    assertThrows(ResponseStatusException::class.java) {
      deliverySessionsService.recordAttendanceFeedback(user, actionPlanId, 1, Attended.YES, "")
    }
  }

  @Test
  fun `session feedback can't be updated once appointment feedback has been submitted`() {
    val user = createActor()
    val session = deliverySessionFactory.createAttended()
    val actionPlanId = UUID.randomUUID()
    whenever(deliverySessionRepository.findAllByActionPlanIdAndSessionNumber(actionPlanId, 1)).thenReturn(session)

    assertThrows(ResponseStatusException::class.java) {
      deliverySessionsService.recordSessionFeedback(user, actionPlanId, 1, "activities", "bad", null, false)
    }
  }

  @Test
  fun `appointment feedback can be submitted when session not attended`() {
    val session = deliverySessionFactory.createScheduled(appointmentTime = OffsetDateTime.now())
    val actionPlanId = UUID.randomUUID()
    whenever(deliverySessionRepository.findAllByActionPlanIdAndSessionNumber(actionPlanId, 1)).thenReturn(
      session,
    )
    whenever(deliverySessionRepository.save(any())).thenReturn(session)

    val user = createActor()
    deliverySessionsService.recordAttendanceFeedback(user, actionPlanId, 1, Attended.NO, "")
    deliverySessionsService.submitAppointmentFeedback(actionPlanId, 1, user)

    verify(deliverySessionRepository, atLeastOnce()).save(session)
    verify(actionPlanAppointmentEventPublisher).attendanceRecordedEvent(session)
    verify(actionPlanAppointmentEventPublisher).appointmentFeedbackRecordedEvent(session)
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
