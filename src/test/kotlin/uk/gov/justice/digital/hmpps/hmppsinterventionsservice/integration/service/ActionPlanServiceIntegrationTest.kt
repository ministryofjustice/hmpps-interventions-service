package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.integration.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.component.ActionPlanValidator
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.ActionPlanEventPublisher
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.DeliverySessionRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.ActionPlanService
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.DeliverySessionService

class ActionPlanServiceIntegrationTest @Autowired constructor(
  val actionPlanValidator: ActionPlanValidator,
) : IntegrationTestBase() {

  private lateinit var actionPlanService: ActionPlanService

  @BeforeEach
  fun beforeEach() {
    actionPlanService = ActionPlanService(
      authUserRepository,
      referralRepository,
      actionPlanRepository,
      this.actionPlanValidator,
      mock<ActionPlanEventPublisher>(),
      mock<DeliverySessionService>(),
      mock<DeliverySessionRepository>(),
    )
  }

  @Test
  fun `can store multiple action plans on a referral and their creation order is preserved`() {
    val referral = setupAssistant.createAssignedReferral()
    assertThat(referral.currentActionPlan).isNull()

    val firstActionPlan = actionPlanService.createDraftActionPlan(referral.id, null, listOf(), referral.currentAssignee!!)
    actionPlanService.createDraftActionPlan(referral.id, 4, listOf(), referral.currentAssignee!!)
    val lastActionPlan = actionPlanService.createDraftActionPlan(referral.id, 10, listOf(), referral.currentAssignee!!)

    // need JPA to reload the referral so it picks up the foreign key associations created above
    val updatedReferral = referralRepository.findByIdOrNull(referral.id)!!

    assertThat(updatedReferral.actionPlans?.size).isEqualTo(3)

    assertThat(updatedReferral.actionPlans?.first()).isEqualTo(firstActionPlan)
    assertThat(updatedReferral.actionPlans?.last()).isEqualTo(lastActionPlan)

    assertThat(updatedReferral.currentActionPlan).isEqualTo(lastActionPlan)
  }
}
