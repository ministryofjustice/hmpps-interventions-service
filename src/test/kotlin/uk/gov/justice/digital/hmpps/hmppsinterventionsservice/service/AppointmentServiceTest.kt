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
    val savedAppointment = appointmentFactory.create(
      appointmentTime = appointmentTime,
      durationInMinutes = durationInMinutes,
      deliusAppointmentId = deliusAppointmentId,
    )

    whenever(communityAPIBookingService.book(referral, null, appointmentTime, durationInMinutes, SUPPLIER_ASSESSMENT, npsOfficeCode))
      .thenReturn(Pair(deliusAppointmentId, savedAppointment.id))
    whenever(appointmentRepository.save(any())).thenReturn(savedAppointment)

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
    val savedAppointment = appointmentFactory.create(
      appointmentTime = appointmentTime,
      durationInMinutes = durationInMinutes,
      deliusAppointmentId = deliusAppointmentId,
    )

    whenever(communityAPIBookingService.book(referral, null, appointmentTime, durationInMinutes, SUPPLIER_ASSESSMENT, null))
      .thenReturn(Pair(deliusAppointmentId, savedAppointment.id))
    whenever(appointmentRepository.save(any())).thenReturn(savedAppointment)

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
    val savedAppointment = appointmentFactory.create(
      appointmentTime = appointmentTime,
      durationInMinutes = durationInMinutes,
      deliusAppointmentId = deliusAppointmentId,
    )

    whenever(communityAPIBookingService.book(referral, null, appointmentTime, durationInMinutes, SUPPLIER_ASSESSMENT, null, YES, false))
      .thenReturn(Pair(deliusAppointmentId, savedAppointment.id))
    whenever(appointmentRepository.save(any())).thenReturn(savedAppointment)

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
      YES,
      "additional information",
      false,
      "description",
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
    verify(appointmentEventPublisher).behaviourRecordedEvent(newAppointment, false, AppointmentType.SUPPLIER_ASSESSMENT)
    verify(appointmentEventPublisher).sessionFeedbackRecordedEvent(newAppointment, false, AppointmentType.SUPPLIER_ASSESSMENT)
    assertThat(newAppointment.attended).isEqualTo(YES)
    assertThat(newAppointment.additionalAttendanceInformation).isEqualTo("additional information")
    assertThat(newAppointment.attendanceBehaviour).isEqualTo("description")
    assertThat(newAppointment.notifyPPOfAttendanceBehaviour).isEqualTo(false)
    assertThat(newAppointment.attendanceSubmittedAt).isNotNull
    assertThat(newAppointment.attendanceSubmittedBy).isEqualTo(createdByUser)
    assertThat(newAppointment.attendanceBehaviourSubmittedAt).isNotNull
    assertThat(newAppointment.attendanceBehaviourSubmittedBy).isEqualTo(createdByUser)
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
      .thenReturn(Pair(rescheduledDeliusAppointmentId, null))
    val savedAppointment = appointmentFactory.create(
      appointmentTime = appointmentTime,
      durationInMinutes = durationInMinutes,
      deliusAppointmentId = rescheduledDeliusAppointmentId,
    )
    whenever(appointmentRepository.saveAndFlush(any())).thenReturn(savedAppointment)

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
    verify(appointmentRepository, atLeast(1)).save(argumentCaptor.capture())
    val oldAppointmentArguments = argumentCaptor.lastValue
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
    val existingAppointment = appointmentFactory.create(deliusAppointmentId = 98L, attended = NO)
    val referral = referralFactory.createSent()
    val additionalDeliusAppointmentId = 99L
    val savedAppointment = appointmentFactory.create(
      appointmentTime = appointmentTime,
      durationInMinutes = durationInMinutes,
      deliusAppointmentId = additionalDeliusAppointmentId,
    )

    whenever(communityAPIBookingService.book(referral, null, appointmentTime, durationInMinutes, SUPPLIER_ASSESSMENT, null))
      .thenReturn(Pair(additionalDeliusAppointmentId, savedAppointment.id))
    whenever(appointmentRepository.save(any())).thenReturn(savedAppointment)

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
    val savedAppointment = appointmentFactory.create(
      appointmentTime = OffsetDateTime.parse("2020-12-04T10:42:43+00:00"),
      durationInMinutes = durationInMinutes,
      deliusAppointmentId = rescheduledDeliusAppointmentId,
      attended = NO,
      additionalAttendanceInformation = "non attended",
      attendanceBehaviour = null,
      notifyPPOfAttendanceBehaviour = null,
    )

    whenever(communityAPIBookingService.book(referral, existingAppointment, pastAppointmentTime, durationInMinutes, SUPPLIER_ASSESSMENT, null))
      .thenReturn(Pair(rescheduledDeliusAppointmentId, savedAppointment.id))
    whenever(appointmentRepository.save(any())).thenReturn(savedAppointment)

    // When
    val updatedAppointment = appointmentService.createOrUpdateAppointment(referral, existingAppointment, durationInMinutes, pastAppointmentTime, SUPPLIER_ASSESSMENT, createdByUser, AppointmentDeliveryType.PHONE_CALL, AppointmentSessionType.ONE_TO_ONE, null, null, NO, "non attended", null, null)

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
    assertThat(updatedAppointment.additionalAttendanceInformation).isEqualTo("non attended")
    assertThat(updatedAppointment.attendanceBehaviour).isNull()
    assertThat(updatedAppointment.notifyPPOfAttendanceBehaviour).isNull()
  }

  @Test
  fun `appointment with none attendance will create a new historic appointment`() {
    // Given
    val durationInMinutes = 60
    val appointmentTime = OffsetDateTime.parse("2021-12-04T10:42:43+00:00")
    val existingAppointment = appointmentFactory.create(deliusAppointmentId = 98L, attended = NO)
    val referral = referralFactory.createSent()
    val additionalDeliusAppointmentId = 99L
    val savedAppointment = appointmentFactory.create(
      appointmentTime = appointmentTime,
      durationInMinutes = durationInMinutes,
      deliusAppointmentId = additionalDeliusAppointmentId,
      attended = NO,
      additionalAttendanceInformation = "non attended",
      attendanceBehaviour = null,
      notifyPPOfAttendanceBehaviour = null,
    )

    whenever(communityAPIBookingService.book(referral, null, appointmentTime, durationInMinutes, SUPPLIER_ASSESSMENT, null, NO, null))
      .thenReturn(Pair(additionalDeliusAppointmentId, savedAppointment.id))
    whenever(appointmentRepository.save(any())).thenReturn(savedAppointment)

    // When
    val newAppointment = appointmentService.createOrUpdateAppointment(referral, existingAppointment, durationInMinutes, appointmentTime, SUPPLIER_ASSESSMENT, createdByUser, AppointmentDeliveryType.PHONE_CALL, AppointmentSessionType.ONE_TO_ONE, null, null, NO, "non attended", null, null)

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
    assertThat(newAppointment.attended).isEqualTo(NO)
    assertThat(newAppointment.additionalAttendanceInformation).isEqualTo("non attended")
    assertThat(newAppointment.attendanceBehaviour).isNull()
    assertThat(newAppointment.notifyPPOfAttendanceBehaviour).isNull()
    assertThat(newAppointment.attendanceSubmittedAt).isNotNull
    assertThat(newAppointment.attendanceSubmittedBy).isEqualTo(createdByUser)
    assertThat(newAppointment.attendanceBehaviourSubmittedAt).isNull()
    assertThat(newAppointment.attendanceBehaviourSubmittedBy).isNull()
    assertThat(newAppointment.appointmentFeedbackSubmittedAt).isNotNull
    assertThat(newAppointment.appointmentFeedbackSubmittedBy).isEqualTo(createdByUser)
  }

  @Test
  fun `any other scenario will throw an error`() {
    val durationInMinutes = 60
    val appointmentTime = OffsetDateTime.parse("2020-12-04T10:42:43+00:00")
    val createdByUser = authUserFactory.create()
    val appointment = appointmentFactory.create(attended = YES)
    val referral = referralFactory.createSent()

    val error = assertThrows<IllegalStateException> {
      appointmentService.createOrUpdateAppointment(referral, appointment, durationInMinutes, appointmentTime, SUPPLIER_ASSESSMENT, createdByUser, AppointmentDeliveryType.PHONE_CALL, AppointmentSessionType.ONE_TO_ONE)
    }
    assertThat(error.message).contains("Is it not possible to update an appointment that has already been attended")
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
        appointmentTime = appointmentTime,
        durationInMinutes = durationInMinutes,
        deliusAppointmentId = rescheduledDeliusAppointmentId,
      )

      whenever(communityAPIBookingService.book(referral, existingAppointment, appointmentTime, durationInMinutes, SUPPLIER_ASSESSMENT, newNpsCode))
        .thenReturn(Pair(rescheduledDeliusAppointmentId, savedAppointment.id))
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
      val behaviourDescription = "description"
      val notifyProbationPractitioner = true
      val appointment = appointmentFactory.create(id = appointmentId)
      val submittedBy = authUserFactory.create()

      whenever(appointmentRepository.save(any())).thenReturn(appointment)
      whenever(authUserRepository.save(any())).thenReturn(submittedBy)

      appointmentService.recordBehaviour(appointment, behaviourDescription, notifyProbationPractitioner, submittedBy)

      val argumentCaptor = argumentCaptor<Appointment>()
      verify(appointmentRepository, times(1)).save(argumentCaptor.capture())
      val arguments = argumentCaptor.firstValue

      assertThat(arguments.id).isEqualTo(appointmentId)
      assertThat(arguments.attendanceBehaviour).isEqualTo(behaviourDescription)
      assertThat(arguments.notifyPPOfAttendanceBehaviour).isEqualTo(notifyProbationPractitioner)
      assertThat(arguments.attendanceBehaviourSubmittedAt).isNotNull
      assertThat(arguments.attendanceBehaviourSubmittedBy).isEqualTo(submittedBy)
    }

    @Test
    fun `appointment behaviour cannot be updated if feedback has been submitted`() {
      val appointmentId = UUID.randomUUID()
      val behaviourDescription = "description"
      val notifyProbationPractitioner = true
      val submittedBy = authUserFactory.create()
      val feedbackSubmittedAt = OffsetDateTime.parse("2020-12-04T10:42:43+00:00")
      val appointment = appointmentFactory.create(id = appointmentId, appointmentFeedbackSubmittedAt = feedbackSubmittedAt)

      val error = assertThrows<ResponseStatusException> {
        appointmentService.recordBehaviour(appointment, behaviourDescription, notifyProbationPractitioner, submittedBy)
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
      val additionalAttendanceInformation = "information"
      val appointment = appointmentFactory.create(id = appointmentId, appointmentTime = OffsetDateTime.now())
      val submittedBy = authUserFactory.create()

      whenever(appointmentRepository.save(any())).thenReturn(appointment)
      whenever(authUserRepository.save(any())).thenReturn(submittedBy)

      appointmentService.recordAppointmentAttendance(appointment, attended, additionalAttendanceInformation, submittedBy)

      val argumentCaptor = argumentCaptor<Appointment>()
      verify(appointmentRepository, times(1)).save(argumentCaptor.capture())
      val arguments = argumentCaptor.firstValue

      assertThat(arguments.id).isEqualTo(appointmentId)
      assertThat(arguments.attended).isEqualTo(attended)
      assertThat(arguments.additionalAttendanceInformation).isEqualTo(additionalAttendanceInformation)
      assertThat(arguments.attendanceSubmittedAt).isNotNull
      assertThat(arguments.attendanceSubmittedBy).isEqualTo(submittedBy)
    }

    @Test
    fun `appointment attendance cannot be updated if feedback has been submitted`() {
      val appointmentId = UUID.randomUUID()
      val attended = YES
      val additionalAttendanceInformation = "information"
      val submittedBy = authUserFactory.create()
      val feedbackSubmittedAt = OffsetDateTime.parse("2020-12-04T10:42:43+00:00")
      val appointment = appointmentFactory.create(id = appointmentId, appointmentFeedbackSubmittedAt = feedbackSubmittedAt)

      val error = assertThrows<ResponseStatusException> {
        appointmentService.recordAppointmentAttendance(appointment, attended, additionalAttendanceInformation, submittedBy)
      }
      assertThat(error.message).contains("Feedback has already been submitted for this appointment [id=$appointmentId]")
    }

    @Test
    fun `appointment attendance cannot be updated if session has a future date`() {
      val appointmentId = UUID.randomUUID()
      val attended = YES
      val additionalAttendanceInformation = "information"
      val submittedBy = authUserFactory.create()
      val appointment = appointmentFactory.create(id = appointmentId, appointmentTime = OffsetDateTime.now().plusMonths(2))

      val error = assertThrows<ResponseStatusException> {
        appointmentService.recordAppointmentAttendance(appointment, attended, additionalAttendanceInformation, submittedBy)
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

      appointmentService.submitSessionFeedback(appointment, submittedBy, SUPPLIER_ASSESSMENT)

      val argumentCaptor = argumentCaptor<Appointment>()
      verify(appointmentRepository, times(1)).save(argumentCaptor.capture())
      val arguments = argumentCaptor.firstValue

      assertThat(arguments.id).isEqualTo(appointmentId)
      assertThat(arguments.appointmentFeedbackSubmittedAt).isNotNull
      assertThat(arguments.appointmentFeedbackSubmittedBy).isEqualTo(submittedBy)

      verify(appointmentEventPublisher).sessionFeedbackRecordedEvent(appointment, false, SUPPLIER_ASSESSMENT)
    }

    @Test
    fun `appointment feedback can't be submitted more than once`() {
      val appointmentId = UUID.randomUUID()
      val appointment = appointmentFactory.create(id = appointmentId)
      val submittedBy = authUserFactory.create()

      appointment.appointmentFeedbackSubmittedAt = OffsetDateTime.now()

      val exception = assertThrows<ResponseStatusException> {
        appointmentService.submitSessionFeedback(appointment, submittedBy, SUPPLIER_ASSESSMENT)
      }
      assertThat(exception.message).contains("appointment feedback has already been submitted")
    }

    @Test
    fun `appointment feedback can't be submitted if attendance hasn't been recorded`() {
      val appointmentId = UUID.randomUUID()
      val appointment = appointmentFactory.create(id = appointmentId)
      val submittedBy = authUserFactory.create()

      val exception = assertThrows<ResponseStatusException> {
        appointmentService.submitSessionFeedback(appointment, submittedBy, SUPPLIER_ASSESSMENT)
      }
      assertThat(exception.message).contains("can't submit feedback unless attendance has been recorded")
    }

    @Test
    fun `session feedback emits application events`() {
      val appointmentId = UUID.randomUUID()
      val appointment = appointmentFactory.create(id = appointmentId)
      val submittedBy = authUserFactory.create()

      appointment.attendanceSubmittedAt = OffsetDateTime.now()
      appointment.attended = NO
      appointment.attendanceBehaviourSubmittedAt = OffsetDateTime.now()
      appointment.notifyPPOfAttendanceBehaviour = true

      whenever(appointmentRepository.save(any())).thenReturn(appointment)
      whenever(authUserRepository.save(any())).thenReturn(submittedBy)

      appointmentService.submitSessionFeedback(appointment, submittedBy, SUPPLIER_ASSESSMENT)

      verify(appointmentEventPublisher).attendanceRecordedEvent(appointment, true, SUPPLIER_ASSESSMENT)
      verify(appointmentEventPublisher).behaviourRecordedEvent(appointment, true, SUPPLIER_ASSESSMENT)
    }
  }
}
