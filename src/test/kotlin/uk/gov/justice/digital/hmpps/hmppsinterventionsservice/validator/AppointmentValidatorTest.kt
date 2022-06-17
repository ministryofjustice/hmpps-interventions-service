package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.validator

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.config.Code
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.config.FieldError
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.config.ValidationError
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.AddressDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.UpdateAppointmentAttendanceDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.UpdateAppointmentDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AppointmentDeliveryType
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AppointmentSessionType
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Attended
import java.time.OffsetDateTime

internal class AppointmentValidatorTest {

  private val deliverySessionValidator = AppointmentValidator()

  @Nested
  inner class ValidateUpdateAppointment {

    @Nested
    inner class DeliusOfficeLocationAppointment {
      @Test
      fun `can request valid a delius office appointment`() {
        val updateAppointmentDTO = UpdateAppointmentDTO(appointmentTime = OffsetDateTime.now().plusDays(1), durationInMinutes = 1, appointmentDeliveryType = AppointmentDeliveryType.IN_PERSON_MEETING_PROBATION_OFFICE, sessionType = AppointmentSessionType.ONE_TO_ONE, npsOfficeCode = "CRSEXT")
        assertDoesNotThrow {
          deliverySessionValidator.validateUpdateAppointment(updateAppointmentDTO, OffsetDateTime.now())
        }
      }
      @Test
      fun `an empty delius office location throws validation error`() {
        var updateAppointmentDTO = UpdateAppointmentDTO(appointmentTime = OffsetDateTime.now().plusDays(1), durationInMinutes = 1, appointmentDeliveryType = AppointmentDeliveryType.IN_PERSON_MEETING_PROBATION_OFFICE, sessionType = AppointmentSessionType.ONE_TO_ONE)
        var exception = assertThrows<ValidationError> {
          deliverySessionValidator.validateUpdateAppointment(updateAppointmentDTO, OffsetDateTime.now())
        }
        assertThat(exception.errors).containsExactly(
          FieldError("npsOfficeCode", Code.CANNOT_BE_EMPTY),
        )
      }
    }

    @Nested
    inner class OtherLocationAppointment {
      @Test
      fun `can request valid non nps office appointment`() {
        val updateAppointmentDTO = UpdateAppointmentDTO(appointmentTime = OffsetDateTime.now().plusHours(1), durationInMinutes = 1, appointmentDeliveryType = AppointmentDeliveryType.IN_PERSON_MEETING_OTHER, sessionType = AppointmentSessionType.ONE_TO_ONE, appointmentDeliveryAddress = AddressDTO("firstline", "secondLine", "town", "county", "A1 1AA"))
        assertDoesNotThrow {
          deliverySessionValidator.validateUpdateAppointment(updateAppointmentDTO, OffsetDateTime.now())
        }
      }

      @Test
      fun `can request valid non nps office appointment with null values for optional fields`() {
        val updateAppointmentDTO = UpdateAppointmentDTO(appointmentTime = OffsetDateTime.now().plusHours(1), durationInMinutes = 1, appointmentDeliveryType = AppointmentDeliveryType.IN_PERSON_MEETING_OTHER, sessionType = AppointmentSessionType.ONE_TO_ONE, appointmentDeliveryAddress = AddressDTO("firstline", null, null, null, "A1 1AA"))
        assertDoesNotThrow {
          deliverySessionValidator.validateUpdateAppointment(updateAppointmentDTO, OffsetDateTime.now())
        }
      }

      @Nested
      inner class PostCodeValidation {

        private fun createUpdateAppointmentDTO(postCode: String): UpdateAppointmentDTO {
          return UpdateAppointmentDTO(appointmentTime = OffsetDateTime.now().plusHours(1), durationInMinutes = 1, appointmentDeliveryType = AppointmentDeliveryType.IN_PERSON_MEETING_OTHER, sessionType = AppointmentSessionType.ONE_TO_ONE, appointmentDeliveryAddress = AddressDTO("firstline", "secondLine", "town", "county", postCode))
        }
        @Test
        fun `can request valid non nps office appointment for various postcodes`() {
          val today = OffsetDateTime.now()
          assertDoesNotThrow {
            deliverySessionValidator.validateUpdateAppointment(createUpdateAppointmentDTO("aa9a 9aa"), today)
            deliverySessionValidator.validateUpdateAppointment(createUpdateAppointmentDTO("a9a 9aa"), today)
            deliverySessionValidator.validateUpdateAppointment(createUpdateAppointmentDTO("a9 9aa"), today)
            deliverySessionValidator.validateUpdateAppointment(createUpdateAppointmentDTO("a99 9aa"), today)
            deliverySessionValidator.validateUpdateAppointment(createUpdateAppointmentDTO("aa9 9aa"), today)
            deliverySessionValidator.validateUpdateAppointment(createUpdateAppointmentDTO("aa99 9aa"), today)
          }
        }

        @Test
        fun `invalid postcode for non nps office appointment throws validation error`() {
          var exception = assertThrows<ValidationError> {
            deliverySessionValidator.validateUpdateAppointment(createUpdateAppointmentDTO("aaa9 9aa"), OffsetDateTime.now())
          }
          assertThat(exception.errors).containsExactly(
            FieldError("appointmentDeliveryAddress.postCode", Code.INVALID_FORMAT),
          )
        }
      }

      @Test
      fun `empty address for non nps office appointment throws validation error`() {
        var updateAppointmentDTO = UpdateAppointmentDTO(appointmentTime = OffsetDateTime.now().plusHours(1), durationInMinutes = 1, appointmentDeliveryType = AppointmentDeliveryType.IN_PERSON_MEETING_OTHER, sessionType = AppointmentSessionType.ONE_TO_ONE, appointmentDeliveryAddress = null)
        var exception = assertThrows<ValidationError> {
          deliverySessionValidator.validateUpdateAppointment(updateAppointmentDTO, OffsetDateTime.now())
        }

        assertThat(exception.errors).containsExactly(
          FieldError("appointmentDeliveryAddress", Code.CANNOT_BE_EMPTY),
        )
      }

      @Test
      fun `empty address fields for non nps office appointment throws validation error`() {
        var updateAppointmentDTO = UpdateAppointmentDTO(appointmentTime = OffsetDateTime.now().plusHours(1), durationInMinutes = 1, appointmentDeliveryType = AppointmentDeliveryType.IN_PERSON_MEETING_OTHER, sessionType = AppointmentSessionType.ONE_TO_ONE, appointmentDeliveryAddress = AddressDTO("", "", "", "", ""))
        var exception = assertThrows<ValidationError> {
          deliverySessionValidator.validateUpdateAppointment(updateAppointmentDTO, OffsetDateTime.now())
        }
        assertThat(exception.errors).containsExactly(
          FieldError("appointmentDeliveryAddress.firstAddressLine", Code.CANNOT_BE_EMPTY),
          FieldError("appointmentDeliveryAddress.postCode", Code.CANNOT_BE_EMPTY),
        )
      }

      @Test
      fun `past appointment on same day allowed if no attendance reported`() {
        val updateAppointmentDTO = UpdateAppointmentDTO(
          appointmentTime = OffsetDateTime.now().minusMinutes(1), // will fail if the test runs just after midnight
          durationInMinutes = 1,
          appointmentDeliveryType = AppointmentDeliveryType.IN_PERSON_MEETING_PROBATION_OFFICE,
          sessionType = AppointmentSessionType.ONE_TO_ONE,
          npsOfficeCode = "CRSEXT",
          appointmentAttendance = null,
          appointmentBehaviour = null
        )
        assertDoesNotThrow {
          deliverySessionValidator.validateUpdateAppointment(updateAppointmentDTO, OffsetDateTime.now().minusMonths(1))
        }
      }

      @Test
      fun `appointment date before referral date throws validation error`() {
        val updateAppointmentDTO = UpdateAppointmentDTO(
          appointmentTime = OffsetDateTime.now().minusMinutes(1), // will fail if the test runs just after midnight
          durationInMinutes = 1,
          appointmentDeliveryType = AppointmentDeliveryType.IN_PERSON_MEETING_PROBATION_OFFICE,
          sessionType = AppointmentSessionType.ONE_TO_ONE,
          npsOfficeCode = "CRSEXT",
          appointmentAttendance = null,
          appointmentBehaviour = null
        )

        var exception = assertThrows<ValidationError> {
          deliverySessionValidator.validateUpdateAppointment(updateAppointmentDTO, OffsetDateTime.now())
        }

        assertThat(exception.errors).containsExactly(
          FieldError("appointmentTime", Code.APPOINTMENT_TIME_BEFORE_REFERRAL_START_DATE),
        )
      }

      @Test
      fun `appointment date greater than 6 months ahead throws validation error`() {
        val updateAppointmentDTO = UpdateAppointmentDTO(
          appointmentTime = OffsetDateTime.now().plusMonths(7), // will fail if the test runs just after midnight
          durationInMinutes = 1,
          appointmentDeliveryType = AppointmentDeliveryType.IN_PERSON_MEETING_PROBATION_OFFICE,
          sessionType = AppointmentSessionType.ONE_TO_ONE,
          npsOfficeCode = "CRSEXT",
          appointmentAttendance = null,
          appointmentBehaviour = null
        )

        var exception = assertThrows<ValidationError> {
          deliverySessionValidator.validateUpdateAppointment(updateAppointmentDTO, OffsetDateTime.now())
        }

        assertThat(exception.errors).containsExactly(
          FieldError("appointmentTime", Code.APPOINTMENT_TIME_TOO_FAR_IN_FUTURE),
        )
      }

      @Test
      fun `past appointment fails if no attendance reported`() {
        val updateAppointmentDTO = UpdateAppointmentDTO(
          appointmentTime = OffsetDateTime.now().minusDays(1),
          durationInMinutes = 1,
          appointmentDeliveryType = AppointmentDeliveryType.IN_PERSON_MEETING_PROBATION_OFFICE,
          sessionType = AppointmentSessionType.ONE_TO_ONE,
          npsOfficeCode = "CRSEXT",
          appointmentAttendance = null,
          appointmentBehaviour = null
        )
        val exception = assertThrows<ValidationError> {
          deliverySessionValidator.validateUpdateAppointment(updateAppointmentDTO, OffsetDateTime.now().minusMonths(1))
        }
        assertThat(exception.errors).containsExactly(
          FieldError("appointmentAttendance.attended", Code.CANNOT_BE_EMPTY),
        )
      }

      @Test
      fun `past appointment fails if attendance reported as yes but behaviour is not`() {
        val updateAppointmentDTO = UpdateAppointmentDTO(
          appointmentTime = OffsetDateTime.now().minusDays(1),
          durationInMinutes = 1,
          appointmentDeliveryType = AppointmentDeliveryType.IN_PERSON_MEETING_PROBATION_OFFICE,
          sessionType = AppointmentSessionType.ONE_TO_ONE,
          npsOfficeCode = "CRSEXT",
          appointmentAttendance = UpdateAppointmentAttendanceDTO(Attended.YES, null),
          appointmentBehaviour = null
        )
        val exception = assertThrows<ValidationError> {
          deliverySessionValidator.validateUpdateAppointment(updateAppointmentDTO, OffsetDateTime.now().minusMonths(1))
        }
        assertThat(exception.errors).containsExactly(
          FieldError("appointmentBehaviour.notifyProbationPractitioner", Code.CANNOT_BE_EMPTY),
          FieldError("appointmentBehaviour.behaviourDescription", Code.CANNOT_BE_EMPTY),
        )
      }

      @Test
      fun `past appointment fails if attendance reported as late but behaviour is not`() {
        val updateAppointmentDTO = UpdateAppointmentDTO(
          appointmentTime = OffsetDateTime.now().minusDays(1),
          durationInMinutes = 1,
          appointmentDeliveryType = AppointmentDeliveryType.IN_PERSON_MEETING_PROBATION_OFFICE,
          sessionType = AppointmentSessionType.ONE_TO_ONE,
          npsOfficeCode = "CRSEXT",
          appointmentAttendance = UpdateAppointmentAttendanceDTO(Attended.LATE, null),
          appointmentBehaviour = null
        )
        val exception = assertThrows<ValidationError> {
          deliverySessionValidator.validateUpdateAppointment(updateAppointmentDTO, OffsetDateTime.now().minusMonths(1))
        }
        assertThat(exception.errors).containsExactly(
          FieldError("appointmentBehaviour.notifyProbationPractitioner", Code.CANNOT_BE_EMPTY),
          FieldError("appointmentBehaviour.behaviourDescription", Code.CANNOT_BE_EMPTY),
        )
      }
    }
  }
}
