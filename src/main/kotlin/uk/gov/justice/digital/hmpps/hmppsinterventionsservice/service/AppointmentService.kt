package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service

import jakarta.persistence.EntityExistsException
import jakarta.persistence.EntityNotFoundException
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.AddressDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.AppointmentEventPublisher
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Appointment
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AppointmentDelivery
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AppointmentDeliveryAddress
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AppointmentDeliveryType
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AppointmentSessionType
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AppointmentType
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Attended
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AuthUser
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Referral
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.AppointmentDeliveryRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.AppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.AuthUserRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ReferralRepository
import java.time.OffsetDateTime
import java.util.UUID

@Service
@Transactional
class AppointmentService(
  val appointmentRepository: AppointmentRepository,
  val communityAPIBookingService: CommunityAPIBookingService,
  val appointmentDeliveryRepository: AppointmentDeliveryRepository,
  val authUserRepository: AuthUserRepository,
  val appointmentEventPublisher: AppointmentEventPublisher,
  val referralRepository: ReferralRepository,
) {

  // TODO complexity of this method can be reduced
  fun createOrUpdateAppointment(
    referral: Referral,
    appointment: Appointment?,
    durationInMinutes: Int,
    appointmentTime: OffsetDateTime,
    appointmentType: AppointmentType,
    createdByUser: AuthUser,
    appointmentDeliveryType: AppointmentDeliveryType,
    appointmentSessionType: AppointmentSessionType?,
    appointmentDeliveryAddress: AddressDTO? = null,
    npsOfficeCode: String? = null,
    attended: Attended? = null,
    attendanceFailureInformation: String? = null,
    notifyProbationPractitioner: Boolean? = null,
    sessionSummary: String? = null,
    sessionResponse: String? = null,
    sessionConcerns: String? = null,
  ): Appointment {
    val appointment = when {
      // an initial appointment is required or an additional appointment is required
      appointment == null || appointment.attended == Attended.NO -> {
        val (deliusAppointmentId, appointmentId) =
          communityAPIBookingService.book(referral, null, appointmentTime, durationInMinutes, appointmentType, npsOfficeCode, attended, notifyProbationPractitioner)
        createAppointment(
          durationInMinutes,
          appointmentTime,
          deliusAppointmentId,
          createdByUser,
          appointmentDeliveryType,
          appointmentSessionType,
          appointmentDeliveryAddress,
          referral,
          npsOfficeCode,
          attended,
          attendanceFailureInformation,
          notifyProbationPractitioner,
          sessionSummary,
          sessionResponse,
          sessionConcerns,
          appointmentType,
          appointmentId,
        )
      }
      // the current appointment needs to be updated
      appointment.attended == null -> {
        val (deliusAppointmentId, appointmentId) =
          communityAPIBookingService.book(referral, appointment, appointmentTime, durationInMinutes, appointmentType, npsOfficeCode, attended, notifyProbationPractitioner)
        updateAppointment(
          durationInMinutes,
          appointment,
          appointmentTime,
          deliusAppointmentId,
          createdByUser,
          appointmentDeliveryType,
          appointmentSessionType,
          appointmentDeliveryAddress,
          npsOfficeCode,
          attended,
          referral,
          attendanceFailureInformation,
          notifyProbationPractitioner,
          sessionSummary,
          sessionResponse,
          sessionConcerns,
          appointmentType,
          appointmentId,
        )
      }
      // the appointment has already been attended
      else -> throw IllegalStateException("Is it not possible to update an appointment that has already been attended")
    }

    appointmentEventPublisher.appointmentScheduledEvent(appointment, appointmentType)

    return appointment
  }

  fun createOrUpdateAppointmentDeliveryDetails(
    appointment: Appointment,
    appointmentDeliveryType: AppointmentDeliveryType,
    appointmentSessionType: AppointmentSessionType?,
    appointmentDeliveryAddressDTO: AddressDTO?,
    npsOfficeCode: String? = null,
  ) {
    var appointmentDelivery = appointment.appointmentDelivery
    if (appointmentDelivery == null) {
      appointmentDelivery = AppointmentDelivery(appointmentId = appointment.id, appointmentDeliveryType = appointmentDeliveryType, appointmentSessionType = appointmentSessionType, npsOfficeCode = npsOfficeCode)
    } else {
      appointmentDelivery.appointmentDeliveryType = appointmentDeliveryType
      appointmentDelivery.appointmentSessionType = appointmentSessionType
      appointmentDelivery.npsOfficeCode = npsOfficeCode
    }
    appointment.appointmentDelivery = appointmentDelivery
    appointmentRepository.saveAndFlush(appointment)
    if (appointmentDeliveryType == AppointmentDeliveryType.IN_PERSON_MEETING_OTHER) {
      appointmentDelivery.appointmentDeliveryAddress =
        createOrUpdateAppointmentDeliveryAddress(appointmentDelivery, appointmentDeliveryAddressDTO!!)
      appointmentDeliveryRepository.saveAndFlush(appointmentDelivery)
    }
  }

  fun recordSessionFeedback(
    appointment: Appointment,
    sessionSummary: String,
    sessionResponse: String,
    sessionConcerns: String?,
    notifyProbationPractitioner: Boolean,
    submittedBy: AuthUser,
  ): Appointment {
    if (appointment.appointmentFeedbackSubmittedAt != null) {
      throw ResponseStatusException(
        HttpStatus.CONFLICT,
        "Feedback has already been submitted for this appointment [id=${appointment.id}]",
      )
    }
    setFeedbackFields(appointment, sessionSummary, sessionResponse, sessionConcerns, notifyProbationPractitioner, submittedBy)
    return appointmentRepository.save(appointment)
  }

  fun clearSessionFeedback(
    appointment: Appointment,
  ) {
    appointment.sessionSummary = null
    appointment.sessionResponse = null
    appointment.sessionConcerns = null
    appointment.sessionFeedbackSubmittedBy = null
    appointment.sessionFeedbackSubmittedAt = null
    appointment.notifyPPOfAttendanceBehaviour = null
  }

  fun clearAttendanceFeedback(
    appointment: Appointment,
  ) {
    appointment.attendanceFailureInformation = null
  }

  fun recordAppointmentAttendance(
    appointment: Appointment,
    attended: Attended,
    attendanceFailureInformation: String?,
    submittedBy: AuthUser,
  ): Appointment {
    if (appointment.appointmentFeedbackSubmittedAt != null) {
      throw ResponseStatusException(
        HttpStatus.CONFLICT,
        "Feedback has already been submitted for this appointment [id=${appointment.id}]",
      )
    }
    if (appointment.appointmentTime.isAfter(OffsetDateTime.now())) {
      throw ResponseStatusException(
        HttpStatus.BAD_REQUEST,
        "Cannot submit feedback for a future appointment [id=${appointment.id}]",
      )
    }
    if (attended == Attended.NO) {
      clearSessionFeedback(appointment)
    }
    if (attended != Attended.NO) {
      clearAttendanceFeedback(appointment)
    }
    setAttendanceFields(appointment, attended, attendanceFailureInformation, submittedBy)
    return appointmentRepository.save(appointment)
  }

  fun submitAppointmentFeedback(
    appointment: Appointment,
    submitter: AuthUser,
    appointmentType: AppointmentType,
  ): Appointment {
    if (appointment.appointmentFeedbackSubmittedAt != null) {
      throw ResponseStatusException(HttpStatus.CONFLICT, "appointment feedback has already been submitted")
    }

    if (appointment.attendanceSubmittedAt == null) {
      throw ResponseStatusException(
        HttpStatus.UNPROCESSABLE_ENTITY,
        "can't submit feedback unless attendance has been recorded",
      )
    }

    appointment.appointmentFeedbackSubmittedAt = OffsetDateTime.now()
    appointment.appointmentFeedbackSubmittedBy = authUserRepository.save(submitter)
    appointmentRepository.save(appointment)

    appointmentEventPublisher.attendanceRecordedEvent(
      appointment,
      appointment.attended!! == Attended.NO,
      appointmentType,
    )

    if (appointment.sessionFeedbackSubmittedAt != null) { // excluding the case of non attendance
      appointmentEventPublisher.sessionFeedbackRecordedEvent(
        appointment,
        appointment.notifyPPOfAttendanceBehaviour!!,
        appointmentType,
      )
    }

    appointmentEventPublisher.appointmentFeedbackRecordedEvent(
      appointment,
      appointment.notifyPPOfAttendanceBehaviour ?: false,
      appointmentType,
    )

    return appointment
  }

  private fun setAttendanceFields(
    appointment: Appointment,
    attended: Attended,
    attendanceFailureInformation: String?,
    submittedBy: AuthUser,
  ) {
    appointment.attended = attended
    appointment.attendanceFailureInformation = attendanceFailureInformation
    appointment.attendanceSubmittedAt = OffsetDateTime.now()
    appointment.attendanceSubmittedBy = authUserRepository.save(submittedBy)
  }

  private fun setFeedbackFields(
    appointment: Appointment,
    sessionSummary: String,
    sessionResponse: String,
    sessionConcerns: String?,
    notifyProbationPractitioner: Boolean,
    submittedBy: AuthUser,
  ) {
    appointment.sessionSummary = sessionSummary
    appointment.sessionResponse = sessionResponse
    appointment.sessionConcerns = sessionConcerns
    appointment.sessionFeedbackSubmittedAt = OffsetDateTime.now()
    appointment.notifyPPOfAttendanceBehaviour = notifyProbationPractitioner
    appointment.sessionFeedbackSubmittedBy = authUserRepository.save(submittedBy)
  }

  private fun createAppointment(
    durationInMinutes: Int,
    appointmentTime: OffsetDateTime,
    deliusAppointmentId: Long?,
    createdByUser: AuthUser,
    appointmentDeliveryType: AppointmentDeliveryType,
    appointmentSessionType: AppointmentSessionType?,
    appointmentDeliveryAddress: AddressDTO? = null,
    referral: Referral,
    npsOfficeCode: String?,
    attended: Attended?,
    attendanceFailureInformation: String?,
    notifyProbationPractitioner: Boolean?,
    sessionSummary: String?,
    sessionResponse: String?,
    sessionConcerns: String?,
    appointmentType: AppointmentType,
    uuid: UUID? = null,
  ): Appointment {
    val appointment = Appointment(
      id = uuid ?: UUID.randomUUID(),
      appointmentTime = appointmentTime,
      durationInMinutes = durationInMinutes,
      deliusAppointmentId = deliusAppointmentId,
      createdBy = authUserRepository.save(createdByUser),
      createdAt = OffsetDateTime.now(),
      referral = referral,
    )
    setAttendanceAndSessionFeedbackIfHistoricAppointment(appointment, attended, attendanceFailureInformation, sessionSummary, sessionResponse, sessionConcerns, notifyProbationPractitioner, createdByUser, appointmentType)
    appointmentRepository.saveAndFlush(appointment)
    createOrUpdateAppointmentDeliveryDetails(appointment, appointmentDeliveryType, appointmentSessionType, appointmentDeliveryAddress, npsOfficeCode)
    return appointment
  }

  private fun updateAppointment(
    durationInMinutes: Int,
    oldAppointment: Appointment,
    appointmentTime: OffsetDateTime,
    deliusAppointmentId: Long?,
    createdByUser: AuthUser,
    appointmentDeliveryType: AppointmentDeliveryType,
    appointmentSessionType: AppointmentSessionType?,
    appointmentDeliveryAddress: AddressDTO? = null,
    npsOfficeCode: String?,
    attended: Attended?,
    referral: Referral,
    attendanceFailureInformation: String?,
    notifyProbationPractitioner: Boolean?,
    sessionSummary: String?,
    sessionResponse: String?,
    sessionConcerns: String?,
    appointmentType: AppointmentType,
    uuid: UUID?,
  ): Appointment {
    val appointment = createAppointment(
      durationInMinutes,
      appointmentTime,
      deliusAppointmentId,
      createdByUser,
      appointmentDeliveryType,
      appointmentSessionType,
      appointmentDeliveryAddress,
      referral,
      npsOfficeCode,
      attended,
      attendanceFailureInformation,
      notifyProbationPractitioner,
      sessionSummary,
      sessionResponse,
      sessionConcerns,
      appointmentType,
      uuid,
    )
    oldAppointment.supersededByAppointmentId = appointment.id
    oldAppointment.superseded = true
    appointmentRepository.save(oldAppointment)
    appointmentRepository.save(appointment)
    return appointment
  }

  private fun createOrUpdateAppointmentDeliveryAddress(
    appointmentDelivery: AppointmentDelivery,
    appointmentDeliveryAddressDTO: AddressDTO,
  ): AppointmentDeliveryAddress {
    var appointmentDeliveryAddress = appointmentDelivery.appointmentDeliveryAddress
    if (appointmentDeliveryAddress == null) {
      appointmentDeliveryAddress = AppointmentDeliveryAddress(
        appointmentDeliveryId = appointmentDelivery.appointmentId,
        firstAddressLine = appointmentDeliveryAddressDTO.firstAddressLine,
        secondAddressLine = appointmentDeliveryAddressDTO.secondAddressLine,
        townCity = appointmentDeliveryAddressDTO.townOrCity,
        county = appointmentDeliveryAddressDTO.county,
        postCode = appointmentDeliveryAddressDTO.postCode,
      )
    } else {
      appointmentDeliveryAddress.firstAddressLine = appointmentDeliveryAddressDTO.firstAddressLine
      appointmentDeliveryAddress.secondAddressLine = appointmentDeliveryAddressDTO.secondAddressLine
      appointmentDeliveryAddress.townCity = appointmentDeliveryAddressDTO.townOrCity
      appointmentDeliveryAddress.county = appointmentDeliveryAddressDTO.county
      appointmentDeliveryAddress.postCode = appointmentDeliveryAddressDTO.postCode
    }
    return appointmentDeliveryAddress
  }

  fun scheduleNewAppointment(
    sentReferralId: UUID,
    appointmentType: AppointmentType,
    durationInMinutes: Int,
    appointmentTime: OffsetDateTime,
    createdByUser: AuthUser,
    appointmentDeliveryType: AppointmentDeliveryType,
    appointmentSessionType: AppointmentSessionType,
    appointmentDeliveryAddress: AddressDTO? = null,
    npsOfficeCode: String? = null,
  ): Appointment {
    val sentReferral = referralRepository.findByIdAndSentAtIsNotNull(sentReferralId) ?: throw EntityNotFoundException(
      "Sent Referral not found [referralId=$sentReferralId]",
    )
    val (deliusAppointmentId, appointmentId) =
      communityAPIBookingService.book(sentReferral, null, appointmentTime, durationInMinutes, appointmentType, npsOfficeCode)
    val appointment = Appointment(
      id = appointmentId ?: UUID.randomUUID(),
      appointmentTime = appointmentTime,
      durationInMinutes = durationInMinutes,
      deliusAppointmentId = deliusAppointmentId,
      createdBy = authUserRepository.save(createdByUser),
      createdAt = OffsetDateTime.now(),
      referral = sentReferral,
    )
    appointmentRepository.saveAndFlush(appointment)
    createOrUpdateAppointmentDeliveryDetails(appointment, appointmentDeliveryType, appointmentSessionType, appointmentDeliveryAddress, npsOfficeCode)
    appointmentEventPublisher.appointmentScheduledEvent(appointment, appointmentType)
    return appointment
  }

  // TODO: Add updatedBy/updatedAt to Appointment entity?
  fun rescheduleExistingAppointment(
    appointmentId: UUID,
    appointmentType: AppointmentType,
    durationInMinutes: Int,
    appointmentTime: OffsetDateTime,
    appointmentDeliveryType: AppointmentDeliveryType,
    appointmentSessionType: AppointmentSessionType,
    appointmentDeliveryAddress: AddressDTO? = null,
    npsOfficeCode: String? = null,
  ): Appointment {
    val appointment = this.appointmentRepository.findById(appointmentId).orElseThrow { EntityNotFoundException("Appointment not found [appointmentId=$appointmentId]") }
    appointment.appointmentFeedbackSubmittedAt ?.let { throw EntityExistsException("Appointment has already been delivered [appointmentId=$appointmentId]") }
    val (deliusAppointmentId, apptId) =
      communityAPIBookingService.book(appointment.referral, appointment, appointmentTime, durationInMinutes, appointmentType, npsOfficeCode)
    val rescheduled = apptId?.let { appointment.copy(id = apptId) } ?: appointment
    rescheduled.durationInMinutes = durationInMinutes
    rescheduled.appointmentTime = appointmentTime
    rescheduled.deliusAppointmentId = deliusAppointmentId
    appointmentRepository.saveAndFlush(rescheduled)
    createOrUpdateAppointmentDeliveryDetails(rescheduled, appointmentDeliveryType, appointmentSessionType, appointmentDeliveryAddress, npsOfficeCode)
    appointmentEventPublisher.appointmentScheduledEvent(rescheduled, appointmentType)
    return rescheduled
  }

  private fun setAttendanceAndSessionFeedbackIfHistoricAppointment(
    appointment: Appointment,
    attended: Attended?,
    attendanceFailureInformation: String?,
    sessionSummary: String?,
    sessionResponse: String?,
    sessionConcerns: String?,
    notifyProbationPractitioner: Boolean?,
    updatedBy: AuthUser,
    appointmentType: AppointmentType,
  ) {
    attended?.let {
      setAttendanceFields(appointment, attended, attendanceFailureInformation, updatedBy)
      if (Attended.NO != attended) {
        setFeedbackFields(appointment, sessionSummary!!, sessionResponse!!, sessionConcerns, notifyProbationPractitioner!!, updatedBy)
      }
      this.submitAppointmentFeedback(appointment, updatedBy, appointmentType)
    }
  }
}
