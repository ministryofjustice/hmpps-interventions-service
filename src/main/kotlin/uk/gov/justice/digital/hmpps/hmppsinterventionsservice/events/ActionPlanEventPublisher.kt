package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events

import org.springframework.context.ApplicationEvent
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.component.LocationMapper
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.controller.ActionPlanController
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ActionPlan
import java.util.UUID

enum class ActionPlanEventType {
  SUBMITTED,
  APPROVED,
}

class ActionPlanEvent(source: Any, val type: ActionPlanEventType, val actionPlanId: UUID, val detailUrl: String) : ApplicationEvent(source) {
  override fun toString(): String = "ActionPlanEvent(type=$type, actionPlanId=$actionPlanId, detailUrl='$detailUrl', source=$source)"
}

@Component
class ActionPlanEventPublisher(
  private val applicationEventPublisher: ApplicationEventPublisher,
  private val locationMapper: LocationMapper,
) {

  fun actionPlanSubmitEvent(actionPlan: ActionPlan) {
    publishAfterCommit(actionPlan, ActionPlanEventType.SUBMITTED)
  }

  fun actionPlanApprovedEvent(actionPlan: ActionPlan) {
    publishAfterCommit(actionPlan, ActionPlanEventType.APPROVED)
  }

  private fun publishAfterCommit(actionPlan: ActionPlan, type: ActionPlanEventType) {
    check(TransactionSynchronizationManager.isActualTransactionActive()) {
      "Action plan events must be published within a transaction"
    }
    val event = ActionPlanEvent(this, type, actionPlan.id, createDetailUrl(actionPlan))
    TransactionSynchronizationManager.registerSynchronization(object : TransactionSynchronization {
      override fun afterCommit() {
        applicationEventPublisher.publishEvent(event)
      }
    })
  }

  private fun createDetailUrl(actionPlan: ActionPlan): String {
    val path = locationMapper.getPathFromControllerMethod(ActionPlanController::getActionPlan)
    return locationMapper.expandPathToCurrentContextPathUrl(path, actionPlan.id).toString()
  }
}
