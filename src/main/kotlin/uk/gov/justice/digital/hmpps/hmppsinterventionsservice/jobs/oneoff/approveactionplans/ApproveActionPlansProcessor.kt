package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jobs.oneoff.approveactionplans

import mu.KLogging
import net.logstash.logback.argument.StructuredArguments.kv
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.item.ItemProcessor
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ActionPlan
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AuthUser
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ActionPlanRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.AuthUserRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.ActionPlanService
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.DeliverySessionService
import java.time.OffsetDateTime

@Component
@JobScope
class ApproveActionPlansProcessor(
  private val actionPlanService: ActionPlanService,
  private val deliverySessionService: DeliverySessionService,
  private val authUserRepository: AuthUserRepository,
  private val actionPlanRepository: ActionPlanRepository,
) : ItemProcessor<ActionPlan, ActionPlan> {
  companion object : KLogging()

  override fun process(actionPlan: ActionPlan): ActionPlan {
    logger.info("processing action plan {} for approval", kv("actionPlanId", actionPlan.id))
    return approveActionPlan(actionPlan)
  }

  fun approveActionPlan(actionPlan: ActionPlan): ActionPlan {
    val user = AuthUser.interventionsServiceUser
    actionPlanService.verifySafeForApproval(actionPlan)

    deliverySessionService.createUnscheduledSessionsForActionPlan(actionPlan)

    actionPlan.approvedAt = OffsetDateTime.now()
    actionPlan.approvedBy = authUserRepository.save(user)

    val approvedActionPlan = actionPlanRepository.save(actionPlan)
//    actionPlanEventPublisher.actionPlanApprovedEvent(approvedActionPlan)
    return actionPlan
  }

//  override fun process(referral: Referral): ActionPlan {
// //    logger.info("processing action plan {} for approval", kv("actionPlanId", referral.currentActionPlan!!.id))
//    return approveActionPlan(referral)
//  }
//
//
//  fun approveActionPlan(referral: Referral): ActionPlan {
//    print(referral)
//    val user = AuthUser.interventionsServiceUser
// //    actionPlanService.verifySafeForApproval(referral.currentActionPlan!!)
//
//    deliverySessionService.createUnscheduledSessionsForActionPlan(referral.currentActionPlan!!)
//
//    referral.currentActionPlan!!.approvedAt = OffsetDateTime.now()
//    referral.currentActionPlan!!.approvedBy = authUserRepository.save(user)
//
//    val approvedActionPlan = actionPlanRepository.save(referral.currentActionPlan!!)
// //    actionPlanEventPublisher.actionPlanApprovedEvent(approvedActionPlan)
//    return referral.currentActionPlan!!
//  }
}
