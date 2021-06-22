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
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.UpdateAppointmentDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AppointmentDeliveryType
import java.time.OffsetDateTime

internal class ActionPlanSessionValidatorTest {

  private val actionPlanSessionValidator = ActionPlanSessionValidator()

  @Nested
  inner class ValidateUpdateAppointment {

    @Test
    fun `nps office not yet implemented`() {
      var updateAppointmentDTO = UpdateAppointmentDTO(appointmentTime = OffsetDateTime.now(), durationInMinutes = 1, appointmentDeliveryType = AppointmentDeliveryType.IN_PERSON_MEETING_PROBATION_OFFICE)
      var exception = assertThrows<ValidationError> {
        actionPlanSessionValidator.validateUpdateAppointment(updateAppointmentDTO)
      }
      assertThat(exception.errors).containsExactly(
        FieldError("appointmentDeliveryType", Code.NOT_YET_IMPLEMENTED),
      )
    }

    @Nested
    inner class OtherLocationAppointment {
      @Test
      fun `can request valid non nps office appointment`() {
        val updateAppointmentDTO = UpdateAppointmentDTO(appointmentTime = OffsetDateTime.now(), durationInMinutes = 1, appointmentDeliveryType = AppointmentDeliveryType.IN_PERSON_MEETING_OTHER, appointmentDeliveryAddress = AddressDTO("firstline", "secondLine", "town", "county", "A1 1AA"))
        assertDoesNotThrow {
          actionPlanSessionValidator.validateUpdateAppointment(updateAppointmentDTO)
        }
      }

      @Test
      fun `can request valid non nps office appointment with null values for optional fields`() {
        val updateAppointmentDTO = UpdateAppointmentDTO(appointmentTime = OffsetDateTime.now(), durationInMinutes = 1, appointmentDeliveryType = AppointmentDeliveryType.IN_PERSON_MEETING_OTHER, appointmentDeliveryAddress = AddressDTO("firstline", null, null, null, "A1 1AA"))
        assertDoesNotThrow {
          actionPlanSessionValidator.validateUpdateAppointment(updateAppointmentDTO)
        }
      }

      @Nested
      inner class PostCodeValidation {

        private fun createUpdateAppointmentDTO(postCode: String): UpdateAppointmentDTO {
          return UpdateAppointmentDTO(appointmentTime = OffsetDateTime.now(), durationInMinutes = 1, appointmentDeliveryType = AppointmentDeliveryType.IN_PERSON_MEETING_OTHER, appointmentDeliveryAddress = AddressDTO("firstline", "secondLine", "town", "county", postCode))
        }
        @Test
        fun `can request valid non nps office appointment for various postcodes`() {
          assertDoesNotThrow {
            actionPlanSessionValidator.validateUpdateAppointment(createUpdateAppointmentDTO("aa9a 9aa"))
            actionPlanSessionValidator.validateUpdateAppointment(createUpdateAppointmentDTO("a9a 9aa"))
            actionPlanSessionValidator.validateUpdateAppointment(createUpdateAppointmentDTO("a9 9aa"))
            actionPlanSessionValidator.validateUpdateAppointment(createUpdateAppointmentDTO("a99 9aa"))
            actionPlanSessionValidator.validateUpdateAppointment(createUpdateAppointmentDTO("aa9 9aa"))
            actionPlanSessionValidator.validateUpdateAppointment(createUpdateAppointmentDTO("aa99 9aa"))
          }
        }

        @Test
        fun `invalid postcode for non nps office appointment throws validation error`() {
          var exception = assertThrows<ValidationError> {
            actionPlanSessionValidator.validateUpdateAppointment(createUpdateAppointmentDTO("aaa9 9aa"))
          }
          assertThat(exception.errors).containsExactly(
            FieldError("appointmentDeliveryAddress.postCode", Code.INVALID_FORMAT),
          )
        }
      }

      @Test
      fun `empty address for non nps office appointment throws validation error`() {
        var updateAppointmentDTO = UpdateAppointmentDTO(appointmentTime = OffsetDateTime.now(), durationInMinutes = 1, appointmentDeliveryType = AppointmentDeliveryType.IN_PERSON_MEETING_OTHER, appointmentDeliveryAddress = null)
        var exception = assertThrows<ValidationError> {
          actionPlanSessionValidator.validateUpdateAppointment(updateAppointmentDTO)
        }

        assertThat(exception.errors).containsExactly(
          FieldError("appointmentDeliveryAddress", Code.CANNOT_BE_EMPTY),
        )
      }

      @Test
      fun `empty address fields for non nps office appointment throws validation error`() {
        var updateAppointmentDTO = UpdateAppointmentDTO(appointmentTime = OffsetDateTime.now(), durationInMinutes = 1, appointmentDeliveryType = AppointmentDeliveryType.IN_PERSON_MEETING_OTHER, appointmentDeliveryAddress = AddressDTO("", "", "", "", ""))
        var exception = assertThrows<ValidationError> {
          actionPlanSessionValidator.validateUpdateAppointment(updateAppointmentDTO)
        }
        assertThat(exception.errors).containsExactly(
          FieldError("appointmentDeliveryAddress.firstAddressLine", Code.CANNOT_BE_EMPTY),
          FieldError("appointmentDeliveryAddress.postCode", Code.CANNOT_BE_EMPTY),
        )
      }
    }
  }
}
