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

    validateAttendanceAndBehaviourFieldsIfHistoricAppointment(
      updateAppointmentDTO.appointmentTime,
      updateAppointmentDTO.attendanceFeedback?.attended,
      updateAppointmentDTO.sessionFeedback?.notifyProbationPractitioner,
      updateAppointmentDTO.sessionFeedback?.sessionSummary,
      updateAppointmentDTO.sessionFeedback?.sessionResponse,
      updateAppointmentDTO.sessionFeedback?.sessionConcerns,
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

  private fun validateAttendanceAndBehaviourFieldsIfHistoricAppointment(
    appointmentTime: OffsetDateTime,
    attended: Attended?,
    notifyProbationPractitioner: Boolean?,
    sessionSummary: String?,
    sessionResponse: String?,
    sessionConcerns: String?,
    errors: MutableList<FieldError>,
  ) {
    // if the appointment occurred today or on a date in the future, no attendance validation is required
    if (appointmentTime.isAfter(OffsetDateTime.now().with(LocalTime.MIN))) {
      return
    }

    if (attended == null) {
      errors.add(FieldError(field = "appointmentAttendance.attended", error = Code.CANNOT_BE_EMPTY))
      return
    }

    when (attended) {
      NO -> {
        checkValueNotSupplied(notifyProbationPractitioner, "sessionFeedback.notifyProbationPractitioner", Code.INVALID_VALUE, errors)
        checkValueNotSupplied(sessionSummary, "sessionFeedback.sessionSummary", Code.INVALID_VALUE, errors)
        checkValueNotSupplied(sessionResponse, "sessionFeedback.sessionResponse", Code.INVALID_VALUE, errors)
      }
      else -> { // YES OR LATE
        checkValueSupplied(notifyProbationPractitioner, "sessionFeedback.notifyProbationPractitioner", Code.CANNOT_BE_EMPTY, errors)
        checkValueSupplied(sessionSummary, "sessionFeedback.sessionSummary", Code.CANNOT_BE_EMPTY, errors)
        checkValueSupplied(sessionResponse, "sessionFeedback.sessionResponse", Code.CANNOT_BE_EMPTY, errors)
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
