package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events

import org.springframework.context.ApplicationEvent
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.component.LocationMapper
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.controller.EndOfServiceReportController
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.EndOfServiceReport
import java.util.UUID

enum class EndOfServiceReportEventType {
  SUBMITTED,
}

class EndOfServiceReportEvent(
  source: Any,
  val type: EndOfServiceReportEventType,
  val endOfServiceReportId: UUID,
  val detailUrl: String,
) : ApplicationEvent(source) {
  override fun toString(): String = "EndOfServiceReportEvent(type=$type, endOfServiceReportId=$endOfServiceReportId, detailUrl='$detailUrl', source=$source)"
}

@Component
class EndOfServiceReportEventPublisher(
  private val applicationEventPublisher: ApplicationEventPublisher,
  private val locationMapper: LocationMapper,
) {
  fun endOfServiceReportSubmittedEvent(endOfServiceReport: EndOfServiceReport) {
    check(TransactionSynchronizationManager.isActualTransactionActive()) {
      "End of service report events must be published within a transaction"
    }
    val event = EndOfServiceReportEvent(
      this,
      EndOfServiceReportEventType.SUBMITTED,
      endOfServiceReport.id,
      getEndOfServiceReportUrl(endOfServiceReport),
    )
    // Listeners must reload committed data, never entities from the publishing session.
    TransactionSynchronizationManager.registerSynchronization(object : TransactionSynchronization {
      override fun afterCommit() {
        applicationEventPublisher.publishEvent(event)
      }
    })
  }

  private fun getEndOfServiceReportUrl(endOfServiceReport: EndOfServiceReport): String {
    val path = locationMapper.getPathFromControllerMethod(EndOfServiceReportController::getEndOfServiceReportById)
    return locationMapper.expandPathToCurrentContextPathUrl(path, endOfServiceReport.id).toString()
  }
}
