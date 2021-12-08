package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events

import org.springframework.context.ApplicationEvent
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.component.LocationMapper
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.controller.EndOfServiceReportController
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.EndOfServiceReport

enum class EndOfServiceReportEventType {
  SUBMITTED,
}

class EndOfServiceReportEvent(
  source: Any,
  val type: EndOfServiceReportEventType,
  val endOfServiceReport: EndOfServiceReport,
  val detailUrl: String
) :
  TraceableEvent(source) {
  override fun toString(): String {
    return "EndOfServiceReportEvent(type=$type, referralId=${endOfServiceReport.referral.id}, detailUrl='$detailUrl', source=$source)"
  }
}

@Component
class EndOfServiceReportEventPublisher(
  private val eventPublisher: TracePropagatingEventPublisher,
  private val locationMapper: LocationMapper
) {
  fun endOfServiceReportSubmittedEvent(endOfServiceReport: EndOfServiceReport) {
    eventPublisher.publishEvent(
      EndOfServiceReportEvent(
        this, EndOfServiceReportEventType.SUBMITTED,
        endOfServiceReport, getEndOfServiceReportUrl(endOfServiceReport)
      )
    )
  }

  private fun getEndOfServiceReportUrl(endOfServiceReport: EndOfServiceReport): String {
    val path = locationMapper.getPathFromControllerMethod(EndOfServiceReportController::getEndOfServiceReportById)
    return locationMapper.expandPathToCurrentContextPathUrl(path, endOfServiceReport.id).toString()
  }
}
