package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events

import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.component.LocationMapper
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.controller.EndOfServiceReportController
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.EndOfServiceReport

@Component
class EndOfServiceReportEventPublisher(
  private val applicationEventPublisher: ApplicationEventPublisher,
  private val locationMapper: LocationMapper
) {
  fun publishSubmittedEvent(endOfServiceReport: EndOfServiceReport) {
    applicationEventPublisher.publishEvent(
      EndOfServiceReportSubmittedEvent(
        this,
        endOfServiceReport.referral.id,
        getEndOfServiceReportUrl(endOfServiceReport),
        endOfServiceReport.id,
        endOfServiceReport.submittedAt!!,
        endOfServiceReport.submittedBy!!
      )
    )
  }

  private fun getEndOfServiceReportUrl(endOfServiceReport: EndOfServiceReport): String {
    val path = locationMapper.getPathFromControllerMethod(EndOfServiceReportController::getEndOfServiceReportById)
    return locationMapper.expandPathToCurrentContextPathUrl(path, endOfServiceReport.id).toString()
  }
}
