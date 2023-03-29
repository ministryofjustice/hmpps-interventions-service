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

  @Test
  fun `referral URN contains the ID`() {
    val uuid = UUID.fromString("70ce237c-9b9c-4028-aae5-52c4622b22ca")
    val referral = referralFactory.createSent(id = uuid)
    assertThat(referral.urn).isEqualTo("urn:hmpps:interventions-referral:70ce237c-9b9c-4028-aae5-52c4622b22ca")
  }

  @Nested
  inner class Assignments {
    @Test
    fun `currentAssignment returns most recent assignment even if list order is wrong`() {
      val currentAssignee = authUserFactory.createSP(userName = "current")
      val oldAssignee = authUserFactory.createSP(userName = "old")
      val referral = referralFactory.createSent(
        assignments = listOf(
          ReferralAssignment(OffsetDateTime.now(), currentAssignee, currentAssignee),
          ReferralAssignment(OffsetDateTime.now().minusHours(2), oldAssignee, oldAssignee),
        ),
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
          ReferralAssignment(OffsetDateTime.now().minusHours(2), oldAssignee, oldAssignee),
        ),
      )

      assertThat(referral.currentAssignee).isEqualTo(currentAssignee)
    }

    @Test
    fun `all but the latest assignment needs to be superseded`() {
      val referral = referralFactory.createSent()
      val assignee = authUserFactory.createSP()

      referral.assignments.clear()
      assertThatNoException().isThrownBy { referral.validateInvariants() }

      val initialAssignment =
        ReferralAssignment(OffsetDateTime.now().plusMinutes(1L), assignee, assignee, superseded = false)
      referral.assignments.add(initialAssignment)
      assertThatNoException().isThrownBy { referral.validateInvariants() }

      val latestAssignment =
        ReferralAssignment(OffsetDateTime.now().plusMinutes(5L), assignee, assignee, superseded = false)
      referral.assignments.add(latestAssignment)
      assertThatExceptionOfType(IllegalStateException::class.java).isThrownBy { referral.validateInvariants() }

      initialAssignment.superseded = false
      latestAssignment.superseded = true
      assertThatExceptionOfType(IllegalStateException::class.java).isThrownBy { referral.validateInvariants() }

      initialAssignment.superseded = true
      latestAssignment.superseded = false
      assertThatNoException().isThrownBy { referral.validateInvariants() }
    }
  }

  @Nested inner class ActionPlans {
    @Test
    fun `approvedActionPlan returns null if there are no approved action plan`() {
      val referral = referralFactory.createSent(
        actionPlans = mutableListOf(
          actionPlanFactory.createSubmitted(),
        ),
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
          actionPlanFactory.createApproved(createdAt = OffsetDateTime.now(), id = correctId),
        ),
      )
      assertThat(referral.approvedActionPlan?.id).isEqualTo(correctId)
    }
  }
}
