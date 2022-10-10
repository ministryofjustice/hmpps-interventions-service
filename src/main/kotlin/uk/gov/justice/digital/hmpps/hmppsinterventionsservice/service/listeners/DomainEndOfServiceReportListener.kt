package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.listeners

import org.springframework.context.ApplicationListener
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.component.SNSPublisher
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.EventDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.EndOfServiceReportSubmittedEvent
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.exception.AsyncEventExceptionHandling
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.SNSService

@Service
class DomainEndOfServiceReportListener(
  private val snsPublisher: SNSPublisher
) : ApplicationListener<EndOfServiceReportSubmittedEvent>, SNSService {
  @AsyncEventExceptionHandling
  override fun onApplicationEvent(event: EndOfServiceReportSubmittedEvent) {
    val snsEvent = EventDTO(
      "intervention.end-of-service-report.submitted",
      "An end of service report has been submitted",
      event.detailUrl,
      event.submittedAt,
      mapOf(
        "endOfServiceReportId" to event.endOfServiceReportId.toString(),
        "submittedBy" to event.submittedBy.userName
      )
    )
    snsPublisher.publish(event.referralId, event.submittedBy, snsEvent)
  }
}
