package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service

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
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.DeliverySession
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.NoSessionReasonType
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Referral
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.AppointmentDeliveryRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.AppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.AuthUserRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ReferralRepository
import java.time.OffsetDateTime
import java.util.UUID
import javax.persistence.EntityExistsException
import javax.persistence.EntityNotFoundException

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
    existingAppointment: Appointment?,
    durationInMinutes: Int,
    appointmentTime: OffsetDateTime,
    appointmentType: AppointmentType,
    createdByUser: AuthUser,
    appointmentDeliveryType: AppointmentDeliveryType,
    appointmentSessionType: AppointmentSessionType?,
    appointmentDeliveryAddress: AddressDTO? = null,
    npsOfficeCode: String? = null,
    attended: Attended? = null,
    notifyProbationPractitioner: Boolean? = null,
    didSessionHappen: Boolean? = null,
    late: Boolean? = null,
    lateReason: String? = null,
    noAttendanceInformation: String? = null,
    futureSessionPlans: String? = null,
    noSessionReasonType: NoSessionReasonType? = null,
    noSessionReasonPopAcceptable: String? = null,
    noSessionReasonPopUnacceptable: String? = null,
    noSessionReasonLogistics: String? = null,
    sessionSummary: String? = null,
    sessionResponse: String? = null,
    sessionConcerns: String? = null,
  ): Appointment {
    val appointment = when {
      // an initial appointment is required or an additional appointment is required
      existingAppointment == null || (existingAppointment.attended == Attended.NO && existingAppointment.didSessionHappen == null) || existingAppointment.didSessionHappen == false -> {
        val (deliusAppointmentId, appointmentId) =
          communityAPIBookingService.book(referral, null, appointmentTime, durationInMinutes, appointmentType, npsOfficeCode, attended, notifyProbationPractitioner, didSessionHappen, noSessionReasonType)
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
          noAttendanceInformation,
          notifyProbationPractitioner,
          didSessionHappen,
          late,
          lateReason,
          futureSessionPlans,
          noSessionReasonType,
          noSessionReasonPopAcceptable,
          noSessionReasonPopUnacceptable,
          noSessionReasonLogistics,
          sessionSummary,
          sessionResponse,
          sessionConcerns,
          appointmentType,
          appointmentId,
        )
      }
      // the current appointment needs to be updated
      existingAppointment.didSessionHappen == null -> {
        val (deliusAppointmentId, appointmentId) =
          communityAPIBookingService.book(referral, existingAppointment, appointmentTime, durationInMinutes, appointmentType, npsOfficeCode, attended, notifyProbationPractitioner, didSessionHappen, noSessionReasonType)
        updateAppointment(
          durationInMinutes,
          existingAppointment,
          appointmentTime,
          deliusAppointmentId,
          createdByUser,
          appointmentDeliveryType,
          appointmentSessionType,
          appointmentDeliveryAddress,
          npsOfficeCode,
          attended,
          referral,
          noAttendanceInformation,
          notifyProbationPractitioner,
          didSessionHappen,
          late,
          lateReason,
          futureSessionPlans,
          noSessionReasonType,
          noSessionReasonPopAcceptable,
          noSessionReasonPopUnacceptable,
          noSessionReasonLogistics,
          sessionSummary,
          sessionResponse,
          sessionConcerns,
          appointmentType,
          appointmentId,
        )
      }
      // the appointment has already been attended
      else -> throw IllegalStateException("It is not possible to update an appointment where a session has already happened.")
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
    late: Boolean?,
    lateReason: String?,
    futureSessionPlans: String?,
    noAttendanceInformation: String?,
    noSessionReasonType: NoSessionReasonType?,
    noSessionReasonPopAcceptable: String?,
    noSessionReasonPopUnacceptable: String?,
    noSessionReasonLogistics: String?,
    sessionSummary: String?,
    sessionResponse: String?,
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
    setFeedbackFields(
      appointment,
      late,
      lateReason,
      futureSessionPlans,
      noAttendanceInformation,
      noSessionReasonType,
      noSessionReasonPopAcceptable,
      noSessionReasonPopUnacceptable,
      noSessionReasonLogistics,
      sessionSummary,
      sessionResponse,
      sessionConcerns,
      notifyProbationPractitioner,
      submittedBy,
    )
    return appointmentRepository.save(appointment)
  }

  private fun clearSessionFeedback(
    appointment: Appointment,
  ) {
    appointment.late = null
    appointment.lateReason = null
    appointment.noSessionReasonType = null
    appointment.noSessionReasonPopAcceptable = null
    appointment.noSessionReasonPopUnacceptable = null
    appointment.noSessionReasonLogistics = null
    appointment.futureSessionPlans = null
    appointment.noAttendanceInformation = null
    appointment.sessionSummary = null
    appointment.sessionResponse = null
    appointment.sessionConcerns = null
    appointment.sessionFeedbackSubmittedBy = null
    appointment.sessionFeedbackSubmittedAt = null
    appointment.notifyPPOfAttendanceBehaviour = null
    appointment.attendanceFailureInformation = null
    appointment.attendanceBehaviour = null
    appointment.attendanceBehaviourSubmittedAt = null
    appointment.attendanceBehaviourSubmittedBy = null
    appointment.attendanceSubmittedAt = null
    appointment.attendanceSubmittedBy = null
  }

  fun recordAppointmentAttendance(
    appointment: Appointment,
    attended: Attended,
    didSessionHappen: Boolean,
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
    // This outer check is temporary to prevent losing feedback data in draft appointments. TODO: Remove
    if (!(appointment.didSessionHappen == null && appointment.attended == Attended.YES)) {
      if (didSessionHappen != appointment.didSessionHappen) {
        clearSessionFeedback(appointment)
      } else if (!didSessionHappen) {
        if (attended != appointment.attended) {
          clearSessionFeedback(appointment)
        }
      }
    }

    setAttendanceFields(appointment, attended, didSessionHappen, submittedBy)
    return appointmentRepository.save(appointment)
  }

  fun submitAppointmentFeedback(
    appointment: Appointment,
    submitter: AuthUser,
    appointmentType: AppointmentType,
    deliverySession: DeliverySession? = null,
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
      deliverySession,
    )

    appointmentEventPublisher.sessionFeedbackRecordedEvent(
      appointment,
      appointment.notifyPPOfAttendanceBehaviour ?: false,
      appointmentType,
      deliverySession,
    )

    appointmentEventPublisher.appointmentFeedbackRecordedEvent(
      appointment,
      appointment.notifyPPOfAttendanceBehaviour ?: false,
      appointmentType,
      deliverySession,
    )

    return appointment
  }

  private fun setAttendanceFields(
    appointment: Appointment,
    attended: Attended,
    didSessionHappen: Boolean?,
    submittedBy: AuthUser,
  ) {
    appointment.attended = attended
    appointment.didSessionHappen = didSessionHappen
    appointment.attendanceSubmittedAt = OffsetDateTime.now()
    appointment.attendanceSubmittedBy = authUserRepository.save(submittedBy)
  }

  private fun setFeedbackFields(
    appointment: Appointment,
    late: Boolean?,
    lateReason: String?,
    futureSessionPlans: String?,
    noAttendanceInformation: String?,
    noSessionReasonType: NoSessionReasonType?,
    noSessionReasonPopAcceptable: String?,
    noSessionReasonPopUnacceptable: String?,
    noSessionReasonLogistics: String?,
    sessionSummary: String?,
    sessionResponse: String?,
    sessionConcerns: String?,
    notifyProbationPractitioner: Boolean?,
    submittedBy: AuthUser,
  ) {
    appointment.late = late
    appointment.lateReason = lateReason
    appointment.futureSessionPlans = futureSessionPlans
    appointment.noAttendanceInformation = noAttendanceInformation
    appointment.noSessionReasonType = noSessionReasonType
    appointment.noSessionReasonPopAcceptable = noSessionReasonPopAcceptable
    appointment.noSessionReasonPopUnacceptable = noSessionReasonPopUnacceptable
    appointment.noSessionReasonLogistics = noSessionReasonLogistics
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
    noAttendanceInformation: String?,
    notifyProbationPractitioner: Boolean?,
    didSessionHappen: Boolean?,
    late: Boolean?,
    lateReason: String?,
    futureSessionPlans: String?,
    noSessionReasonType: NoSessionReasonType?,
    noSessionReasonPopAcceptable: String?,
    noSessionReasonPopUnacceptable: String?,
    noSessionReasonLogistics: String?,
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
    setAttendanceAndSessionFeedbackIfHistoricAppointment(
      appointment,
      attended,
      late,
      lateReason,
      futureSessionPlans,
      noAttendanceInformation,
      didSessionHappen,
      noSessionReasonType,
      noSessionReasonPopAcceptable,
      noSessionReasonPopUnacceptable,
      noSessionReasonLogistics,
      sessionSummary,
      sessionResponse,
      sessionConcerns,
      notifyProbationPractitioner,
      createdByUser,
      appointmentType,
    )
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
    noAttendanceInformation: String?,
    notifyProbationPractitioner: Boolean?,
    didSessionHappen: Boolean?,
    late: Boolean?,
    lateReason: String?,
    futureSessionPlans: String?,
    noSessionReasonType: NoSessionReasonType?,
    noSessionReasonPopAcceptable: String?,
    noSessionReasonPopUnacceptable: String?,
    noSessionReasonLogistics: String?,
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
      noAttendanceInformation,
      notifyProbationPractitioner,
      didSessionHappen,
      late,
      lateReason,
      futureSessionPlans,
      noSessionReasonType,
      noSessionReasonPopAcceptable,
      noSessionReasonPopUnacceptable,
      noSessionReasonLogistics,
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
    late: Boolean?,
    lateReason: String?,
    futureSessionPlans: String?,
    noAttendanceInformation: String?,
    didSessionHappen: Boolean?,
    noSessionReasonType: NoSessionReasonType?,
    noSessionReasonPopAcceptable: String?,
    noSessionReasonPopUnacceptable: String?,
    noSessionReasonLogistics: String?,
    sessionSummary: String?,
    sessionResponse: String?,
    sessionConcerns: String?,
    notifyProbationPractitioner: Boolean?,
    updatedBy: AuthUser,
    appointmentType: AppointmentType,
  ) {
    attended?.let {
      setAttendanceFields(appointment, attended, didSessionHappen, updatedBy)
      setFeedbackFields(
        appointment,
        late,
        lateReason,
        futureSessionPlans,
        noAttendanceInformation,
        noSessionReasonType,
        noSessionReasonPopAcceptable,
        noSessionReasonPopUnacceptable,
        noSessionReasonLogistics,
        sessionSummary,
        sessionResponse,
        sessionConcerns,
        notifyProbationPractitioner,
        updatedBy,
      )
      this.submitAppointmentFeedback(appointment, updatedBy, appointmentType)
    }
  }
}
