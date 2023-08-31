package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.component

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.config.Code
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.config.ValidationError
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Attended
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.SampleData
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ActionPlanFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.AppointmentFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ReferralFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.SupplierAssessmentFactory
import java.time.OffsetDateTime
import java.util.UUID

class ActionPlanValidatorTest {
  private val actionPlanValidator = ActionPlanValidator()
  private val referralFactory = ReferralFactory()
  private val appointmentFactory = AppointmentFactory()
  private val supplierAssessmentFactory = SupplierAssessmentFactory()
  private val actionPlanFactory = ActionPlanFactory()

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
  fun `update action plan fails validation - number of sessions reduced raises error`() {
    val draftActionPlanId = UUID.randomUUID()
    val referral = SampleData.sampleReferral("X99999", "HARMONY")
    val actionPlanApproved = SampleData.sampleActionPlan(id = draftActionPlanId, referral = referral, numberOfSessions = 2, approvedAt = OffsetDateTime.now())
    referral.actionPlans = mutableListOf(actionPlanApproved)
    val actionPlanUpdate = SampleData.sampleActionPlan(id = draftActionPlanId, referral = referral, numberOfSessions = 1)

    val exception = Assertions.assertThrows(ValidationError::class.java) {
      actionPlanValidator.validateDraftActionPlanUpdate(actionPlanUpdate)
    }
    assertThat(exception.message).isEqualTo("draft action plan update invalid")
    assertThat(exception.errors.size).isEqualTo(1)
    assertThat(exception.errors[0].field).isEqualTo("numberOfSessions")
    assertThat(exception.errors[0].error).isEqualTo(Code.CANNOT_BE_REDUCED)
  }

  @Test
  fun `update action plan does not fail validation - number of sessions not reduced and no error raised`() {
    val draftActionPlanId = UUID.randomUUID()
    val referral = SampleData.sampleReferral("X99999", "HARMONY")
    val actionPlanApproved = SampleData.sampleActionPlan(id = draftActionPlanId, referral = referral, numberOfSessions = 2, approvedAt = OffsetDateTime.now())
    referral.actionPlans = mutableListOf(actionPlanApproved)
    val actionPlanUpdate = SampleData.sampleActionPlan(id = draftActionPlanId, referral = referral, numberOfSessions = 2)

    actionPlanValidator.validateDraftActionPlanUpdate(actionPlanUpdate)

    // no exception should be raised
  }

  @Test
  fun `update action plan does not fail validation - number of sessions not specified and no error raised`() {
    val draftActionPlanId = UUID.randomUUID()
    val referral = SampleData.sampleReferral("X99999", "HARMONY")
    val actionPlanApproved = SampleData.sampleActionPlan(id = draftActionPlanId, referral = referral, numberOfSessions = 2, approvedAt = OffsetDateTime.now())
    referral.actionPlans = mutableListOf(actionPlanApproved)
    val actionPlanUpdate = SampleData.sampleActionPlan(id = draftActionPlanId, referral = referral, numberOfSessions = null)

    actionPlanValidator.validateDraftActionPlanUpdate(actionPlanUpdate)

    // no exception should be raised
  }

  @Test
  fun `update action plan does not fail validation - no previous approved plans and error not raised`() {
    val draftActionPlanId = UUID.randomUUID()
    val referral = SampleData.sampleReferral("X99999", "HARMONY")
    referral.actionPlans = null
    val actionPlanUpdate = SampleData.sampleActionPlan(id = draftActionPlanId, referral = referral, numberOfSessions = 1)

    actionPlanValidator.validateDraftActionPlanUpdate(actionPlanUpdate)

    // no exception should be raised
  }

  @Test
  fun `submit action plan fails validation - number of sessions is empty`() {
    val referral = referralFactory.createSent()
    val appointment = appointmentFactory.create(attended = Attended.YES, appointmentFeedbackSubmittedAt = OffsetDateTime.now())
    val supplierAssessment = supplierAssessmentFactory.create(appointment = appointment)
    referral.supplierAssessment = supplierAssessment
    val actionPlan = actionPlanFactory.create(referral = referral)

    val exception = Assertions.assertThrows(ValidationError::class.java) {
      actionPlanValidator.validateSubmittedActionPlan(actionPlan)
    }
    assertThat(exception.errors.size).isEqualTo(1)
    assertThat(exception.errors[0].field).isEqualTo("numberOfSessions")
    assertThat(exception.errors[0].error).isEqualTo(Code.CANNOT_BE_EMPTY)
  }

  @Test
  fun `submit action plan fails validation if supplier assessment has not been completed`() {
    val actionPlan = actionPlanFactory.create(numberOfSessions = 1)

    val exception = Assertions.assertThrows(ValidationError::class.java) {
      actionPlanValidator.validateSubmittedActionPlan(actionPlan)
    }
    assertThat(exception.errors.size).isEqualTo(1)
    assertThat(exception.errors[0].field).isEqualTo("supplierAssessmentAppointment")
    assertThat(exception.errors[0].error).isEqualTo(Code.APPOINTMENT_MUST_BE_COMPLETED)
  }

  @Test
  fun `submit action plan passes validation if supplier assessment has been completed`() {
    val referral = referralFactory.createSent()
    val appointment = appointmentFactory.create(attended = Attended.YES, appointmentFeedbackSubmittedAt = OffsetDateTime.now())
    val supplierAssessment = supplierAssessmentFactory.create(appointment = appointment)
    referral.supplierAssessment = supplierAssessment
    val actionPlan = actionPlanFactory.create(referral = referral, numberOfSessions = 1)

    actionPlanValidator.validateSubmittedActionPlan(actionPlan)

    // no exception should be raised
  }
}
