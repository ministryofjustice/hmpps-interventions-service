package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.validator

import mu.KLogging
import net.logstash.logback.argument.StructuredArguments
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.config.Code
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.config.FieldError
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.config.ValidationError
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.AddressDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.UpdateAppointmentDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AppointmentDeliveryType
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Attended
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Attended.NO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Attended.YES
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.NoSessionReasonType
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.NoSessionReasonType.LOGISTICS
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.NoSessionReasonType.POP_ACCEPTABLE
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.NoSessionReasonType.POP_UNACCEPTABLE
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit

@Component
class AppointmentValidator {
  companion object : KLogging() {
    val postCodeRegex = Regex("""^[A-Z]{1,2}\d[A-Z\d]?\d[A-Z]{2}${'$'}""")
  }

  fun validateUpdateAppointment(updateAppointmentDTO: UpdateAppointmentDTO, referralStartDate: OffsetDateTime?) {
    val errors = mutableListOf<FieldError>()
    val appointmentDeliveryAddress = updateAppointmentDTO.appointmentDeliveryAddress
    when (updateAppointmentDTO.appointmentDeliveryType) {
      AppointmentDeliveryType.IN_PERSON_MEETING_PROBATION_OFFICE -> {
        if (updateAppointmentDTO.npsOfficeCode.isNullOrEmpty()) {
          errors.add(FieldError(field = "npsOfficeCode", error = Code.CANNOT_BE_EMPTY))
        }
      }
      AppointmentDeliveryType.IN_PERSON_MEETING_OTHER -> {
        if (appointmentDeliveryAddress == null) {
          errors.add(FieldError(field = "appointmentDeliveryAddress", error = Code.CANNOT_BE_EMPTY))
        } else {
          validateAddress(appointmentDeliveryAddress, errors)
        }
      }
      else -> {}
    }

    val referralStartDateStartOfDay = referralStartDate?.withHour(0)?.withMinute(0)?.withSecond(1)

    if (referralStartDate != null && updateAppointmentDTO.appointmentTime.isBefore(referralStartDateStartOfDay)) {
      errors.add(FieldError(field = "appointmentTime", error = Code.APPOINTMENT_TIME_BEFORE_REFERRAL_START_DATE))
    }

    val maximumDate =
      OffsetDateTime.now().plus(6, ChronoUnit.MONTHS).withHour(23).withMinute(59).withSecond(59)

    if (referralStartDate != null && updateAppointmentDTO.appointmentTime.isAfter(maximumDate)) {
      errors.add(FieldError(field = "appointmentTime", error = Code.APPOINTMENT_TIME_TOO_FAR_IN_FUTURE))
    }

    validateFeedbackFieldsIfHistoricAppointment(
      updateAppointmentDTO.appointmentTime,
      updateAppointmentDTO.attendanceFeedback?.didSessionHappen,
      updateAppointmentDTO.attendanceFeedback?.attended,
      updateAppointmentDTO.sessionFeedback?.late,
      updateAppointmentDTO.sessionFeedback?.lateReason,
      updateAppointmentDTO.sessionFeedback?.noAttendanceInformation,
      updateAppointmentDTO.sessionFeedback?.noSessionReasonType,
      updateAppointmentDTO.sessionFeedback?.noSessionReasonPopAcceptable,
      updateAppointmentDTO.sessionFeedback?.noSessionReasonPopUnacceptable,
      updateAppointmentDTO.sessionFeedback?.noSessionReasonLogistics,
      updateAppointmentDTO.sessionFeedback?.sessionSummary,
      updateAppointmentDTO.sessionFeedback?.sessionResponse,
      updateAppointmentDTO.sessionFeedback?.sessionConcerns,
      updateAppointmentDTO.sessionFeedback?.notifyProbationPractitioner,
      errors,
    )
    if (errors.isNotEmpty()) {
      throw ValidationError("invalid update session appointment request", errors)
    }
  }

  private fun validateAddress(addressDTO: AddressDTO, errors: MutableList<FieldError>) {
    if (addressDTO.firstAddressLine.isNullOrEmpty()) {
      errors.add(FieldError(field = "appointmentDeliveryAddress.firstAddressLine", error = Code.CANNOT_BE_EMPTY))
    }
    if (addressDTO.postCode.isNullOrEmpty()) {
      errors.add(FieldError(field = "appointmentDeliveryAddress.postCode", error = Code.CANNOT_BE_EMPTY))
    } else if (!postCodeRegex.matches(addressDTO.postCode)) {
      logger.info("invalid postcode format detected", StructuredArguments.kv("postcode", addressDTO.postCode))
      errors.add(FieldError(field = "appointmentDeliveryAddress.postCode", error = Code.INVALID_FORMAT))
    }
  }

  private fun validateFeedbackFieldsIfHistoricAppointment(
    appointmentTime: OffsetDateTime,
    didSessionHappen: Boolean?,
    attended: Attended?,
    late: Boolean?,
    lateReason: String?,
    noAttendanceInformation: String?,
    noSessionReasonType: NoSessionReasonType?,
    noSessionReasonPopAcceptable: String?,
    noSessionReasonPopUnacceptable: String?,
    noSessionReasonLogistics: String?,
    sessionSummary: String?,
    sessionResponse: String?,
    sessionConcerns: String?,
    notifyProbationPractitioner: Boolean?,
    errors: MutableList<FieldError>,
  ) {
    // if the appointment occurred today or on a date in the future, no attendance validation is required
    if (appointmentTime.isAfter(OffsetDateTime.now().with(LocalTime.MIN))) {
      return
    }

    if (didSessionHappen == null) {
      errors.add(FieldError(field = "attendanceFeedback.didSessionHappen", error = Code.CANNOT_BE_EMPTY))
      return
    }

    if (attended == null) {
      errors.add(FieldError(field = "attendanceFeedback.attended", error = Code.CANNOT_BE_EMPTY))
      return
    }

    if (didSessionHappen) {
      if (attended != YES) {
        errors.add(FieldError(field = "attendanceFeedback.attended", error = Code.INVALID_VALUE))
        return
      }
      if (late == null) {
        errors.add(FieldError(field = "sessionFeedback.late", error = Code.CANNOT_BE_EMPTY))
        return
      } else if (late) {
        checkValueSupplied(lateReason, "sessionFeedback.lateReason", Code.CANNOT_BE_EMPTY, errors)
      }
      checkValueSupplied(sessionSummary, "sessionFeedback.sessionSummary", Code.CANNOT_BE_EMPTY, errors)
      checkValueSupplied(sessionResponse, "sessionFeedback.sessionResponse", Code.CANNOT_BE_EMPTY, errors)
      checkValueSupplied(notifyProbationPractitioner, "sessionFeedback.notifyProbationPractitioner", Code.CANNOT_BE_EMPTY, errors)
      if (notifyProbationPractitioner == true) {
        checkValueSupplied(sessionConcerns, "sessionFeedback.sessionConcerns", Code.CANNOT_BE_EMPTY, errors)
      }
    } else {
      when (attended) {
        YES -> {
          when (noSessionReasonType) {
            null -> errors.add(FieldError(field = "sessionFeedback.noSessionReasonType", error = Code.CANNOT_BE_EMPTY))
            POP_ACCEPTABLE -> checkValueSupplied(noSessionReasonPopAcceptable, "sessionFeedback.noSessionReasonPopAcceptable", Code.CANNOT_BE_EMPTY, errors)
            POP_UNACCEPTABLE -> checkValueSupplied(noSessionReasonPopUnacceptable, "sessionFeedback.noSessionReasonPopUnacceptable", Code.CANNOT_BE_EMPTY, errors)
            LOGISTICS -> checkValueSupplied(noSessionReasonLogistics, "sessionFeedback.noSessionReasonLogistics", Code.CANNOT_BE_EMPTY, errors)
          }
          checkValueSupplied(notifyProbationPractitioner, "sessionFeedback.notifyProbationPractitioner", Code.CANNOT_BE_EMPTY, errors)
          if (notifyProbationPractitioner == true) {
            checkValueSupplied(sessionConcerns, "sessionFeedback.sessionConcerns", Code.CANNOT_BE_EMPTY, errors)
          }
        }
        NO -> {
          checkValueSupplied(noAttendanceInformation, "sessionFeedback.noAttendanceInformation", Code.CANNOT_BE_EMPTY, errors)
          checkValueSupplied(notifyProbationPractitioner, "sessionFeedback.notifyProbationPractitioner", Code.CANNOT_BE_EMPTY, errors)
          if (notifyProbationPractitioner == true) {
            checkValueSupplied(sessionConcerns, "sessionFeedback.sessionConcerns", Code.CANNOT_BE_EMPTY, errors)
          }
        }
        else -> {
          return
        }
      }
    }
  }

  fun <T : Any> checkValueSupplied(field: T?, fieldName: String, errorCode: Code, errors: MutableList<FieldError>) {
    field ?: errors.add(FieldError(field = fieldName, error = errorCode))
  }

  fun <T : Any> checkValueNotSupplied(field: T?, fieldName: String, errorCode: Code, errors: MutableList<FieldError>) {
    field?.let { errors.add(FieldError(field = fieldName, error = errorCode)) }
  }
}
