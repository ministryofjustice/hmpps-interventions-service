package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.assertj.core.api.Assertions.assertThatNoException
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ActionPlanFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.AuthUserFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ReferralFactory
import java.time.OffsetDateTime
import java.util.UUID

internal class ReferralTest {
  private val referralFactory = ReferralFactory()
  private val authUserFactory = AuthUserFactory()
  private val actionPlanFactory = ActionPlanFactory()

  @Nested
  inner class Assignments {
    @Test
    fun `currentAssignment returns most recent assignment even if list order is wrong`() {
      val currentAssignee = authUserFactory.createSP(userName = "current")
      val oldAssignee = authUserFactory.createSP(userName = "old")
      val referral = referralFactory.createSent(
        assignments = listOf(
          ReferralAssignment(OffsetDateTime.now(), currentAssignee, currentAssignee),
          ReferralAssignment(OffsetDateTime.now().minusHours(2), oldAssignee, oldAssignee)
        )
      )

      assertThat(referral.currentAssignment!!.assignedTo).isEqualTo(currentAssignee)
    }

    @Test
    fun `currentAssignee returns most recent assignment assignedTo`() {
      val currentAssignee = authUserFactory.createSP(userName = "current")
      val oldAssignee = authUserFactory.createSP(userName = "old")
      val referral = referralFactory.createSent(
        assignments = listOf(
          ReferralAssignment(OffsetDateTime.now(), currentAssignee, currentAssignee),
          ReferralAssignment(OffsetDateTime.now().minusHours(2), oldAssignee, oldAssignee)
        )
      )

      assertThat(referral.currentAssignee).isEqualTo(currentAssignee)
    }

    @Test
    fun `all but the latest assignment needs to be superseded`() {
      val referral = referralFactory.createSent()
      val assignee = authUserFactory.createSP()

      referral.assignments.clear()
      assertThatNoException().isThrownBy { referral.validateInvariants() }

      val firstAssignment =
        ReferralAssignment(OffsetDateTime.now().plusMinutes(1L), assignee, assignee, superseded = false)
      referral.assignments.add(firstAssignment)
      assertThatNoException().isThrownBy { referral.validateInvariants() }

      val secondAssignment =
        ReferralAssignment(OffsetDateTime.now().plusMinutes(5L), assignee, assignee, superseded = false)
      referral.assignments.add(secondAssignment)
      assertThatExceptionOfType(IllegalStateException::class.java).isThrownBy { referral.validateInvariants() }

      firstAssignment.superseded = true
      secondAssignment.superseded = false
      assertThatNoException().isThrownBy { referral.validateInvariants() }
    }
  }

  @Nested inner class ActionPlans {
    @Test
    fun `approvedActionPlan returns null if there are no approved action plan`() {
      val referral = referralFactory.createSent(
        actionPlans = mutableListOf(
          actionPlanFactory.createSubmitted()
        )
      )

      assertThat(referral.approvedActionPlan).isNull()
    }

    @Test
    fun `approvedActionPlan returns most recent approved action plan`() {
      val correctId = UUID.randomUUID()
      val referral = referralFactory.createSent(
        actionPlans = mutableListOf(
          actionPlanFactory.createSubmitted(),
          actionPlanFactory.createApproved(createdAt = OffsetDateTime.now().minusHours(1)),
          actionPlanFactory.createApproved(createdAt = OffsetDateTime.now(), id = correctId)
        )
      )
      assertThat(referral.approvedActionPlan?.id).isEqualTo(correctId)
    }
  }
}
