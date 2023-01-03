package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.listeners

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationListener
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.component.EmailSender
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.component.SNSPublisher
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.EventDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.EndOfServiceReportSubmittedEvent
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.exception.AsyncEventExceptionHandling
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.NotifyService
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.ReferralService
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.SNSService

@Service
class EndOfServiceReportDomainEventListener(
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

@Service
class EndOfServiceReportNotificationListener(
  @Value("\${notify.templates.end-of-service-report-submitted}") private val endOfServiceReportSubmittedTemplateID: String,
  @Value("\${interventions-ui.baseurl}") private val interventionsUIBaseURL: String,
  @Value("\${interventions-ui.locations.probation-practitioner.end-of-service-report}") private val ppEndOfServiceReportLocation: String,
  private val emailSender: EmailSender,
  private val referralService: ReferralService
) : ApplicationListener<EndOfServiceReportSubmittedEvent>, NotifyService {
  @AsyncEventExceptionHandling
  override fun onApplicationEvent(event: EndOfServiceReportSubmittedEvent) {
    val referral = referralService.getSentReferral(event.referralId)!!
    val recipient = referralService.getResponsibleProbationPractitioner(referral)
    val location = generateResourceUrl(interventionsUIBaseURL, ppEndOfServiceReportLocation, event.endOfServiceReportId)
    emailSender.sendEmail(
      endOfServiceReportSubmittedTemplateID,
      recipient.email,
      mapOf(
        "ppFirstName" to recipient.firstName,
        "referralReference" to referral.referenceNumber!!,
        "endOfServiceReportLink" to location.toString()
      )
    )
  }
}
