package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Appointment
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AppointmentDeliveryType
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AppointmentSessionType
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Attended
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.DeliverySession
import java.time.OffsetDateTime
import java.util.UUID

data class AddressDTO private constructor(
  val firstAddressLine: String,
  val secondAddressLine: String? = null,
  val townOrCity: String? = null,
  val county: String? = null,
  val postCode: String,
) {
  companion object {
    @JvmStatic
    @JsonCreator
    operator fun invoke(firstAddressLine: String, secondAddressLine: String? = null, townOrCity: String? = null, county: String? = null, postCode: String): AddressDTO {
      val normalizedPostCode = postCode.replace("\\s".toRegex(), "").uppercase()
      return AddressDTO(firstAddressLine, secondAddressLine, townOrCity, county, normalizedPostCode)
    }
  }
}

data class UpdateAppointmentDTO(
  val appointmentTime: OffsetDateTime,
  @JsonProperty(required = true) val durationInMinutes: Int,
  val appointmentDeliveryType: AppointmentDeliveryType,
  val sessionType: AppointmentSessionType? = null,
  val appointmentDeliveryAddress: AddressDTO? = null,
  val npsOfficeCode: String? = null,
  val attendanceFeedback: AttendanceFeedbackRequestDTO? = null,
  val sessionFeedback: SessionFeedbackRequestDTO? = null,
)

data class DeliverySessionAppointmentScheduleDetailsRequestDTO(
  val sessionId: Int,
  val appointmentTime: OffsetDateTime,
  @JsonProperty(required = true) val durationInMinutes: Int,
  val appointmentDeliveryType: AppointmentDeliveryType,
  val sessionType: AppointmentSessionType,
  val appointmentDeliveryAddress: AddressDTO? = null,
  val npsOfficeCode: String? = null,
  val attendanceFeedback: AttendanceFeedbackRequestDTO? = null,
  val sessionFeedback: SessionFeedbackRequestDTO? = null,
)

data class AttendanceFeedbackRequestDTO(
  val attended: Attended,
  val attendanceFailureInformation: String?,
)

data class SessionFeedbackRequestDTO(
  val sessionSummary: String,
  val sessionResponse: String,
  val sessionConcerns: String?,
  val notifyProbationPractitioner: Boolean,
)

data class DeliverySessionDTO(
  val id: UUID,
  val sessionNumber: Int,
  val appointmentTime: OffsetDateTime?,
  val durationInMinutes: Int?,
  val appointmentDeliveryType: AppointmentDeliveryType?,
  val oldAppointments: MutableSet<AppointmentDTO> = mutableSetOf(),
  val sessionType: AppointmentSessionType?,
  val npsOfficeCode: String?,
  val appointmentDeliveryAddress: AddressDTO?,
  val appointmentFeedback: AppointmentFeedbackDTO,
  val deliusAppointmentId: Long?,
  val appointmentId: UUID?,
) {
  companion object {
    fun from(session: DeliverySession): DeliverySessionDTO {
      val appointmentDelivery = session.currentAppointment?.appointmentDelivery
      val address = when (appointmentDelivery?.appointmentDeliveryType) {
        AppointmentDeliveryType.IN_PERSON_MEETING_OTHER -> {
          if (appointmentDelivery?.appointmentDeliveryAddress !== null) {
            val address = appointmentDelivery.appointmentDeliveryAddress
            if (address != null) {
              AddressDTO(address.firstAddressLine, address.secondAddressLine ?: "", address.townCity, address.county, address.postCode)
            } else {
              null
            }
          } else {
            null
          }
        }
        else -> null
      }

      return DeliverySessionDTO(
        id = session.id,
        sessionNumber = session.sessionNumber,
        appointmentTime = session.currentAppointment?.appointmentTime,
        durationInMinutes = session.currentAppointment?.durationInMinutes,
        appointmentDeliveryType = session.currentAppointment?.appointmentDelivery?.appointmentDeliveryType,
        sessionType = session.currentAppointment?.appointmentDelivery?.appointmentSessionType,
        appointmentDeliveryAddress = address,
        oldAppointments = session.appointments
          .filterNot { it == session.currentAppointment }
          .mapTo(HashSet()) { AppointmentDTO.from(it) },
        npsOfficeCode = session.currentAppointment?.appointmentDelivery?.npsOfficeCode,
        appointmentFeedback = AppointmentFeedbackDTO.from(session.currentAppointment),
        deliusAppointmentId = session.currentAppointment?.deliusAppointmentId,
        appointmentId = session.currentAppointment?.id,
      )
    }
    fun from(sessions: List<DeliverySession>): List<DeliverySessionDTO> {
      return sessions.map { from(it) }
    }
  }
}

data class AppointmentFeedbackDTO(
  val attendanceFeedback: AttendanceFeedbackDTO,
  val sessionFeedback: SessionFeedbackDTO,
  val submitted: Boolean,
  val submittedBy: AuthUserDTO?,
) {
  companion object {
    fun from(
      appointment: Appointment?,
    ): AppointmentFeedbackDTO {
      return appointment?.let {
        AppointmentFeedbackDTO(
          AttendanceFeedbackDTO.from(appointment),
          SessionFeedbackDTO.from(appointment),
          appointment.appointmentFeedbackSubmittedAt !== null,
          appointment.appointmentFeedbackSubmittedBy?.let { AuthUserDTO.from(it) },
        )
      } ?: AppointmentFeedbackDTO(AttendanceFeedbackDTO(null, null, null, null, null), SessionFeedbackDTO(null, null, null, null, null), false, null)
    }
  }
}

data class AttendanceFeedbackDTO(
  val attended: Attended?,
  val additionalAttendanceInformation: String?,
  val attendanceFailureInformation: String?,
  val submittedAt: OffsetDateTime?,
  val submittedBy: AuthUserDTO?,
) {
  companion object {
    fun from(appointment: Appointment): AttendanceFeedbackDTO {
      return AttendanceFeedbackDTO(attended = appointment.attended, additionalAttendanceInformation = appointment.additionalAttendanceInformation, attendanceFailureInformation = appointment.attendanceFailureInformation, appointment.attendanceSubmittedAt, appointment.attendanceSubmittedBy?.let { AuthUserDTO.from(it) } ?: null)
    }
  }
}

data class SessionFeedbackDTO(
  val behaviourDescription: String?,
  val sessionSummary: String?,
  val sessionResponse: String?,
  val sessionConcerns: String?,
  val notifyProbationPractitioner: Boolean?,
) {
  companion object {
    fun from(appointment: Appointment): SessionFeedbackDTO {
      return SessionFeedbackDTO(appointment.attendanceBehaviour, appointment.sessionSummary, appointment.sessionResponse, appointment.sessionConcerns, appointment.notifyPPOfAttendanceBehaviour)
    }
  }
}
