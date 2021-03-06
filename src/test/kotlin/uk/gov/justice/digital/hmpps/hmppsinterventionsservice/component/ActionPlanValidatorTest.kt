package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.component

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.config.Code
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.config.ValidationError
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.SampleData
import java.util.UUID

class ActionPlanValidatorTest {
  private val actionPlanValidator = ActionPlanValidator()

  @Test
  fun `update action plan fails validation - number of sessions negative`() {
    val draftActionPlanId = UUID.randomUUID()
    val actionPlanUpdate = SampleData.sampleActionPlan(id = draftActionPlanId, numberOfSessions = -1)

    val exception = Assertions.assertThrows(ValidationError::class.java) {
      actionPlanValidator.validateDraftActionPlanUpdate(actionPlanUpdate)
    }
    assertThat(exception.errors.size).isEqualTo(1)
    assertThat(exception.errors[0].field).isEqualTo("numberOfSessions")
    assertThat(exception.errors[0].error).isEqualTo(Code.CANNOT_BE_NEGATIVE_OR_ZERO)
  }

  @Test
  fun `update action plan fails validation - number of sessions zero`() {
    val draftActionPlanId = UUID.randomUUID()
    val actionPlanUpdate = SampleData.sampleActionPlan(id = draftActionPlanId, numberOfSessions = 0)

    val exception = Assertions.assertThrows(ValidationError::class.java) {
      actionPlanValidator.validateDraftActionPlanUpdate(actionPlanUpdate)
    }
    assertThat(exception.message).isEqualTo("draft action plan update invalid")
    assertThat(exception.errors.size).isEqualTo(1)
    assertThat(exception.errors[0].field).isEqualTo("numberOfSessions")
    assertThat(exception.errors[0].error).isEqualTo(Code.CANNOT_BE_NEGATIVE_OR_ZERO)
  }

  @Test
  fun `submit action plan fails validation - number of sessions is empty`() {
    val draftActionPlanId = UUID.randomUUID()
    val actionPlanUpdate = SampleData.sampleActionPlan(id = draftActionPlanId, numberOfSessions = null)

    val exception = Assertions.assertThrows(ValidationError::class.java) {
      actionPlanValidator.validateSubmittedActionPlan(actionPlanUpdate)
    }
    assertThat(exception.errors.size).isEqualTo(1)
    assertThat(exception.errors[0].field).isEqualTo("numberOfSessions")
    assertThat(exception.errors[0].error).isEqualTo(Code.CANNOT_BE_EMPTY)
  }
}
