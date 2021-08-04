package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.atLeast
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.web.server.ResponseStatusException
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.AppointmentEventPublisher
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Appointment
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AppointmentDelivery
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AppointmentDeliveryType
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AppointmentSessionType
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AppointmentType.SUPPLIER_ASSESSMENT
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Attended
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Attended.NO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Attended.YES
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.AppointmentDeliveryRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.AppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.AuthUserRepository
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

  private val authUserFactory = AuthUserFactory()
  private val appointmentFactory = AppointmentFactory()
  private val referralFactory = ReferralFactory()

  private val createdByUser = authUserFactory.create()

  private val appointmentService = AppointmentService(
    appointmentRepository,
    communityAPIBookingService,
    appointmentDeliveryRepository,
    authUserRepository,
    appointmentEventPublisher
  )

  @BeforeEach
  fun beforeEach() {
    whenever(authUserRepository.save(any())).thenReturn(createdByUser)
  }

  @Test
  fun `can create new appointment with delius office location delivery`() {
    // Given
    val durationInMinutes = 60
    val appointmentTime = OffsetDateTime.now()
    val referral = referralFactory.createSent()
    val deliusAppointmentId = 99L
    val npsOfficeCode = "CRSEXT"

    whenever(communityAPIBookingService.book(referral, null, appointmentTime, durationInMinutes, SUPPLIER_ASSESSMENT, npsOfficeCode))
      .thenReturn(deliusAppointmentId)
    val savedAppointment = appointmentFactory.create(
      appointmentTime = appointmentTime,
      durationInMinutes = durationInMinutes,
      deliusAppointmentId = deliusAppointmentId,
    )
    whenever(appointmentRepository.save(any())).thenReturn(savedAppointment)

    // When
    val newAppointment = appointmentService.createOrUpdateAppointment(referral, null, durationInMinutes, appointmentTime, SUPPLIER_ASSESSMENT, createdByUser, AppointmentDeliveryType.IN_PERSON_MEETING_PROBATION_OFFICE, AppointmentSessionType.ONE_TO_ONE, npsOfficeCode = npsOfficeCode)

    // Then
    verifyResponse(newAppointment, null, true, deliusAppointmentId, appointmentTime, durationInMinutes, AppointmentDeliveryType.IN_PERSON_MEETING_PROBATION_OFFICE, AppointmentSessionType.ONE_TO_ONE, npsOfficeCode)
    verifySavedAppointment(appointmentTime, durationInMinutes, deliusAppointmentId, AppointmentDeliveryType.IN_PERSON_MEETING_PROBATION_OFFICE, AppointmentSessionType.ONE_TO_ONE, npsOfficeCode)
  }

  @Test
  fun `create new appointment if none currently exist`() {
    // Given
    val durationInMinutes = 60
    val appointmentTime = OffsetDateTime.parse("2020-12-04T10:42:43+00:00")
    val referral = referralFactory.createSent()
    val deliusAppointmentId = 99L

    whenever(communityAPIBookingService.book(referral, null, appointmentTime, durationInMinutes, SUPPLIER_ASSESSMENT, null))
      .thenReturn(deliusAppointmentId)
    val savedAppointment = appointmentFactory.create(
      appointmentTime = appointmentTime,
      durationInMinutes = durationInMinutes,
      deliusAppointmentId = deliusAppointmentId,
    )
    whenever(appointmentRepository.save(any())).thenReturn(savedAppointment)

    // When
    val newAppointment = appointmentService.createOrUpdateAppointment(referral, null, durationInMinutes, appointmentTime, SUPPLIER_ASSESSMENT, createdByUser, AppointmentDeliveryType.PHONE_CALL, AppointmentSessionType.ONE_TO_ONE)

    // Then
    verifyResponse(newAppointment, null, true, deliusAppointmentId, appointmentTime, durationInMinutes, AppointmentDeliveryType.PHONE_CALL, AppointmentSessionType.ONE_TO_ONE)
    verifySavedAppointment(appointmentTime, durationInMinutes, deliusAppointmentId, AppointmentDeliveryType.PHONE_CALL, AppointmentSessionType.ONE_TO_ONE)
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
      .thenReturn(rescheduledDeliusAppointmentId)
    val savedAppointment = appointmentFactory.create(
      appointmentTime = appointmentTime,
      durationInMinutes = durationInMinutes,
      deliusAppointmentId = rescheduledDeliusAppointmentId,
    )
    whenever(appointmentRepository.save(any())).thenReturn(savedAppointment)

    // When
    val updatedAppointment = appointmentService.createOrUpdateAppointment(referral, existingAppointment, durationInMinutes, appointmentTime, SUPPLIER_ASSESSMENT, createdByUser, AppointmentDeliveryType.PHONE_CALL, AppointmentSessionType.ONE_TO_ONE)

    // Then
    verifyResponse(updatedAppointment, existingAppointment.id, false, rescheduledDeliusAppointmentId, appointmentTime, durationInMinutes, AppointmentDeliveryType.PHONE_CALL, AppointmentSessionType.ONE_TO_ONE)
    verifySavedAppointment(appointmentTime, durationInMinutes, rescheduledDeliusAppointmentId, AppointmentDeliveryType.PHONE_CALL, AppointmentSessionType.ONE_TO_ONE)
  }

  @Test
  fun `appointment with none attendance will create a new appointment`() {
    // Given
    val durationInMinutes = 60
    val appointmentTime = OffsetDateTime.parse("2020-12-04T10:42:43+00:00")
    val existingAppointment = appointmentFactory.create(deliusAppointmentId = 98L, attended = NO)
    val referral = referralFactory.createSent()
    val additionalDeliusAppointmentId = 99L

    whenever(communityAPIBookingService.book(referral, null, appointmentTime, durationInMinutes, SUPPLIER_ASSESSMENT, null)).thenReturn(additionalDeliusAppointmentId)
    val savedAppointment = appointmentFactory.create(
      appointmentTime = appointmentTime,
      durationInMinutes = durationInMinutes,
      deliusAppointmentId = additionalDeliusAppointmentId,
    )
    whenever(appointmentRepository.save(any())).thenReturn(savedAppointment)

    // When
    val newAppointment = appointmentService.createOrUpdateAppointment(referral, existingAppointment, durationInMinutes, appointmentTime, SUPPLIER_ASSESSMENT, createdByUser, AppointmentDeliveryType.PHONE_CALL, AppointmentSessionType.ONE_TO_ONE)

    // Then
    verifyResponse(newAppointment, existingAppointment.id, true, additionalDeliusAppointmentId, appointmentTime, durationInMinutes, AppointmentDeliveryType.PHONE_CALL, AppointmentSessionType.ONE_TO_ONE)
    verifySavedAppointment(appointmentTime, durationInMinutes, additionalDeliusAppointmentId, AppointmentDeliveryType.PHONE_CALL, AppointmentSessionType.ONE_TO_ONE)
  }

  @Test
  fun `any other scenario will throw an error`() {
    val durationInMinutes = 60
    val appointmentTime = OffsetDateTime.parse("2020-12-04T10:42:43+00:00")
    val createdByUser = authUserFactory.create()
    val appointment = appointmentFactory.create(attended = Attended.YES)
    val referral = referralFactory.createSent()

    val error = assertThrows<IllegalStateException> {
      appointmentService.createOrUpdateAppointment(referral, appointment, durationInMinutes, appointmentTime, SUPPLIER_ASSESSMENT, createdByUser, AppointmentDeliveryType.PHONE_CALL, AppointmentSessionType.ONE_TO_ONE)
    }
    assertThat(error.message).contains("Is it not possible to update an appointment that has already been attended")
  }

  private fun verifyResponse(appointment: Appointment, originalId: UUID?, expectNewId: Boolean, deliusAppointmentId: Long, appointmentTime: OffsetDateTime?, durationInMinutes: Int, appointmentDeliveryType: AppointmentDeliveryType, appointmentSessionType: AppointmentSessionType, npsOfficeCode: String? = null) {

    // Verifying create or update route
    if (expectNewId)
      assertThat(appointment).isNotEqualTo(originalId)
    else
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

      whenever(communityAPIBookingService.book(referral, existingAppointment, appointmentTime, durationInMinutes, SUPPLIER_ASSESSMENT, null))
        .thenReturn(rescheduledDeliusAppointmentId)
      val savedAppointment = appointmentFactory.create(
        appointmentTime = appointmentTime,
        durationInMinutes = durationInMinutes,
        deliusAppointmentId = rescheduledDeliusAppointmentId,
      )
      savedAppointment.appointmentDelivery = AppointmentDelivery(appointmentId = savedAppointment.id, appointmentDeliveryType = AppointmentDeliveryType.IN_PERSON_MEETING_PROBATION_OFFICE, AppointmentSessionType.ONE_TO_ONE, npsOfficeCode = oldNpsCode)

      whenever(appointmentRepository.save(any())).thenReturn(savedAppointment)

      // When
      val updatedAppointment = appointmentService.createOrUpdateAppointment(referral, existingAppointment, durationInMinutes, appointmentTime, SUPPLIER_ASSESSMENT, createdByUser, AppointmentDeliveryType.IN_PERSON_MEETING_PROBATION_OFFICE, AppointmentSessionType.ONE_TO_ONE, npsOfficeCode = newNpsCode)

      // Then
      verifyResponse(updatedAppointment, existingAppointment.id, false, rescheduledDeliusAppointmentId, appointmentTime, durationInMinutes, AppointmentDeliveryType.IN_PERSON_MEETING_PROBATION_OFFICE, AppointmentSessionType.ONE_TO_ONE, newNpsCode)
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
      val attended = Attended.YES
      val additionalAttendanceInformation = "information"
      val appointment = appointmentFactory.create(id = appointmentId)
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
      val attended = Attended.YES
      val additionalAttendanceInformation = "information"
      val submittedBy = authUserFactory.create()
      val feedbackSubmittedAt = OffsetDateTime.parse("2020-12-04T10:42:43+00:00")
      val appointment = appointmentFactory.create(id = appointmentId, appointmentFeedbackSubmittedAt = feedbackSubmittedAt)

      val error = assertThrows<ResponseStatusException> {
        appointmentService.recordAppointmentAttendance(appointment, attended, additionalAttendanceInformation, submittedBy)
      }
      assertThat(error.message).contains("Feedback has already been submitted for this appointment [id=$appointmentId]")
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
