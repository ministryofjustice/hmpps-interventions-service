package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ActionPlan
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.SampleData
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.AuthUserFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ReferralFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.RepositoryTest
import java.time.OffsetDateTime

@RepositoryTest
class ActionPlanRepositoryTest @Autowired constructor(
  val entityManager: TestEntityManager,
  val actionPlanRepository: ActionPlanRepository,
) {
  private val authUserFactory = AuthUserFactory(entityManager)
  private val referralFactory = ReferralFactory(entityManager)

  @Test
  fun `can retrieve an action plan`() {
    val actionPlan = buildAndPersistActionPlan()
    val actionPlanFirstActivity = actionPlan.activities.first()

    val savedPlan = actionPlanRepository.findById(actionPlan.id)

    assertThat(savedPlan.get().numberOfSessions).isEqualTo(actionPlan.numberOfSessions)
    assertThat(savedPlan.get().activities.size).isEqualTo(1)

    val savedPlanFirstActivity = savedPlan.get().activities.first()
    assertThat(actionPlanFirstActivity.description).isEqualTo(savedPlanFirstActivity.description)
  }

  @Test
  fun `finds latest approved action plan`() {
    val referral = referralFactory.createSent()
    val user = authUserFactory.create(id = "referral_repository_test_user_id")

    val oldestActionPlan = SampleData.sampleActionPlan(referral = referral, createdBy = user, approvedAt = OffsetDateTime.parse("2020-12-04T10:42:43+00:00"))
    val newerActionPlan = SampleData.sampleActionPlan(referral = referral, createdBy = user, approvedAt = OffsetDateTime.parse("2020-12-05T10:42:43+00:00"))
    val latestActionPlan = SampleData.sampleActionPlan(referral = referral, createdBy = user, approvedAt = OffsetDateTime.parse("2020-12-06T10:42:43+00:00"))
    actionPlanRepository.saveAll(listOf(oldestActionPlan, newerActionPlan, latestActionPlan))

    val result = actionPlanRepository.findLatestApprovedActionPlan(referral.id)

    assertThat(result?.approvedAt).isEqualTo(latestActionPlan.approvedAt)
  }

  private fun buildAndPersistActionPlan(): ActionPlan {
    val user = authUserFactory.create(id = "referral_repository_test_user_id")
    val referral = referralFactory.createSent()
    val serviceCategory = SampleData.sampleServiceCategory()
    entityManager.persist(serviceCategory)

    val desiredOutcome =
      SampleData.sampleDesiredOutcome(description = "Removing Barriers", serviceCategoryId = serviceCategory.id)
    entityManager.persist(desiredOutcome)

    val actionPlan = SampleData.sampleActionPlan(referral = referral, createdBy = user)
    actionPlanRepository.save(actionPlan)
    entityManager.flush()

    return actionPlan
  }
}
