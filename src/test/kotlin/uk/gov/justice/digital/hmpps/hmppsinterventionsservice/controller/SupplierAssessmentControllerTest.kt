package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.controller

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.authorization.UserMapper
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.AddressDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.AttendanceFeedbackRequestDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.SessionFeedbackRequestDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.UpdateAppointmentDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AppointmentDeliveryType
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AppointmentSessionType
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AppointmentType
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Attended
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Attended.LATE
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.AppointmentService
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.ReferralService
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.SupplierAssessmentService
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.AppointmentFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.AuthUserFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.JwtTokenFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ReferralFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.SupplierAssessmentFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.validator.AppointmentValidator
import java.time.OffsetDateTime
import java.util.UUID

class SupplierAssessmentControllerTest {
  private val userMapper = mock<UserMapper>()
  private val referralService = mock<ReferralService>()
  private val supplierAssessmentService = mock<SupplierAssessmentService>()
  private val appointmentValidator = mock<AppointmentValidator>()
  private val appointmentService = mock<AppointmentService>()

  private val tokenFactory = JwtTokenFactory()
  private val authUserFactory = AuthUserFactory()
  private val referralFactory = ReferralFactory()
  private val supplierAssessmentFactory = SupplierAssessmentFactory()
  private val appointmentFactory = AppointmentFactory()

  private val supplierAssessmentController = SupplierAssessmentController(referralService, supplierAssessmentService, userMapper, appointmentValidator, appointmentService)

  @Nested
  inner class UpdateSupplierAssessmentAppointment {
    @Test
    fun `update supplier assessment appointment details`() {
      val referral = referralFactory.createSent(sentAt = OffsetDateTime.now().minusDays(10))
      val durationInMinutes = 60
      val appointmentTime = OffsetDateTime.now().minusDays(5)
      val appointmentDeliveryType = AppointmentDeliveryType.PHONE_CALL
      val npsOfficeCode = "CRSEXT"
      val appointmentSessionType = AppointmentSessionType.ONE_TO_ONE
      val addressDTO = AddressDTO(
        firstAddressLine = "Harmony Living Office, Room 4",
        secondAddressLine = "44 Bouverie Road",
        townOrCity = "Blackpool",
        county = "Lancashire",
        postCode = "SY40RE",
      )
      val update = UpdateAppointmentDTO(appointmentTime, durationInMinutes, appointmentDeliveryType, appointmentSessionType, addressDTO, npsOfficeCode)
      val user = authUserFactory.create()
      val token = tokenFactory.create()
      val supplierAssessment = supplierAssessmentFactory.create()

      whenever(userMapper.fromToken(token)).thenReturn(user)
      whenever(referralService.getSentReferralForUser(referral.id, user)).thenReturn(referral)
      whenever(supplierAssessmentService.getSupplierAssessmentById(any())).thenReturn(supplierAssessment)
      whenever(supplierAssessmentService.createOrUpdateSupplierAssessmentAppointment(supplierAssessment, durationInMinutes, appointmentTime, user, appointmentDeliveryType, appointmentSessionType, addressDTO, npsOfficeCode, null, null, null, null)).thenReturn(supplierAssessment.currentAppointment)

      val response = supplierAssessmentController.updateSupplierAssessmentAppointment(referral.id, update, token)
      verify(appointmentValidator).validateUpdateAppointment(eq(update), any())
      assertThat(response).isNotNull
    }

    @Test
    fun `update supplier assessment with new historic appointment`() {
      val referral = referralFactory.createSent(sentAt = OffsetDateTime.now().minusDays(10))
      val durationInMinutes = 60
      val appointmentTime = OffsetDateTime.now().minusDays(2)
      val appointmentDeliveryType = AppointmentDeliveryType.PHONE_CALL
      val npsOfficeCode = "CRSEXT"
      val appointmentSessionType = AppointmentSessionType.ONE_TO_ONE
      val addressDTO = null
      val attendanceFeedbackRequestDTO = AttendanceFeedbackRequestDTO(LATE, true)
      val sessionFeedbackRequestDTO = SessionFeedbackRequestDTO(
        late = true,
        lateReason = "missed bus",
        sessionSummary = "summary",
        sessionResponse = "response",
        notifyProbationPractitioner = true,
      )
      val update = UpdateAppointmentDTO(appointmentTime, durationInMinutes, appointmentDeliveryType, appointmentSessionType, addressDTO, npsOfficeCode, attendanceFeedbackRequestDTO, sessionFeedbackRequestDTO)
      val user = authUserFactory.create()
      val token = tokenFactory.create()
      val supplierAssessment = supplierAssessmentFactory.create()

      whenever(userMapper.fromToken(token)).thenReturn(user)
      whenever(referralService.getSentReferralForUser(referral.id, user)).thenReturn(referral)
      whenever(supplierAssessmentService.getSupplierAssessmentById(any())).thenReturn(supplierAssessment)
      whenever(
        supplierAssessmentService.createOrUpdateSupplierAssessmentAppointment(
          supplierAssessment,
          durationInMinutes,
          appointmentTime,
          user,
          appointmentDeliveryType,
          appointmentSessionType,
          addressDTO,
          npsOfficeCode,
          attended = LATE,
          didSessionHappen = true,
          notifyProbationPractitioner = true,
          late = true,
          lateReason = "missed bus",
          sessionSummary = "summary",
          sessionResponse = "response",
        ),
      ).thenReturn(supplierAssessment.currentAppointment)

      val response = supplierAssessmentController.updateSupplierAssessmentAppointment(referral.id, update, token)
      verify(appointmentValidator).validateUpdateAppointment(eq(update), any())
      assertThat(response).isNotNull
    }
  }

  @Nested
  inner class RecordAppointmentSessionFeedback {
    @Test
    fun `can record session feedback`() {
      val referralId = UUID.randomUUID()
      val sessionSummary = "summary"
      val sessionResponse = "response"
      val sessionConcerns = "concerns"
      val notifyProbationPractitioner = true
      val submittedBy = authUserFactory.createSP()
      val token = tokenFactory.create()

      val supplierAssessment = supplierAssessmentFactory.create()
      supplierAssessment.referral.supplierAssessment = supplierAssessment
      whenever(userMapper.fromToken(token)).thenReturn(submittedBy)
      whenever(referralService.getSentReferralForUser(eq(referralId), eq(submittedBy))).thenReturn(supplierAssessment.referral)
      whenever(
        appointmentService.recordSessionFeedback(
          eq(supplierAssessment!!.currentAppointment!!),
          eq(false),
          eq(null),
          eq(null),
          eq(null),
          eq(null),
          eq(null),
          eq(null),
          eq(null),
          eq(sessionSummary),
          eq(sessionResponse),
          eq(sessionConcerns),
          eq(notifyProbationPractitioner),
          eq(submittedBy),
        ),
      ).thenReturn(appointmentFactory.create())

      val request = SessionFeedbackRequestDTO(
        late = false,
        sessionSummary = sessionSummary,
        sessionResponse = sessionResponse,
        sessionConcerns = sessionConcerns,
        notifyProbationPractitioner = notifyProbationPractitioner,
      )
      val result = supplierAssessmentController.recordSessionFeedback(referralId, request, token)
      assertThat(result).isNotNull
    }

    @Test
    fun `expect not found if referral does not exist`() {
      val referralId = UUID.randomUUID()
      val sessionSummary = "summary"
      val sessionResponse = "response"
      val notifyProbationPractitioner = true
      val submittedBy = authUserFactory.createSP()
      val token = tokenFactory.create()

      whenever(userMapper.fromToken(token)).thenReturn(submittedBy)
      whenever(referralService.getSentReferralForUser(eq(referralId), eq(submittedBy))).thenReturn(null)

      val request = SessionFeedbackRequestDTO(
        late = false,
        sessionSummary = sessionSummary,
        sessionResponse = sessionResponse,
        notifyProbationPractitioner = notifyProbationPractitioner,
      )
      val exception = assertThrows<ResponseStatusException> {
        supplierAssessmentController.recordSessionFeedback(referralId, request, token)
      }
      assertThat(exception.status).isEqualTo(HttpStatus.NOT_FOUND)
      assertThat(exception.message).contains("referral not found")
    }

    @Test
    fun `expect not found if supplier assessment does not exist`() {
      val referralId = UUID.randomUUID()
      val sessionSummary = "summary"
      val sessionResponse = "response"
      val notifyProbationPractitioner = true
      val submittedBy = authUserFactory.createSP()
      val token = tokenFactory.create()

      val referral = referralFactory.createSent(supplierAssessment = null)
      whenever(userMapper.fromToken(token)).thenReturn(submittedBy)
      whenever(referralService.getSentReferralForUser(eq(referralId), eq(submittedBy))).thenReturn(referral)

      val request = SessionFeedbackRequestDTO(
        late = false,
        sessionSummary = sessionSummary,
        sessionResponse = sessionResponse,
        notifyProbationPractitioner = notifyProbationPractitioner,
      )
      val exception = assertThrows<ResponseStatusException> {
        supplierAssessmentController.recordSessionFeedback(referralId, request, token)
      }
      assertThat(exception.status).isEqualTo(HttpStatus.NOT_FOUND)
      assertThat(exception.message).contains("supplier assessment not found for referral")
    }

    @Test
    fun `expect not found if current appointment does not exist`() {
      val referralId = UUID.randomUUID()
      val sessionSummary = "summary"
      val sessionResponse = "response"
      val notifyProbationPractitioner = true
      val submittedBy = authUserFactory.createSP()
      val token = tokenFactory.create()

      val supplierAssessment = supplierAssessmentFactory.createWithNoAppointment()
      supplierAssessment.referral.supplierAssessment = supplierAssessment

      whenever(userMapper.fromToken(token)).thenReturn(submittedBy)
      whenever(referralService.getSentReferralForUser(eq(referralId), eq(submittedBy))).thenReturn(supplierAssessment.referral)

      val request = SessionFeedbackRequestDTO(
        late = false,
        sessionSummary = sessionSummary,
        sessionResponse = sessionResponse,
        notifyProbationPractitioner = notifyProbationPractitioner,
      )
      val exception = assertThrows<ResponseStatusException> {
        supplierAssessmentController.recordSessionFeedback(referralId, request, token)
      }
      assertThat(exception.status).isEqualTo(HttpStatus.NOT_FOUND)
      assertThat(exception.message).contains("no current appointment exists on supplier assessment for referral")
    }
  }

  @Nested
  inner class RecordAppointmentAttendance {
    @Test
    fun `can record appointment attendance`() {
      val referralId = UUID.randomUUID()
      val attended = Attended.YES
      val submittedBy = authUserFactory.createSP()
      val token = tokenFactory.create()

      val supplierAssessment = supplierAssessmentFactory.create()
      supplierAssessment.referral.supplierAssessment = supplierAssessment
      whenever(userMapper.fromToken(token)).thenReturn(submittedBy)
      whenever(referralService.getSentReferralForUser(eq(referralId), eq(submittedBy))).thenReturn(supplierAssessment.referral)
      whenever(
        appointmentService.recordAppointmentAttendance(
          eq(supplierAssessment!!.currentAppointment!!),
          eq(attended),
          eq(true),
          eq(submittedBy),
        ),
      ).thenReturn(appointmentFactory.create())

      val request = AttendanceFeedbackRequestDTO(attended, true)
      val result = supplierAssessmentController.recordAttendance(referralId, request, token)
      assertThat(result).isNotNull
    }

    @Test
    fun `expect not found if referral does not exist`() {
      val referralId = UUID.randomUUID()
      val attended = Attended.YES
      val submittedBy = authUserFactory.createSP()
      val token = tokenFactory.create()

      whenever(userMapper.fromToken(token)).thenReturn(submittedBy)
      whenever(referralService.getSentReferralForUser(eq(referralId), eq(submittedBy))).thenReturn(null)

      val request = AttendanceFeedbackRequestDTO(attended, true)
      val exception = assertThrows<ResponseStatusException> {
        supplierAssessmentController.recordAttendance(referralId, request, token)
      }
      assertThat(exception.status).isEqualTo(HttpStatus.NOT_FOUND)
      assertThat(exception.message).contains("referral not found")
    }

    @Test
    fun `expect not found if supplier assessment does not exist`() {
      val referralId = UUID.randomUUID()
      val attended = Attended.YES
      val submittedBy = authUserFactory.createSP()
      val token = tokenFactory.create()

      val referral = referralFactory.createSent(supplierAssessment = null)
      whenever(userMapper.fromToken(token)).thenReturn(submittedBy)
      whenever(referralService.getSentReferralForUser(eq(referralId), eq(submittedBy))).thenReturn(referral)

      val request = AttendanceFeedbackRequestDTO(attended, true)
      val exception = assertThrows<ResponseStatusException> {
        supplierAssessmentController.recordAttendance(referralId, request, token)
      }
      assertThat(exception.status).isEqualTo(HttpStatus.NOT_FOUND)
      assertThat(exception.message).contains("supplier assessment not found for referral")
    }

    @Test
    fun `expect not found if current appointment does not exist`() {
      val referralId = UUID.randomUUID()
      val attended = Attended.YES
      val submittedBy = authUserFactory.createSP()
      val token = tokenFactory.create()

      val supplierAssessment = supplierAssessmentFactory.createWithNoAppointment()
      supplierAssessment.referral.supplierAssessment = supplierAssessment

      whenever(userMapper.fromToken(token)).thenReturn(submittedBy)
      whenever(referralService.getSentReferralForUser(eq(referralId), eq(submittedBy))).thenReturn(supplierAssessment.referral)

      val request = AttendanceFeedbackRequestDTO(attended, true)
      val exception = assertThrows<ResponseStatusException> {
        supplierAssessmentController.recordAttendance(referralId, request, token)
      }
      assertThat(exception.status).isEqualTo(HttpStatus.NOT_FOUND)
      assertThat(exception.message).contains("no current appointment exists on supplier assessment for referral")
    }
  }

  @Nested
  inner class SubmitFeedback {
    @Test
    fun `can submit appointment feedback`() {
      val referralId = UUID.randomUUID()
      val submittedBy = authUserFactory.create()
      val token = tokenFactory.create()

      val supplierAssessment = supplierAssessmentFactory.create()
      supplierAssessment.referral.supplierAssessment = supplierAssessment
      whenever(referralService.getSentReferralForUser(eq(referralId), eq(submittedBy))).thenReturn(supplierAssessment.referral)
      whenever(userMapper.fromToken(token)).thenReturn(submittedBy)
      whenever(appointmentService.submitAppointmentFeedback(eq(supplierAssessment!!.currentAppointment!!), eq(submittedBy), eq(AppointmentType.SUPPLIER_ASSESSMENT), eq(null))).thenReturn(appointmentFactory.create())

      val result = supplierAssessmentController.submitFeedback(referralId, token)
      assertThat(result).isNotNull
    }

    @Test
    fun `expect not found if referral does not exist`() {
      val referralId = UUID.randomUUID()
      val submittedBy = authUserFactory.create()
      val token = tokenFactory.create()

      whenever(userMapper.fromToken(token)).thenReturn(submittedBy)
      whenever(referralService.getSentReferralForUser(eq(referralId), eq(submittedBy))).thenReturn(null)

      val exception = assertThrows<ResponseStatusException> {
        supplierAssessmentController.submitFeedback(referralId, token)
      }
      assertThat(exception.status).isEqualTo(HttpStatus.NOT_FOUND)
      assertThat(exception.message).contains("referral not found")
    }

    @Test
    fun `expect not found if supplier assessment does not exist`() {
      val referralId = UUID.randomUUID()
      val submittedBy = authUserFactory.createSP()
      val token = tokenFactory.create()

      val referral = referralFactory.createSent(supplierAssessment = null)
      whenever(userMapper.fromToken(token)).thenReturn(submittedBy)
      whenever(referralService.getSentReferralForUser(eq(referralId), eq(submittedBy))).thenReturn(referral)

      val exception = assertThrows<ResponseStatusException> {
        supplierAssessmentController.submitFeedback(referralId, token)
      }
      assertThat(exception.status).isEqualTo(HttpStatus.NOT_FOUND)
      assertThat(exception.message).contains("supplier assessment not found for referral")
    }

    @Test
    fun `expect not found if current appointment does not exist`() {
      val referralId = UUID.randomUUID()
      val submittedBy = authUserFactory.createSP()
      val token = tokenFactory.create()

      val supplierAssessment = supplierAssessmentFactory.createWithNoAppointment()
      supplierAssessment.referral.supplierAssessment = supplierAssessment

      whenever(userMapper.fromToken(token)).thenReturn(submittedBy)
      whenever(referralService.getSentReferralForUser(eq(referralId), eq(submittedBy))).thenReturn(supplierAssessment.referral)

      val exception = assertThrows<ResponseStatusException> {
        supplierAssessmentController.submitFeedback(referralId, token)
      }
      assertThat(exception.status).isEqualTo(HttpStatus.NOT_FOUND)
      assertThat(exception.message).contains("no current appointment exists on supplier assessment for referral")
    }
  }
}
