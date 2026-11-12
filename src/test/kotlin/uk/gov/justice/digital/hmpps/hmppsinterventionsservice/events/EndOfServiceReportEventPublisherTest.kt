package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.context.ApplicationEventPublisher
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.AbstractPlatformTransactionManager
import org.springframework.transaction.support.DefaultTransactionStatus
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.component.LocationMapper
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.controller.EndOfServiceReportController
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.EndOfServiceReportFactory
import java.net.URI

class EndOfServiceReportEventPublisherTest {

  private val eventPublisher = mock<ApplicationEventPublisher>()
  private val locationMapper = mock<LocationMapper>()
  private val endOfServiceReportFactory = EndOfServiceReportFactory()
  private val endOfServiceReport = endOfServiceReportFactory.create()
  private val uri = URI.create("http://localhost/end-of-service-report/${endOfServiceReport.id}")
  private val publisher = EndOfServiceReportEventPublisher(eventPublisher, locationMapper)
  private val transactionTemplate = TransactionTemplate(object : AbstractPlatformTransactionManager() {
    override fun doGetTransaction(): Any = Any()
    override fun doBegin(transaction: Any, definition: TransactionDefinition) {}
    override fun doCommit(status: DefaultTransactionStatus) {}
    override fun doRollback(status: DefaultTransactionStatus) {}
  })

  @BeforeEach
  fun setup() {
    whenever(locationMapper.expandPathToCurrentContextPathUrl("/end-of-service-report/{id}", endOfServiceReport.id))
      .thenReturn(uri)
    whenever(locationMapper.getPathFromControllerMethod(EndOfServiceReportController::getEndOfServiceReportById))
      .thenReturn("/end-of-service-report/{id}")
  }

  @Test
  fun `publishes an ID-only event after the transaction commits`() {
    transactionTemplate.executeWithoutResult {
      publisher.endOfServiceReportSubmittedEvent(endOfServiceReport)
      verifyNoInteractions(eventPublisher)
    }

    val eventCaptor = argumentCaptor<EndOfServiceReportEvent>()
    verify(eventPublisher).publishEvent(eventCaptor.capture())
    val event = eventCaptor.firstValue

    Assertions.assertThat(event.source).isSameAs(publisher)
    Assertions.assertThat(event.type).isSameAs(EndOfServiceReportEventType.SUBMITTED)
    Assertions.assertThat(event.endOfServiceReportId).isEqualTo(endOfServiceReport.id)
    Assertions.assertThat(event.detailUrl).isEqualTo(uri.toString())
  }

  @Test
  fun `does not publish when the transaction rolls back`() {
    transactionTemplate.executeWithoutResult { status ->
      publisher.endOfServiceReportSubmittedEvent(endOfServiceReport)
      status.setRollbackOnly()
    }

    verifyNoInteractions(eventPublisher)
  }

  @Test
  fun `rejects publication without a transaction`() {
    assertThrows<IllegalStateException> {
      publisher.endOfServiceReportSubmittedEvent(endOfServiceReport)
    }
    verifyNoInteractions(eventPublisher)
  }
}
