package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.notifications

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationListener
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.component.EmailSender
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.EndOfServiceReportSubmittedEvent
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.exception.AsyncEventExceptionHandling
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.NotifyService
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.ReferralService

@Service
class NotifyEndOfServiceReportListener(
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
