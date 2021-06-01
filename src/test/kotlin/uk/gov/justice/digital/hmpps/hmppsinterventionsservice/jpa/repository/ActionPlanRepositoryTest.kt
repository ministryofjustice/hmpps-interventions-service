package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ActionPlan
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Attended
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.SampleData
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ActionPlanFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ActionPlanSessionFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.AppointmentFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.AuthUserFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ReferralFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.RepositoryTest
import java.util.UUID

@RepositoryTest
class ActionPlanRepositoryTest @Autowired constructor(
  val entityManager: TestEntityManager,
  val actionPlanRepository: ActionPlanRepository,
) {
  private val actionPlanFactory = ActionPlanFactory(entityManager)
  private val authUserFactory = AuthUserFactory(entityManager)
  private val referralFactory = ReferralFactory(entityManager)
  private val actionPlanSessionFactory = ActionPlanSessionFactory(entityManager)
  private val appointmentFactory = AppointmentFactory(entityManager)

  @Test
  fun `count number of attended appointments`() {
    val actionPlan = actionPlanFactory.create(numberOfSessions = 4)
    (1..2).forEach {
      actionPlanSessionFactory.createAttended(actionPlan = actionPlan, sessionNumber = it)
    }

    actionPlanSessionFactory.create(actionPlan = actionPlan, sessionNumber = 3)
    actionPlanSessionFactory.create(
      actionPlan = actionPlan, sessionNumber = 3,
      appointment = appointmentFactory.createAttended(attended = Attended.LATE)
    )

    assertThat(actionPlanRepository.countNumberOfAttendedSessions(actionPlanId = actionPlan.id)).isEqualTo(3)
  }

  @Test
  fun `existsByReferralId returns true for duplicate action plans`() {
    val id = UUID.randomUUID()
    actionPlanFactory.create(referral = referralFactory.createSent(id))
    assertThat(actionPlanRepository.existsByReferralId(id)).isTrue
    assertThat(actionPlanRepository.existsByReferralId(UUID.randomUUID())).isFalse
  }

  @Test
  fun `can retrieve an action plan`() {
    val actionPlan = buildAndPersistActionPlan()
    val actionPlanFirstActivity = actionPlan.activities.first()

    val savedPlan = actionPlanRepository.findById(actionPlan.id)

    assertThat(savedPlan.get().numberOfSessions).isEqualTo(actionPlan.numberOfSessions)
    assertThat(savedPlan.get().activities.size).isEqualTo(1)

    val savedPlanFirstActivity = savedPlan.get().activities.first()
    assertThat(actionPlanFirstActivity.desiredOutcome.id).isEqualTo(savedPlanFirstActivity.desiredOutcome.id)
    assertThat(actionPlanFirstActivity.description).isEqualTo(savedPlanFirstActivity.description)
  }

  private fun buildAndPersistActionPlan(): ActionPlan {
    val user = authUserFactory.create(id = "referral_repository_test_user_id")
    val referral = referralFactory.createSent()
    val serviceCategory = SampleData.sampleServiceCategory()
    entityManager.persist(serviceCategory)

    val desiredOutcome = SampleData.sampleDesiredOutcome(description = "Removing Barriers", serviceCategoryId = serviceCategory.id)
    entityManager.persist(desiredOutcome)

    val actionPlan = SampleData.sampleActionPlan(referral = referral, desiredOutcome = desiredOutcome, createdBy = user)
    actionPlanRepository.save(actionPlan)
    entityManager.flush()

    return actionPlan
  }
}
