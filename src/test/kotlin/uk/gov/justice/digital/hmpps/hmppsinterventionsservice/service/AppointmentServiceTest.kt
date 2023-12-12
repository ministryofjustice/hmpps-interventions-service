package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.atLeast
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.web.server.ResponseStatusException
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.AppointmentEventPublisher
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Appointment
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AppointmentDelivery
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AppointmentDeliveryType
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AppointmentSessionType
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AppointmentType
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AppointmentType.SUPPLIER_ASSESSMENT
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Attended.NO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Attended.YES
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.NoSessionReasonType
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.AppointmentDeliveryRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.AppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.AuthUserRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ReferralRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.AppointmentFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.AuthUserFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ReferralFactory
import java.time.OffsetDateTime
import java.util.UUID

class AppointmentServiceTest {
  private val authUserRepository: AuthUserRepository = mock()
  private val appointmentRepository: AppointmentRepository = mock()
  private val communityAPIBookingService: CommunityAPIBookingService = mock()
  private val appointmentDeliveryRepository: AppointmentDeliveryRepository = mock()
  private val appointmentEventPublisher: AppointmentEventPublisher = mock()
  private val referralRepository: ReferralRepository = mock()

  private val authUserFactory = AuthUserFactory()
  private val appointmentFactory = AppointmentFactory()
  private val referralFactory = ReferralFactory()

  private val createdByUser = authUserFactory.create()

  private val appointmentService = AppointmentService(
    appointmentRepository,
    communityAPIBookingService,
    appointmentDeliveryRepository,
    authUserRepository,
    appointmentEventPublisher,
    referralRepository,
  )

  @BeforeEach
  fun beforeEach() {
    whenever(authUserRepository.save(any())).thenReturn(createdByUser)
  }

  @Test
  fun `can create new appointment with delius office location delivery`() {
    // Given
    val durationInMinutes = 60
    val appointmentTime = OffsetDateTime.now().plusHours(1)
    val referral = referralFactory.createSent()
    val deliusAppointmentId = 99L
    val npsOfficeCode = "CRSEXT"

    whenever(communityAPIBookingService.book(referral, null, appointmentTime, durationInMinutes, SUPPLIER_ASSESSMENT, npsOfficeCode))
      .thenReturn(Pair(deliusAppointmentId, UUID.randomUUID()))
    whenever(appointmentRepository.save(any())).thenAnswer { it.arguments[0] }

    // When
    val newAppointment = appointmentService.createOrUpdateAppointment(referral, null, durationInMinutes, appointmentTime, SUPPLIER_ASSESSMENT, createdByUser, AppointmentDeliveryType.IN_PERSON_MEETING_PROBATION_OFFICE, AppointmentSessionType.ONE_TO_ONE, npsOfficeCode = npsOfficeCode)

    // Then
    verifyResponse(
      newAppointment,
      null,
      deliusAppointmentId,
      appointmentTime,
      durationInMinutes,
      AppointmentDeliveryType.IN_PERSON_MEETING_PROBATION_OFFICE,
      AppointmentSessionType.ONE_TO_ONE,
      npsOfficeCode,
    )
    verifySavedAppointment(appointmentTime, durationInMinutes, deliusAppointmentId, AppointmentDeliveryType.IN_PERSON_MEETING_PROBATION_OFFICE, AppointmentSessionType.ONE_TO_ONE, npsOfficeCode)
  }

  @Test
  fun `create new appointment if none currently exist`() {
    // Given
    val durationInMinutes = 60
    val appointmentTime = OffsetDateTime.parse("2022-12-04T10:42:43+00:00")
    val referral = referralFactory.createSent()
    val deliusAppointmentId = 99L

    whenever(communityAPIBookingService.book(referral, null, appointmentTime, durationInMinutes, SUPPLIER_ASSESSMENT, null))
      .thenReturn(Pair(deliusAppointmentId, UUID.randomUUID()))
    whenever(appointmentRepository.save(any())).thenAnswer { it.arguments[0] }

    // When
    val newAppointment = appointmentService.createOrUpdateAppointment(referral, null, durationInMinutes, appointmentTime, SUPPLIER_ASSESSMENT, createdByUser, AppointmentDeliveryType.PHONE_CALL, AppointmentSessionType.ONE_TO_ONE)

    // Then
    verifyResponse(
      newAppointment,
      null,
      deliusAppointmentId,
      appointmentTime,
      durationInMinutes,
      AppointmentDeliveryType.PHONE_CALL,
      AppointmentSessionType.ONE_TO_ONE,
    )
    verifySavedAppointment(appointmentTime, durationInMinutes, deliusAppointmentId, AppointmentDeliveryType.PHONE_CALL, AppointmentSessionType.ONE_TO_ONE)
    assertThat(newAppointment.attended).isNull()
    assertThat(newAppointment.additionalAttendanceInformation).isNull()
    assertThat(newAppointment.attendanceBehaviour).isNull()
    assertThat(newAppointment.notifyPPOfAttendanceBehaviour).isNull()
    assertThat(newAppointment.attendanceSubmittedAt).isNull()
    assertThat(newAppointment.attendanceSubmittedBy).isNull()
    assertThat(newAppointment.attendanceBehaviourSubmittedAt).isNull()
    assertThat(newAppointment.attendanceBehaviourSubmittedBy).isNull()
    assertThat(newAppointment.appointmentFeedbackSubmittedAt).isNull()
    assertThat(newAppointment.appointmentFeedbackSubmittedBy).isNull()
  }

  @Test
  fun `create a historic appointment`() {
    // Given
    val durationInMinutes = 60
    val appointmentTime = OffsetDateTime.parse("2020-12-04T10:42:43+00:00")
    val referral = referralFactory.createSent()
    val deliusAppointmentId = 99L

    whenever(communityAPIBookingService.book(referral, null, appointmentTime, durationInMinutes, SUPPLIER_ASSESSMENT, null, YES, false, false, NoSessionReasonType.LOGISTICS))
      .thenReturn(Pair(deliusAppointmentId, UUID.randomUUID()))
    whenever(appointmentRepository.save(any())).thenAnswer { it.arguments[0] }

    // When
    val newAppointment = appointmentService.createOrUpdateAppointment(
      referral,
      null,
      durationInMinutes,
      appointmentTime,
      SUPPLIER_ASSESSMENT,
      createdByUser,
      AppointmentDeliveryType.PHONE_CALL,
      AppointmentSessionType.ONE_TO_ONE,
      null,
      null,
      attended = YES,
      notifyProbationPractitioner = false,
      didSessionHappen = false,
      late = false,
      sessionSummary = "session summary",
      sessionResponse = "session response",
      noSessionReasonType = NoSessionReasonType.LOGISTICS,
    )

    // Then
    verifyResponse(
      newAppointment,
      null,
      deliusAppointmentId,
      appointmentTime,
      durationInMinutes,
      AppointmentDeliveryType.PHONE_CALL,
      AppointmentSessionType.ONE_TO_ONE,
    )
    verifySavedAppointment(appointmentTime, durationInMinutes, deliusAppointmentId, AppointmentDeliveryType.PHONE_CALL, AppointmentSessionType.ONE_TO_ONE)
    verify(appointmentEventPublisher).attendanceRecordedEvent(newAppointment, false, AppointmentType.SUPPLIER_ASSESSMENT)
    verify(appointmentEventPublisher).sessionFeedbackRecordedEvent(newAppointment, false, AppointmentType.SUPPLIER_ASSESSMENT)
    verify(appointmentEventPublisher).appointmentFeedbackRecordedEvent(newAppointment, false, AppointmentType.SUPPLIER_ASSESSMENT)
    assertThat(newAppointment.attended).isEqualTo(YES)
    assertThat(newAppointment.noAttendanceInformation).isEqualTo(null)
    assertThat(newAppointment.sessionSummary).isEqualTo("session summary")
    assertThat(newAppointment.sessionResponse).isEqualTo("session response")
    assertThat(newAppointment.notifyPPOfAttendanceBehaviour).isEqualTo(false)
    assertThat(newAppointment.attendanceSubmittedAt).isNotNull
    assertThat(newAppointment.attendanceSubmittedBy).isEqualTo(createdByUser)
    assertThat(newAppointment.sessionFeedbackSubmittedAt).isNotNull
    assertThat(newAppointment.sessionFeedbackSubmittedBy).isEqualTo(createdByUser)
    assertThat(newAppointment.appointmentFeedbackSubmittedAt).isNotNull
    assertThat(newAppointment.appointmentFeedbackSubmittedBy).isEqualTo(createdByUser)
  }

  @Test
  fun `appointment without attendance data can be updated`() {
    // Given
    val durationInMinutes = 60
    val appointmentTime = OffsetDateTime.parse("2020-12-04T10:42:43+00:00")
    val existingAppointment = appointmentFactory.create(deliusAppointmentId = 98L, attended = null)
    val referral = referralFactory.createSent()
    val rescheduledDeliusAppointmentId = 99L

    whenever(communityAPIBookingService.book(referral, existingAppointment, appointmentTime, durationInMinutes, SUPPLIER_ASSESSMENT, null))
      .thenReturn(Pair(rescheduledDeliusAppointmentId, UUID.randomUUID()))

    whenever(appointmentRepository.saveAndFlush(any())).thenAnswer { it.arguments[0] }

    // When
    val updatedAppointment = appointmentService.createOrUpdateAppointment(referral, existingAppointment, durationInMinutes, appointmentTime, SUPPLIER_ASSESSMENT, createdByUser, AppointmentDeliveryType.PHONE_CALL, AppointmentSessionType.ONE_TO_ONE)

    // Then
    verifyResponse(
      updatedAppointment,
      existingAppointment.id,
      rescheduledDeliusAppointmentId,
      appointmentTime,
      durationInMinutes,
      AppointmentDeliveryType.PHONE_CALL,
      AppointmentSessionType.ONE_TO_ONE,
    )
    verifySavedAppointment(appointmentTime, durationInMinutes, rescheduledDeliusAppointmentId, AppointmentDeliveryType.PHONE_CALL, AppointmentSessionType.ONE_TO_ONE)
    val argumentCaptor = argumentCaptor<Appointment>()
    verify(appointmentRepository, times(2)).save(argumentCaptor.capture())
    val oldAppointmentArguments = argumentCaptor.firstValue
    assertThat(oldAppointmentArguments.superseded).isTrue
    assertThat(updatedAppointment.attended).isNull()
    assertThat(updatedAppointment.additionalAttendanceInformation).isNull()
    assertThat(updatedAppointment.attendanceBehaviour).isNull()
    assertThat(updatedAppointment.notifyPPOfAttendanceBehaviour).isNull()
    assertThat(updatedAppointment.attendanceSubmittedAt).isNull()
    assertThat(updatedAppointment.attendanceSubmittedBy).isNull()
    assertThat(updatedAppointment.attendanceBehaviourSubmittedAt).isNull()
    assertThat(updatedAppointment.attendanceBehaviourSubmittedBy).isNull()
    assertThat(updatedAppointment.appointmentFeedbackSubmittedAt).isNull()
    assertThat(updatedAppointment.appointmentFeedbackSubmittedBy).isNull()
  }

  @Test
  fun `appointment with none attendance will create a new appointment`() {
    // Given
    val durationInMinutes = 60
    val appointmentTime = OffsetDateTime.parse("2022-12-04T10:42:43+00:00")
    val existingAppointment = appointmentFactory.create(deliusAppointmentId = 98L, didSessionHappen = false)
    val referral = referralFactory.createSent()
    val additionalDeliusAppointmentId = 99L

    whenever(communityAPIBookingService.book(referral, null, appointmentTime, durationInMinutes, SUPPLIER_ASSESSMENT, null))
      .thenReturn(Pair(additionalDeliusAppointmentId, UUID.randomUUID()))
    whenever(appointmentRepository.save(any())).thenAnswer { it.arguments[0] }

    // When
    val newAppointment = appointmentService.createOrUpdateAppointment(referral, existingAppointment, durationInMinutes, appointmentTime, SUPPLIER_ASSESSMENT, createdByUser, AppointmentDeliveryType.PHONE_CALL, AppointmentSessionType.ONE_TO_ONE)

    // Then
    verifyResponse(
      newAppointment,
      existingAppointment.id,
      additionalDeliusAppointmentId,
      appointmentTime,
      durationInMinutes,
      AppointmentDeliveryType.PHONE_CALL,
      AppointmentSessionType.ONE_TO_ONE,
    )
    verifySavedAppointment(appointmentTime, durationInMinutes, additionalDeliusAppointmentId, AppointmentDeliveryType.PHONE_CALL, AppointmentSessionType.ONE_TO_ONE)
    assertThat(newAppointment.attended).isNull()
    assertThat(newAppointment.additionalAttendanceInformation).isNull()
    assertThat(newAppointment.attendanceBehaviour).isNull()
    assertThat(newAppointment.notifyPPOfAttendanceBehaviour).isNull()
    assertThat(newAppointment.attendanceSubmittedAt).isNull()
    assertThat(newAppointment.attendanceSubmittedBy).isNull()
    assertThat(newAppointment.attendanceBehaviourSubmittedAt).isNull()
    assertThat(newAppointment.attendanceBehaviourSubmittedBy).isNull()
    assertThat(newAppointment.appointmentFeedbackSubmittedAt).isNull()
    assertThat(newAppointment.appointmentFeedbackSubmittedBy).isNull()
  }

  @Test
  fun `future appointment can be updated with a past appointment containing feedback`() {
    // Given
    val durationInMinutes = 60
    val pastAppointmentTime = OffsetDateTime.parse("2020-12-04T10:42:43+00:00")
    val existingAppointment = appointmentFactory.create(deliusAppointmentId = 98L, attended = null)
    val referral = referralFactory.createSent()
    val rescheduledDeliusAppointmentId = 99L

    whenever(communityAPIBookingService.book(referral, existingAppointment, pastAppointmentTime, durationInMinutes, SUPPLIER_ASSESSMENT, null, NO, false, false, NoSessionReasonType.POP_ACCEPTABLE))
      .thenReturn(Pair(rescheduledDeliusAppointmentId, UUID.randomUUID()))
    whenever(appointmentRepository.save(any())).thenAnswer { it.arguments[0] }

    // When
    val updatedAppointment = appointmentService.createOrUpdateAppointment(
      referral,
      existingAppointment,
      durationInMinutes,
      pastAppointmentTime,
      SUPPLIER_ASSESSMENT,
      createdByUser,
      AppointmentDeliveryType.PHONE_CALL,
      AppointmentSessionType.ONE_TO_ONE,
      attended = NO,
      notifyProbationPractitioner = false,
      didSessionHappen = false,
      noSessionReasonType = NoSessionReasonType.POP_ACCEPTABLE,
      late = false,
    )

    // Then
    verifyResponse(
      updatedAppointment,
      existingAppointment.id,
      rescheduledDeliusAppointmentId,
      OffsetDateTime.parse("2020-12-04T10:42:43+00:00"),
      durationInMinutes,
      AppointmentDeliveryType.PHONE_CALL,
      AppointmentSessionType.ONE_TO_ONE,
    )
    verifySavedAppointment(OffsetDateTime.parse("2020-12-04T10:42:43+00:00"), durationInMinutes, rescheduledDeliusAppointmentId, AppointmentDeliveryType.PHONE_CALL, AppointmentSessionType.ONE_TO_ONE)
    assertThat(updatedAppointment.attended).isEqualTo(NO)
    assertThat(updatedAppointment.didSessionHappen).isEqualTo(false)
    assertThat(updatedAppointment.noAttendanceInformation).isEqualTo(null)
    assertThat(updatedAppointment.attendanceBehaviour).isNull()
    assertThat(updatedAppointment.notifyPPOfAttendanceBehaviour).isFalse
  }

  @Test
  fun `any other scenario will throw an error`() {
    val durationInMinutes = 60
    val appointmentTime = OffsetDateTime.parse("2020-12-04T10:42:43+00:00")
    val createdByUser = authUserFactory.create()
    val appointment = appointmentFactory.create(didSessionHappen = true)
    val referral = referralFactory.createSent()

    val error = assertThrows<IllegalStateException> {
      appointmentService.createOrUpdateAppointment(referral, appointment, durationInMinutes, appointmentTime, SUPPLIER_ASSESSMENT, createdByUser, AppointmentDeliveryType.PHONE_CALL, AppointmentSessionType.ONE_TO_ONE)
    }
    assertThat(error.message).contains("It is not possible to update an appointment where a session has already happened.")
  }

  private fun verifyResponse(
    appointment: Appointment,
    originalId: UUID?,
    deliusAppointmentId: Long,
    appointmentTime: OffsetDateTime?,
    durationInMinutes: Int,
    appointmentDeliveryType: AppointmentDeliveryType,
    appointmentSessionType: AppointmentSessionType,
    npsOfficeCode: String? = null,
  ) {
    // Verifying create or update route
    assertThat(appointment).isNotEqualTo(originalId)

    assertThat(appointment.deliusAppointmentId).isEqualTo(deliusAppointmentId)
    assertThat(appointment.appointmentTime).isEqualTo(appointmentTime)
    assertThat(appointment.durationInMinutes).isEqualTo(durationInMinutes)
    assertThat(appointment.appointmentDelivery?.appointmentDeliveryType).isEqualTo(appointmentDeliveryType)
    assertThat(appointment.appointmentDelivery?.appointmentSessionType).isEqualTo(appointmentSessionType)
    assertThat(appointment.appointmentDelivery?.npsOfficeCode).isEqualTo(npsOfficeCode)
  }

  private fun verifySavedAppointment(appointmentTime: OffsetDateTime, durationInMinutes: Int, deliusAppointmentId: Long, appointmentDeliveryType: AppointmentDeliveryType, appointmentSessionType: AppointmentSessionType, npsOfficeCode: String? = null) {
    val argumentCaptor = argumentCaptor<Appointment>()
    verify(appointmentRepository, atLeast(1)).saveAndFlush(argumentCaptor.capture())
    val arguments = argumentCaptor.lastValue

    assertThat(arguments.id).isNotNull
    assertThat(arguments.appointmentTime).isEqualTo(appointmentTime)
    assertThat(arguments.durationInMinutes).isEqualTo(durationInMinutes)
    assertThat(arguments.deliusAppointmentId).isEqualTo(deliusAppointmentId)
    assertThat(arguments.appointmentDelivery?.appointmentDeliveryType).isEqualTo(appointmentDeliveryType)
    assertThat(arguments.appointmentDelivery?.appointmentSessionType).isEqualTo(appointmentSessionType)
    assertThat(arguments.appointmentDelivery?.npsOfficeCode).isEqualTo(npsOfficeCode)
  }

  @Nested
  inner class UpdateAppointmentDeliveryAddress {
    @Test
    fun `overriding appointment delivery sets new values for npsOfficeCode`() {
      // Given
      val durationInMinutes = 60
      val appointmentTime = OffsetDateTime.parse("2020-12-04T10:42:43+00:00")
      val existingAppointment = appointmentFactory.create(deliusAppointmentId = 98L, attended = null)
      val referral = referralFactory.createSent()
      val rescheduledDeliusAppointmentId = 99L
      val oldNpsCode = "CRS0001"
      val newNpsCode = "CRS0002"
      val savedAppointment = appointmentFactory.create(
        existingAppointment.id,
        appointmentTime = appointmentTime,
        durationInMinutes = durationInMinutes,
        deliusAppointmentId = rescheduledDeliusAppointmentId,
      )

      whenever(communityAPIBookingService.book(referral, existingAppointment, appointmentTime, durationInMinutes, SUPPLIER_ASSESSMENT, newNpsCode))
        .thenReturn(Pair(rescheduledDeliusAppointmentId, existingAppointment.id))
      savedAppointment.appointmentDelivery = AppointmentDelivery(appointmentId = savedAppointment.id, appointmentDeliveryType = AppointmentDeliveryType.IN_PERSON_MEETING_PROBATION_OFFICE, AppointmentSessionType.ONE_TO_ONE, npsOfficeCode = oldNpsCode)

      whenever(appointmentRepository.save(any())).thenReturn(savedAppointment)

      // When
      val updatedAppointment = appointmentService.createOrUpdateAppointment(referral, existingAppointment, durationInMinutes, appointmentTime, SUPPLIER_ASSESSMENT, createdByUser, AppointmentDeliveryType.IN_PERSON_MEETING_PROBATION_OFFICE, AppointmentSessionType.ONE_TO_ONE, npsOfficeCode = newNpsCode)

      // Then
      assertThat(existingAppointment.superseded).isTrue
      verifyResponse(
        updatedAppointment,
        existingAppointment.id,
        rescheduledDeliusAppointmentId,
        appointmentTime,
        durationInMinutes,
        AppointmentDeliveryType.IN_PERSON_MEETING_PROBATION_OFFICE,
        AppointmentSessionType.ONE_TO_ONE,
        newNpsCode,
      )
      verifySavedAppointment(appointmentTime, durationInMinutes, rescheduledDeliusAppointmentId, AppointmentDeliveryType.IN_PERSON_MEETING_PROBATION_OFFICE, AppointmentSessionType.ONE_TO_ONE, newNpsCode)
    }
  }

  @Nested
  inner class RecordBehaviour {
    @Test
    fun `appointment behaviour can be updated`() {
      val appointmentId = UUID.randomUUID()
      val sessionSummary = "summary"
      val sessionResponse = "response"
      val notifyProbationPractitioner = true
      val appointment = appointmentFactory.create(id = appointmentId)
      val submittedBy = authUserFactory.create()

      whenever(appointmentRepository.save(any())).thenReturn(appointment)
      whenever(authUserRepository.save(any())).thenReturn(submittedBy)

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
        sessionSummary,
        sessionResponse,
        null,
        notifyProbationPractitioner,
        submittedBy,
      )

      val argumentCaptor = argumentCaptor<Appointment>()
      verify(appointmentRepository, times(1)).save(argumentCaptor.capture())
      val arguments = argumentCaptor.firstValue

      assertThat(arguments.id).isEqualTo(appointmentId)
      assertThat(arguments.sessionSummary).isEqualTo(sessionSummary)
      assertThat(arguments.notifyPPOfAttendanceBehaviour).isEqualTo(notifyProbationPractitioner)
      assertThat(arguments.sessionFeedbackSubmittedAt).isNotNull
      assertThat(arguments.sessionFeedbackSubmittedBy).isEqualTo(submittedBy)
    }

    @Test
    fun `appointment behaviour cannot be updated if feedback has been submitted`() {
      val appointmentId = UUID.randomUUID()
      val sessionSummary = "summary"
      val sessionResponse = "response"
      val notifyProbationPractitioner = true
      val submittedBy = authUserFactory.create()
      val feedbackSubmittedAt = OffsetDateTime.parse("2020-12-04T10:42:43+00:00")
      val appointment = appointmentFactory.create(id = appointmentId, appointmentFeedbackSubmittedAt = feedbackSubmittedAt)

      val error = assertThrows<ResponseStatusException> {
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
          sessionSummary,
          sessionResponse,
          null,
          notifyProbationPractitioner,
          submittedBy,
        )
      }
      assertThat(error.message).contains("Feedback has already been submitted for this appointment [id=$appointmentId]")
    }
  }

  @Nested
  inner class RecordAppointmentAttendance {
    @Test
    fun `appointment attendance can be updated`() {
      val appointmentId = UUID.randomUUID()
      val attended = YES
      val appointment = appointmentFactory.create(id = appointmentId, appointmentTime = OffsetDateTime.now())
      val submittedBy = authUserFactory.create()

      whenever(appointmentRepository.save(any())).thenReturn(appointment)
      whenever(authUserRepository.save(any())).thenReturn(submittedBy)

      appointmentService.recordAppointmentAttendance(appointment, attended, true, submittedBy)

      val argumentCaptor = argumentCaptor<Appointment>()
      verify(appointmentRepository, times(1)).save(argumentCaptor.capture())
      val arguments = argumentCaptor.firstValue

      assertThat(arguments.id).isEqualTo(appointmentId)
      assertThat(arguments.attended).isEqualTo(attended)
      assertThat(arguments.attendanceFailureInformation).isEqualTo(null)
      assertThat(arguments.attendanceSubmittedAt).isNotNull
      assertThat(arguments.attendanceSubmittedBy).isEqualTo(submittedBy)
    }

    @Test
    fun `previously saved session feedback fields are cleared when recording attendance and didSessionHappen value has changed`() {
      val appointmentId = UUID.randomUUID()
      val attended = YES
      val appointment = appointmentFactory.create(
        id = appointmentId,
        appointmentTime = OffsetDateTime.now(),
        didSessionHappen = true,
        sessionSummary = "sessionSummary",
        sessionResponse = "sessionResponse",
      )
      val submittedBy = authUserFactory.create()

      whenever(appointmentRepository.save(any())).thenReturn(appointment)
      whenever(authUserRepository.save(any())).thenReturn(submittedBy)

      appointmentService.recordAppointmentAttendance(appointment, attended, false, submittedBy)

      val argumentCaptor = argumentCaptor<Appointment>()
      verify(appointmentRepository, times(1)).save(argumentCaptor.capture())
      val arguments = argumentCaptor.firstValue

      assertThat(arguments.id).isEqualTo(appointmentId)
      assertThat(arguments.attended).isEqualTo(attended)
      assertThat(arguments.sessionSummary).isNull()
      assertThat(arguments.sessionResponse).isNull()
    }

    @Test
    fun `previously saved session feedback fields are cleared when recording attendance and didSessionHappen has not changed but is false and attendance has changed`() {
      val appointmentId = UUID.randomUUID()
      val appointment = appointmentFactory.create(
        id = appointmentId,
        appointmentTime = OffsetDateTime.now(),
        attended = YES,
        didSessionHappen = false,
        noSessionReasonType = NoSessionReasonType.POP_ACCEPTABLE,
        noSessionReasonPopAcceptable = "noSessionReasonPopAcceptable",
      )
      val submittedBy = authUserFactory.create()

      whenever(appointmentRepository.save(any())).thenReturn(appointment)
      whenever(authUserRepository.save(any())).thenReturn(submittedBy)

      appointmentService.recordAppointmentAttendance(appointment, NO, false, submittedBy)

      val argumentCaptor = argumentCaptor<Appointment>()
      verify(appointmentRepository, times(1)).save(argumentCaptor.capture())
      val arguments = argumentCaptor.firstValue

      assertThat(arguments.id).isEqualTo(appointmentId)
      assertThat(arguments.attended).isEqualTo(NO)
      assertThat(arguments.didSessionHappen).isEqualTo(false)
      assertThat(arguments.noSessionReasonType).isNull()
      assertThat(arguments.noSessionReasonPopAcceptable).isNull()
    }

    @Test
    fun `previously saved session feedback fields are not cleared when recording attendance and didSessionHappen value has not changed`() {
      val appointmentId = UUID.randomUUID()
      val attended = YES
      val appointment = appointmentFactory.create(
        id = appointmentId,
        appointmentTime = OffsetDateTime.now(),
        didSessionHappen = true,
        sessionSummary = "sessionSummary",
        sessionResponse = "sessionResponse",
      )
      val submittedBy = authUserFactory.create()

      whenever(appointmentRepository.save(any())).thenReturn(appointment)
      whenever(authUserRepository.save(any())).thenReturn(submittedBy)

      appointmentService.recordAppointmentAttendance(appointment, attended, true, submittedBy)

      val argumentCaptor = argumentCaptor<Appointment>()
      verify(appointmentRepository, times(1)).save(argumentCaptor.capture())
      val arguments = argumentCaptor.firstValue

      assertThat(arguments.id).isEqualTo(appointmentId)
      assertThat(arguments.attended).isEqualTo(attended)
      assertThat(arguments.sessionSummary).isEqualTo("sessionSummary")
      assertThat(arguments.sessionResponse).isEqualTo("sessionResponse")
    }

    @Test
    fun `appointment attendance cannot be updated if feedback has been submitted`() {
      val appointmentId = UUID.randomUUID()
      val attended = YES
      val submittedBy = authUserFactory.create()
      val feedbackSubmittedAt = OffsetDateTime.parse("2020-12-04T10:42:43+00:00")
      val appointment = appointmentFactory.create(id = appointmentId, appointmentFeedbackSubmittedAt = feedbackSubmittedAt)

      val error = assertThrows<ResponseStatusException> {
        appointmentService.recordAppointmentAttendance(appointment, attended, true, submittedBy)
      }
      assertThat(error.message).contains("Feedback has already been submitted for this appointment [id=$appointmentId]")
    }

    @Test
    fun `appointment attendance cannot be updated if session has a future date`() {
      val appointmentId = UUID.randomUUID()
      val attended = YES
      val submittedBy = authUserFactory.create()
      val appointment = appointmentFactory.create(id = appointmentId, appointmentTime = OffsetDateTime.now().plusMonths(2))

      val error = assertThrows<ResponseStatusException> {
        appointmentService.recordAppointmentAttendance(appointment, attended, true, submittedBy)
      }
      assertThat(error.message).contains("Cannot submit feedback for a future appointment [id=${appointment.id}]")
    }
  }

  @Nested
  inner class SubmitSessionFeedback {
    @Test
    fun `appointment feedback can be submitted`() {
      val appointmentId = UUID.randomUUID()
      val appointment = appointmentFactory.create(id = appointmentId)
      val submittedBy = authUserFactory.create()

      appointment.attendanceSubmittedAt = OffsetDateTime.now()
      appointment.attended = YES

      whenever(appointmentRepository.save(any())).thenReturn(appointment)
      whenever(authUserRepository.save(any())).thenReturn(submittedBy)

      appointmentService.submitAppointmentFeedback(appointment, submittedBy, SUPPLIER_ASSESSMENT)

      val argumentCaptor = argumentCaptor<Appointment>()
      verify(appointmentRepository, times(1)).save(argumentCaptor.capture())
      val arguments = argumentCaptor.firstValue

      assertThat(arguments.id).isEqualTo(appointmentId)
      assertThat(arguments.appointmentFeedbackSubmittedAt).isNotNull
      assertThat(arguments.appointmentFeedbackSubmittedBy).isEqualTo(submittedBy)

      verify(appointmentEventPublisher).appointmentFeedbackRecordedEvent(appointment, false, SUPPLIER_ASSESSMENT)
    }

    @Test
    fun `appointment feedback can't be submitted more than once`() {
      val appointmentId = UUID.randomUUID()
      val appointment = appointmentFactory.create(id = appointmentId)
      val submittedBy = authUserFactory.create()

      appointment.appointmentFeedbackSubmittedAt = OffsetDateTime.now()

      val exception = assertThrows<ResponseStatusException> {
        appointmentService.submitAppointmentFeedback(appointment, submittedBy, SUPPLIER_ASSESSMENT)
      }
      assertThat(exception.message).contains("appointment feedback has already been submitted")
    }

    @Test
    fun `appointment feedback can't be submitted if attendance hasn't been recorded`() {
      val appointmentId = UUID.randomUUID()
      val appointment = appointmentFactory.create(id = appointmentId)
      val submittedBy = authUserFactory.create()

      val exception = assertThrows<ResponseStatusException> {
        appointmentService.submitAppointmentFeedback(appointment, submittedBy, SUPPLIER_ASSESSMENT)
      }
      assertThat(exception.message).contains("can't submit feedback unless attendance has been recorded")
    }

    @Test
    fun `appointment feedback emits application events`() {
      val appointmentId = UUID.randomUUID()
      val appointment = appointmentFactory.create(id = appointmentId)
      val submittedBy = authUserFactory.create()

      appointment.attendanceSubmittedAt = OffsetDateTime.now()
      appointment.attended = NO
      appointment.sessionFeedbackSubmittedAt = OffsetDateTime.now()
      appointment.notifyPPOfAttendanceBehaviour = true

      whenever(appointmentRepository.save(any())).thenReturn(appointment)
      whenever(authUserRepository.save(any())).thenReturn(submittedBy)

      appointmentService.submitAppointmentFeedback(appointment, submittedBy, SUPPLIER_ASSESSMENT)

      verify(appointmentEventPublisher).attendanceRecordedEvent(appointment, true, SUPPLIER_ASSESSMENT)
      verify(appointmentEventPublisher).sessionFeedbackRecordedEvent(appointment, true, SUPPLIER_ASSESSMENT)
    }
  }
}
