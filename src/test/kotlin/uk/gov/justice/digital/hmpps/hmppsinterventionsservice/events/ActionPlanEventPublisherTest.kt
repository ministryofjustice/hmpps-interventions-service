package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.context.ApplicationEventPublisher
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.AbstractPlatformTransactionManager
import org.springframework.transaction.support.DefaultTransactionStatus
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.component.LocationMapper
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.controller.ActionPlanController
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.ActionPlanEventType.SUBMITTED
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.SampleData
import java.net.URI

class ActionPlanEventPublisherTest {

  private val eventPublisher = mock<ApplicationEventPublisher>()
  private val locationMapper = mock<LocationMapper>()
  private val transactionTemplate = TransactionTemplate(object : AbstractPlatformTransactionManager() {
    override fun doGetTransaction(): Any = Any()
    override fun doBegin(transaction: Any, definition: TransactionDefinition) {}
    override fun doCommit(status: DefaultTransactionStatus) {}
    override fun doRollback(status: DefaultTransactionStatus) {}
  })

  @Test
  fun `publishes submitted and approved ID-only events after commit`() {
    val actionPlan = SampleData.sampleActionPlan()
    val uri = URI.create("http://localhost/action-plan/${actionPlan.id}")
    whenever(locationMapper.expandPathToCurrentContextPathUrl("/action-plan/{id}", actionPlan.id)).thenReturn(uri)
    whenever(locationMapper.getPathFromControllerMethod(ActionPlanController::getActionPlan)).thenReturn("/action-plan/{id}")
    val publisher = ActionPlanEventPublisher(eventPublisher, locationMapper)

    transactionTemplate.executeWithoutResult {
      publisher.actionPlanSubmitEvent(actionPlan)
      publisher.actionPlanApprovedEvent(actionPlan)
      verifyNoInteractions(eventPublisher)
    }

    val eventCaptor = argumentCaptor<ActionPlanEvent>()
    verify(eventPublisher, times(2)).publishEvent(eventCaptor.capture())
    val event = eventCaptor.firstValue

    assertThat(event.source).isSameAs(publisher)
    assertThat(event.type).isSameAs(SUBMITTED)
    assertThat(event.actionPlanId).isEqualTo(actionPlan.id)
    assertThat(event.detailUrl).isEqualTo(uri.toString())
    val approvedEvent = eventCaptor.allValues[1]
    assertThat(approvedEvent.type).isEqualTo(ActionPlanEventType.APPROVED)
    assertThat(approvedEvent.actionPlanId).isEqualTo(actionPlan.id)
    assertThat(approvedEvent.detailUrl).isEqualTo(uri.toString())
  }

  @Test
  fun `does not publish either event when the transaction rolls back`() {
    val actionPlan = SampleData.sampleActionPlan()
    whenever(locationMapper.getPathFromControllerMethod(ActionPlanController::getActionPlan)).thenReturn("/action-plan/{id}")
    whenever(locationMapper.expandPathToCurrentContextPathUrl("/action-plan/{id}", actionPlan.id))
      .thenReturn(URI.create("http://localhost/action-plan/${actionPlan.id}"))
    val publisher = ActionPlanEventPublisher(eventPublisher, locationMapper)

    transactionTemplate.executeWithoutResult { status ->
      publisher.actionPlanSubmitEvent(actionPlan)
      publisher.actionPlanApprovedEvent(actionPlan)
      status.setRollbackOnly()
    }

    verifyNoInteractions(eventPublisher)
  }

  @Test
  fun `rejects publication without a transaction`() {
    val publisher = ActionPlanEventPublisher(eventPublisher, locationMapper)
    val actionPlan = SampleData.sampleActionPlan()

    assertThrows<IllegalStateException> { publisher.actionPlanSubmitEvent(actionPlan) }
    assertThrows<IllegalStateException> { publisher.actionPlanApprovedEvent(actionPlan) }
    verifyNoInteractions(eventPublisher)
  }
}
