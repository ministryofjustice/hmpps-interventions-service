package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.atLeastOnce
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.firstValue
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.springframework.web.server.ResponseStatusException
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.AppointmentEventPublisher
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ActionPlanSession
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Attended
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.SampleData
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.SupplierAssessment
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ActionPlanRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ActionPlanSessionRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.AppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.AuthUserRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ReferralRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.SupplierAssessmentRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ActionPlanFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.AppointmentFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.AuthUserFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ReferralFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.SupplierAssessmentFactory
import java.time.OffsetDateTime
import java.util.Optional.of
import java.util.UUID
import javax.persistence.EntityExistsException
import javax.persistence.EntityNotFoundException

internal class AppointmentsServiceTest {

  private val actionPlanRepository: ActionPlanRepository = mock()
  private val actionPlanSessionRepository: ActionPlanSessionRepository = mock()
  private val authUserRepository: AuthUserRepository = mock()
  private val appointmentRepository: AppointmentRepository = mock()
  private val appointmentEventPublisher: AppointmentEventPublisher = mock()
  private val communityAPIBookingService: CommunityAPIBookingService = mock()
  private val actionPlanFactory = ActionPlanFactory()
  private val supplierAssessmentRepository: SupplierAssessmentRepository = mock()
  val referralRepository: ReferralRepository = mock()
  private val referralFactory = ReferralFactory()
  private val authUserFactory = AuthUserFactory()
  private val appointmentFactory = AppointmentFactory()
  private val supplierAssessmentFactory = SupplierAssessmentFactory()

  private val appointmentsService = AppointmentsService(
    actionPlanSessionRepository, actionPlanRepository,
    authUserRepository, appointmentRepository, appointmentEventPublisher,
    communityAPIBookingService, supplierAssessmentRepository, referralRepository
  )

  @Test
  fun `creates an appointment`() {
    val actionPlanId = UUID.randomUUID()
    val sessionNumber = 1
    val appointmentTime = OffsetDateTime.now()
    val durationInMinutes = 15
    val createdByUser = SampleData.sampleAuthUser()
    val actionPlan = SampleData.sampleActionPlan(id = actionPlanId)
    val appointment = SampleData.sampleAppointment(createdBy = createdByUser)

    whenever(actionPlanSessionRepository.findByActionPlanIdAndSessionNumber(actionPlanId, sessionNumber)).thenReturn(null)
    whenever(authUserRepository.save(createdByUser)).thenReturn(createdByUser)
    whenever(actionPlanRepository.findById(actionPlanId)).thenReturn(of(actionPlan))
    val savedAppointment = SampleData.sampleActionPlanSession(actionPlan = actionPlan, appointment = appointment)
    whenever(actionPlanSessionRepository.save(any())).thenReturn(savedAppointment)
    whenever(appointmentRepository.save(any())).thenReturn(appointment)

    val createdAppointment = appointmentsService.createAppointment(
      actionPlan,
      sessionNumber,
      appointmentTime,
      durationInMinutes,
      createdByUser
    )

    assertThat(createdAppointment).isEqualTo(savedAppointment)
  }

  @Test
  fun `create unscheduled appointments creates one for each action plan session`() {
    val actionPlan = actionPlanFactory.create(numberOfSessions = 3)
    whenever(actionPlanSessionRepository.findByActionPlanIdAndSessionNumber(eq(actionPlan.id), any())).thenReturn(null)
    whenever(authUserRepository.save(actionPlan.createdBy)).thenReturn(actionPlan.createdBy)
    whenever(actionPlanSessionRepository.save(any())).thenAnswer { it.arguments[0] }
    whenever(appointmentRepository.save(any())).thenReturn(SampleData.sampleAppointment())

    appointmentsService.createUnscheduledAppointmentsForActionPlan(actionPlan, actionPlan.createdBy)
    verify(actionPlanSessionRepository, times(3)).save(any())
  }

  @Test
  fun `create unscheduled appointments throws exception if session already exists`() {
    val actionPlan = actionPlanFactory.create(numberOfSessions = 1)
    whenever(actionPlanSessionRepository.findByActionPlanIdAndSessionNumber(actionPlan.id, 1))
      .thenReturn(
        SampleData.sampleActionPlanSession(
          actionPlan = actionPlan,
          appointment = SampleData.sampleAppointment(createdBy = actionPlan.createdBy)
        )
      )

    assertThrows(EntityExistsException::class.java) {
      appointmentsService.createUnscheduledAppointmentsForActionPlan(actionPlan, actionPlan.createdBy)
    }
  }

  @Test
  fun `updates an appointment`() {
    val actionPlanId = UUID.randomUUID()
    val sessionNumber = 1
    val appointmentTime = OffsetDateTime.now()
    val durationInMinutes = 15
    val createdByUser = SampleData.sampleAuthUser()
    val actionPlan = SampleData.sampleActionPlan()
    val actionPlanAppointment = SampleData.sampleActionPlanSession(actionPlan = actionPlan)

    whenever(actionPlanSessionRepository.findByActionPlanIdAndSessionNumber(actionPlanId, sessionNumber)).thenReturn(actionPlanAppointment)
    whenever(authUserRepository.save(createdByUser)).thenReturn(createdByUser)
    whenever(actionPlanSessionRepository.save(any())).thenReturn(actionPlanAppointment)

    val updatedAppointment = appointmentsService.updateActionPlanSessionAppointment(
      actionPlanId,
      sessionNumber,
      appointmentTime,
      durationInMinutes
    )

    assertThat(updatedAppointment).isEqualTo(actionPlanAppointment)
  }

  @Test
  fun `makes a booking when an appointment is updated`() {
    val actionPlanId = UUID.randomUUID()
    val sessionNumber = 1
    val appointmentTime = OffsetDateTime.now()
    val durationInMinutes = 15
    val createdByUser = SampleData.sampleAuthUser()
    val actionPlan = SampleData.sampleActionPlan()
    val actionPlanAppointment = SampleData.sampleActionPlanSession(actionPlan = actionPlan)

    whenever(communityAPIBookingService.book(actionPlanAppointment, appointmentTime, durationInMinutes)).thenReturn(999L)
    whenever(actionPlanSessionRepository.findByActionPlanIdAndSessionNumber(actionPlanId, sessionNumber)).thenReturn(actionPlanAppointment)
    whenever(authUserRepository.save(createdByUser)).thenReturn(createdByUser)
    whenever(actionPlanSessionRepository.save(any())).thenReturn(actionPlanAppointment)

    val updatedAppointment = appointmentsService.updateActionPlanSessionAppointment(
      actionPlanId,
      sessionNumber,
      appointmentTime,
      durationInMinutes
    )

    assertThat(updatedAppointment).isEqualTo(actionPlanAppointment)
    verify(communityAPIBookingService).book(actionPlanAppointment, appointmentTime, durationInMinutes)
    verify(actionPlanSessionRepository).save(
      ArgumentMatchers.argThat { (
        _, _, _, appointmentArg,
      ) ->
        (
          appointmentArg.first().deliusAppointmentId == 999L
          )
      }
    )
  }

  @Test
  fun `does not make a booking when an appointment is updated because timings aren't present`() {
    val actionPlanId = UUID.randomUUID()
    val sessionNumber = 1
    val appointmentTime = OffsetDateTime.now()
    val durationInMinutes = 15
    val createdByUser = SampleData.sampleAuthUser()
    val actionPlan = SampleData.sampleActionPlan()
    val actionPlanAppointment = SampleData.sampleActionPlanSession(actionPlan = actionPlan)

    whenever(communityAPIBookingService.book(actionPlanAppointment, appointmentTime, durationInMinutes)).thenReturn(null)
    whenever(actionPlanSessionRepository.findByActionPlanIdAndSessionNumber(actionPlanId, sessionNumber)).thenReturn(actionPlanAppointment)
    whenever(authUserRepository.save(createdByUser)).thenReturn(createdByUser)
    whenever(actionPlanSessionRepository.save(any())).thenReturn(actionPlanAppointment)

    appointmentsService.updateActionPlanSessionAppointment(
      actionPlanId,
      sessionNumber,
      appointmentTime,
      durationInMinutes
    )

    verify(actionPlanSessionRepository).save(
      ArgumentMatchers.argThat { (
        _, _, _, appointmentArg
      ) ->
        (
          appointmentArg.first().deliusAppointmentId == null
          )
      }
    )
  }

  @Test
  fun `updates an appointment and throws exception if it not exists`() {
    val actionPlanId = UUID.randomUUID()
    val sessionNumber = 1
    val appointmentTime = OffsetDateTime.now()
    val durationInMinutes = 15

    whenever(actionPlanSessionRepository.findByActionPlanIdAndSessionNumber(actionPlanId, sessionNumber)).thenReturn(null)

    val exception = assertThrows(EntityNotFoundException::class.java) {
      appointmentsService.updateActionPlanSessionAppointment(
        actionPlanId,
        sessionNumber,
        appointmentTime,
        durationInMinutes
      )
    }
    assertThat(exception.message).isEqualTo("Action plan appointment not found [id=$actionPlanId, sessionNumber=$sessionNumber]")
  }

  @Test
  fun `gets an appointment`() {
    val actionPlanId = UUID.randomUUID()
    val sessionNumber = 1
    val actionPlan = SampleData.sampleActionPlan()
    val actionPlanAppointment = SampleData.sampleActionPlanSession(actionPlan = actionPlan)

    whenever(actionPlanSessionRepository.findByActionPlanIdAndSessionNumber(actionPlanId, sessionNumber)).thenReturn(actionPlanAppointment)

    val actualAppointment = appointmentsService.getActionPlanSessionAppointment(actionPlanId, sessionNumber)

    assertThat(actualAppointment.sessionNumber).isEqualTo(actionPlanAppointment.sessionNumber)
    assertThat(actualAppointment.appointment.appointmentTime).isEqualTo(actionPlanAppointment.appointment.appointmentTime)
    assertThat(actualAppointment.appointment.durationInMinutes).isEqualTo(actionPlanAppointment.appointment.durationInMinutes)
  }

  @Test
  fun `gets an appointment and throws exception if it not exists`() {
    val actionPlanId = UUID.randomUUID()
    val sessionNumber = 1

    whenever(actionPlanSessionRepository.findByActionPlanIdAndSessionNumber(actionPlanId, sessionNumber)).thenReturn(null)

    val exception = assertThrows(EntityNotFoundException::class.java) {
      appointmentsService.getActionPlanSessionAppointment(actionPlanId, sessionNumber)
    }
    assertThat(exception.message).isEqualTo("Action plan appointment not found [id=$actionPlanId, sessionNumber=$sessionNumber]")
  }

  @Test
  fun `gets all appointments for an action plan`() {
    val actionPlanId = UUID.randomUUID()
    val actionPlan = SampleData.sampleActionPlan()
    val actionPlanAppointment = SampleData.sampleActionPlanSession(actionPlan = actionPlan)

    whenever(actionPlanSessionRepository.findAllByActionPlanId(actionPlanId)).thenReturn(listOf(actionPlanAppointment))

    val appointments = appointmentsService.getActionPlanSessionAppointments(actionPlanId)

    assertThat(appointments.first().sessionNumber).isEqualTo(actionPlanAppointment.sessionNumber)
    assertThat(appointments.first().appointment.appointmentTime).isEqualTo(actionPlanAppointment.appointment.appointmentTime)
    assertThat(appointments.first().appointment.durationInMinutes).isEqualTo(actionPlanAppointment.appointment.durationInMinutes)
  }

  @Test
  fun `update appointment with attendance`() {
    val appointmentId = UUID.randomUUID()
    val sessionNumber = 1
    val attended = Attended.YES
    val additionalInformation = "extra info"
    val actionPlan = SampleData.sampleActionPlan()

    val existingAppointment = SampleData.sampleActionPlanSession(actionPlan = actionPlan)

    whenever(actionPlanSessionRepository.findByActionPlanIdAndSessionNumber(appointmentId, sessionNumber))
      .thenReturn(existingAppointment)
    whenever(actionPlanSessionRepository.save(any())).thenReturn(existingAppointment)

    val savedAppointment = appointmentsService.recordActionPlanSessionAttendance(appointmentId, 1, attended, additionalInformation)
    val argumentCaptor: ArgumentCaptor<ActionPlanSession> = ArgumentCaptor.forClass(ActionPlanSession::class.java)

//    verify(appointmentEventPublisher).appointmentNotAttendedEvent(existingAppointment)
    verify(actionPlanSessionRepository).save(argumentCaptor.capture())
    assertThat(argumentCaptor.firstValue.appointment.attended).isEqualTo(attended)
    assertThat(argumentCaptor.firstValue.appointment.additionalAttendanceInformation).isEqualTo(additionalInformation)
    assertThat(argumentCaptor.firstValue.appointment.attendanceSubmittedAt).isNotNull
    assertThat(savedAppointment).isNotNull
  }

  @Test
  fun `update appointment with attendance - no appointment found`() {
    val actionPlanId = UUID.randomUUID()
    val sessionNumber = 1
    val attended = Attended.YES
    val additionalInformation = "extra info"

    whenever(actionPlanSessionRepository.findByActionPlanIdAndSessionNumber(actionPlanId, sessionNumber))
      .thenReturn(null)

    val exception = assertThrows(EntityNotFoundException::class.java) {
      appointmentsService.recordActionPlanSessionAttendance(actionPlanId, 1, attended, additionalInformation)
    }

    assertThat(exception.message).isEqualTo("Action plan appointment not found [id=$actionPlanId, sessionNumber=$sessionNumber]")
  }

  @Test
  fun `updating session behaviour sets relevant fields`() {
    val actionPlan = SampleData.sampleActionPlan()
    val actionPlanSession = SampleData.sampleActionPlanSession(
      actionPlan = actionPlan,
      appointment = SampleData.sampleAppointment(createdBy = actionPlan.createdBy)
    )
    whenever(actionPlanSessionRepository.findByActionPlanIdAndSessionNumber(any(), any())).thenReturn(actionPlanSession)
    whenever(actionPlanSessionRepository.save(any())).thenReturn(actionPlanSession)
    val updatedAppointment = appointmentsService.recordActionPlanSessionBehaviour(actionPlanSession.id, 1, "not good", false)

    verify(actionPlanSessionRepository, times(1)).save(actionPlanSession)
    assertThat(updatedAppointment).isSameAs(actionPlanSession)
    assertThat(actionPlanSession.appointment.attendanceBehaviour).isEqualTo("not good")
    assertThat(actionPlanSession.appointment.notifyPPOfAttendanceBehaviour).isFalse
    assertThat(actionPlanSession.appointment.attendanceBehaviourSubmittedAt).isNotNull
  }

  @Test
  fun `updating session behaviour for missing appointment throws error`() {
    whenever(actionPlanSessionRepository.findByActionPlanIdAndSessionNumber(any(), any())).thenReturn(null)

    assertThrows(EntityNotFoundException::class.java) {
      appointmentsService.recordActionPlanSessionBehaviour(UUID.randomUUID(), 1, "not good", false)
    }
  }

  @Test
  fun `session feedback cant be submitted more than once`() {
    val actionPlan = SampleData.sampleActionPlan()
    val actionPlanSession = SampleData.sampleActionPlanSession(
      actionPlan = actionPlan,
      appointment = SampleData.sampleAppointment(createdBy = actionPlan.createdBy)
    )
    whenever(actionPlanSessionRepository.findByActionPlanIdAndSessionNumber(actionPlan.id, 1)).thenReturn(actionPlanSession)
    whenever(actionPlanSessionRepository.save(any())).thenReturn(actionPlanSession)

    appointmentsService.recordActionPlanSessionAttendance(actionPlan.id, 1, Attended.YES, "")
    appointmentsService.recordActionPlanSessionBehaviour(actionPlan.id, 1, "bad", false)
    actionPlanSession.appointment.sessionFeedbackSubmittedAt = OffsetDateTime.now()

    assertThrows(ResponseStatusException::class.java) {
      appointmentsService.submitActionPlanSessionFeedback(actionPlan.id, 1)
    }
  }

  @Test
  fun `session feedback cant be submitted if attendance is missing`() {
    val actionPlan = SampleData.sampleActionPlan()
    val actionPlanSession = SampleData.sampleActionPlanSession(
      actionPlan = actionPlan,
      appointment = SampleData.sampleAppointment(createdBy = actionPlan.createdBy)
    )
    whenever(actionPlanSessionRepository.findByActionPlanIdAndSessionNumber(actionPlan.id, 1)).thenReturn(actionPlanSession)
    whenever(actionPlanSessionRepository.save(any())).thenReturn(actionPlanSession)

    appointmentsService.recordActionPlanSessionBehaviour(actionPlan.id, 1, "bad", false)

    assertThrows(ResponseStatusException::class.java) {
      appointmentsService.submitActionPlanSessionFeedback(actionPlan.id, 1)
    }
  }

  @Test
  fun `session feedback can be submitted and stores timestamp and emits application events`() {
    val actionPlan = SampleData.sampleActionPlan()
    val actionPlanSession = SampleData.sampleActionPlanSession(
      actionPlan = actionPlan,
      appointment = SampleData.sampleAppointment(createdBy = actionPlan.createdBy)
    )
    whenever(actionPlanSessionRepository.findByActionPlanIdAndSessionNumber(actionPlan.id, 1)).thenReturn(
      actionPlanSession
    )
    whenever(actionPlanSessionRepository.save(any())).thenReturn(actionPlanSession)

    appointmentsService.recordActionPlanSessionAttendance(actionPlan.id, 1, Attended.YES, "")
    appointmentsService.recordActionPlanSessionBehaviour(actionPlan.id, 1, "bad", true)
    appointmentsService.submitActionPlanSessionFeedback(actionPlan.id, 1)

    val appointmentCaptor = argumentCaptor<ActionPlanSession>()
    verify(actionPlanSessionRepository, atLeastOnce()).save(appointmentCaptor.capture())
    appointmentCaptor.allValues.forEach {
      if (it == appointmentCaptor.lastValue) {
        assertThat(it.appointment.sessionFeedbackSubmittedAt != null)
      } else {
        assertThat(it.appointment.sessionFeedbackSubmittedAt == null)
      }
    }

    verify(appointmentEventPublisher).attendanceRecordedEvent(actionPlanSession, false)
    verify(appointmentEventPublisher).behaviourRecordedEvent(actionPlanSession, true)
    verify(appointmentEventPublisher).sessionFeedbackRecordedEvent(actionPlanSession, true)
  }

  @Test
  fun `attendance can't be updated once session feedback has been submitted`() {
    val actionPlan = SampleData.sampleActionPlan()
    val actionPlanSession = SampleData.sampleActionPlanSession(
      actionPlan = actionPlan,
      appointment = SampleData.sampleAppointment(createdBy = actionPlan.createdBy)
    )
    actionPlanSession.appointment.sessionFeedbackSubmittedAt = OffsetDateTime.now()
    whenever(actionPlanSessionRepository.findByActionPlanIdAndSessionNumber(actionPlan.id, 1)).thenReturn(actionPlanSession)

    assertThrows(ResponseStatusException::class.java) {
      appointmentsService.recordActionPlanSessionAttendance(actionPlan.id, 1, Attended.YES, "")
    }
  }

  @Test
  fun `behaviour can't be updated once session feedback has been submitted`() {
    val actionPlan = SampleData.sampleActionPlan()
    val actionPlanSession = SampleData.sampleActionPlanSession(
      actionPlan = actionPlan,
      appointment = SampleData.sampleAppointment(createdBy = actionPlan.createdBy)
    )
    actionPlanSession.appointment.sessionFeedbackSubmittedAt = OffsetDateTime.now()
    whenever(actionPlanSessionRepository.findByActionPlanIdAndSessionNumber(actionPlan.id, 1)).thenReturn(actionPlanSession)

    assertThrows(ResponseStatusException::class.java) {
      appointmentsService.recordActionPlanSessionBehaviour(actionPlan.id, 1, "bad", false)
    }
  }

  @Test
  fun `initial assessment can be created`() {
    val referral = referralFactory.createDraft()
    val createdByUser = authUserFactory.create()

    whenever(authUserRepository.save(any())).thenReturn(createdByUser)
    whenever(appointmentRepository.save(any())).thenReturn(appointmentFactory.create())
    whenever(supplierAssessmentRepository.save(any())).thenReturn(supplierAssessmentFactory.create())
    whenever(referralRepository.save(any())).thenReturn(referral)

    appointmentsService.createInitialAssessment(referral, createdByUser)

    val argumentCaptor = argumentCaptor<SupplierAssessment>()
    verify(supplierAssessmentRepository, atLeastOnce()).save(argumentCaptor.capture())
    val arguments = argumentCaptor.firstValue

    assertThat(arguments.referral).isEqualTo(referral)
    assertThat(arguments.appointments.size).isEqualTo(1)
    assertThat(arguments.appointment.createdBy).isEqualTo(createdByUser)
  }

  @Test
  fun `initial assessment appointment can be updated`() {
    val referral = referralFactory.createSent()
    referral.supplierAssessment = supplierAssessmentFactory.create()
    val durationInMinutes = 60
    val appointmentTime = OffsetDateTime.parse("2020-12-04T10:42:43+00:00")

    whenever(supplierAssessmentRepository.save(any())).thenReturn(supplierAssessmentFactory.create())

    appointmentsService.updateInitialAssessment(referral, durationInMinutes, appointmentTime)

    val argumentCaptor = argumentCaptor<SupplierAssessment>()
    verify(supplierAssessmentRepository, atLeastOnce()).save(argumentCaptor.capture())
    val arguments = argumentCaptor.firstValue

    assertThat(arguments.appointments.size).isEqualTo(1)
    assertThat(arguments.appointment.durationInMinutes).isEqualTo(durationInMinutes)
    assertThat(arguments.appointment.appointmentTime).isEqualTo(appointmentTime)
  }
}
