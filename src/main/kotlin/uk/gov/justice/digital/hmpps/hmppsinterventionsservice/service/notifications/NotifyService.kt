package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service

import mu.KLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationListener
import org.springframework.stereotype.Service
import org.springframework.web.util.UriComponentsBuilder
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.component.EmailSender
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.ActionPlanEvent
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.ActionPlanEventType
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.AppointmentEvent
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.AppointmentEventType
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.EndOfServiceReportEvent
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.EndOfServiceReportEventType
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.exception.AsyncEventExceptionHandling
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AppointmentType
import java.net.URI

interface NotifyService {
  fun generateResourceUrl(baseURL: String, path: String, vararg args: Any): URI {
    return UriComponentsBuilder.fromHttpUrl(baseURL).path(path).buildAndExpand(*args).toUri()
  }
}

interface ContactablePerson {
  val firstName: String
  val email: String
  val lastName: String
}

@Service
class NotifyActionPlanService(
  @Value("\${notify.templates.action-plan-submitted}") private val actionPlanSubmittedTemplateID: String,
  @Value("\${notify.templates.action-plan-approved}") private val actionPlanApprovedTemplateID: String,
  @Value("\${interventions-ui.baseurl}") private val interventionsUIBaseURL: String,
  @Value("\${interventions-ui.locations.probation-practitioner.action-plan}") private val ppActionPlanLocation: String,
  @Value("\${interventions-ui.locations.service-provider.action-plan}") private val spActionPlanLocation: String,
  private val emailSender: EmailSender,
  private val hmppsAuthService: HMPPSAuthService,
  private val referralService: ReferralService,
) : ApplicationListener<ActionPlanEvent>, NotifyService {

  @AsyncEventExceptionHandling
  override fun onApplicationEvent(event: ActionPlanEvent) {
    when (event.type) {
      ActionPlanEventType.SUBMITTED -> {
        val recipient = referralService.getResponsibleProbationPractitioner(event.actionPlan.referral)
        val location = generateResourceUrl(interventionsUIBaseURL, ppActionPlanLocation, event.actionPlan.referral.id)
        val popFirstName = event.actionPlan.referral.serviceUserData!!.firstName?.lowercase()?.replaceFirstChar { it.uppercase() }
        val popLastName = event.actionPlan.referral.serviceUserData!!.lastName?.lowercase()?.replaceFirstChar { it.uppercase() }

        emailSender.sendEmail(
          actionPlanSubmittedTemplateID,
          recipient.email,
          mapOf(
            "submitterFirstName" to recipient.firstName,
            "referenceNumber" to event.actionPlan.referral.referenceNumber!!,
            "actionPlanUrl" to location.toString(),
            "popFullName" to "$popFirstName $popLastName",
            "crn" to event.actionPlan.referral.serviceUserCRN,
          ),
        )
      }
      ActionPlanEventType.APPROVED -> {
        val recipient = hmppsAuthService.getUserDetail(event.actionPlan.submittedBy!!)
        val location = generateResourceUrl(interventionsUIBaseURL, spActionPlanLocation, event.actionPlan.referral.id)
        emailSender.sendEmail(
          actionPlanApprovedTemplateID,
          recipient.email,
          mapOf(
            "submitterFirstName" to recipient.firstName,
            "referenceNumber" to event.actionPlan.referral.referenceNumber!!,
            "actionPlanUrl" to location.toString(),
          ),
        )
      }
    }
  }
}

@Service
class NotifyEndOfServiceReportService(
  @Value("\${notify.templates.end-of-service-report-submitted}") private val endOfServiceReportSubmittedTemplateID: String,
  @Value("\${interventions-ui.baseurl}") private val interventionsUIBaseURL: String,
  @Value("\${interventions-ui.locations.probation-practitioner.end-of-service-report}") private val ppEndOfServiceReportLocation: String,
  private val emailSender: EmailSender,
  private val referralService: ReferralService,
) : ApplicationListener<EndOfServiceReportEvent>, NotifyService {

  @AsyncEventExceptionHandling
  override fun onApplicationEvent(event: EndOfServiceReportEvent) {
    when (event.type) {
      EndOfServiceReportEventType.SUBMITTED -> {
        val recipient = referralService.getResponsibleProbationPractitioner(event.endOfServiceReport.referral)
        val location = generateResourceUrl(interventionsUIBaseURL, ppEndOfServiceReportLocation, event.endOfServiceReport.id)
        val popFirstName = event.endOfServiceReport.referral.serviceUserData!!.firstName?.lowercase()?.replaceFirstChar { it.uppercase() }
        val popLastName = event.endOfServiceReport.referral.serviceUserData!!.lastName?.lowercase()?.replaceFirstChar { it.uppercase() }

        emailSender.sendEmail(
          endOfServiceReportSubmittedTemplateID,
          recipient.email,
          mapOf(
            "ppFirstName" to recipient.firstName,
            "referralReference" to event.endOfServiceReport.referral.referenceNumber!!,
            "endOfServiceReportLink" to location.toString(),
            "popFullName" to "$popFirstName $popLastName",
            "crn" to event.endOfServiceReport.referral.serviceUserCRN,
          ),
        )
      }
    }
  }
}

@Service
class NotifyAppointmentService(
  @Value("\${notify.templates.appointment-not-attended}") private val appointmentNotAttendedTemplateID: String,
  @Value("\${notify.templates.concerning-behaviour}") private val concerningBehaviourTemplateID: String,
  @Value("\${notify.templates.initial-assessment-scheduled}") private val initialAssessmentScheduledTemplateID: String,
  @Value("\${interventions-ui.baseurl}") private val interventionsUIBaseURL: String,
  @Value("\${interventions-ui.locations.probation-practitioner.intervention-progress}") private val ppInterventionProgressUrl: String,
  @Value("\${interventions-ui.locations.probation-practitioner.supplier-assessment-feedback}") private val ppSAASessionFeedbackLocation: String,
  @Value("\${interventions-ui.locations.probation-practitioner.session-feedback}") private val ppSessionFeedbackLocation: String,
  private val emailSender: EmailSender,
  private val referralService: ReferralService,
) : ApplicationListener<AppointmentEvent>, NotifyService {
  companion object : KLogging()

  @AsyncEventExceptionHandling
  override fun onApplicationEvent(event: AppointmentEvent) {
    if (event.notifyPP) {
      val referral = event.appointment.referral
      val recipient = referralService.getResponsibleProbationPractitioner(referral)

      val sessionFeedbackLocation = when (event.appointmentType) {
        AppointmentType.SUPPLIER_ASSESSMENT -> generateResourceUrl(
          interventionsUIBaseURL,
          ppSAASessionFeedbackLocation,
          event.appointment.referral.id,
        )
        AppointmentType.SERVICE_DELIVERY -> event.deliverySession?.let {
          generateResourceUrl(
            interventionsUIBaseURL,
            ppSessionFeedbackLocation,
            event.appointment.referral.id,
            it.sessionNumber,
            event.appointment.id,
          )
        }
      }

      val popFirstName = referral.serviceUserData!!.firstName?.lowercase()?.replaceFirstChar { it.uppercase() }
      val popLastName = referral.serviceUserData!!.lastName?.lowercase()?.replaceFirstChar { it.uppercase() }
      val popFullName = "$popFirstName $popLastName"

      when (event.type) {
        AppointmentEventType.ATTENDANCE_RECORDED -> {
          emailSender.sendEmail(
            appointmentNotAttendedTemplateID,
            recipient.email,
            mapOf(
              "ppFirstName" to recipient.firstName,
              "popfullname" to popFullName,
              "attendanceUrl" to sessionFeedbackLocation.toString(),
              "crn" to referral.serviceUserCRN,
            ),
          )
        }
        AppointmentEventType.SESSION_FEEDBACK_RECORDED -> {
          emailSender.sendEmail(
            concerningBehaviourTemplateID,
            recipient.email,
            mapOf(
              "ppFirstName" to recipient.firstName,
              "popfullname" to popFullName,
              "sessionUrl" to sessionFeedbackLocation.toString(),
              "crn" to referral.serviceUserCRN,
            ),
          )
        }
        AppointmentEventType.SCHEDULED -> {
          emailSender.sendEmail(
            initialAssessmentScheduledTemplateID,
            recipient.email,
            mapOf(
              "ppFirstName" to recipient.firstName,
              "referenceNumber" to referral.referenceNumber!!,
              "referralUrl" to generateResourceUrl(interventionsUIBaseURL, ppInterventionProgressUrl, referral.id).toString(),
              "popFullName" to popFullName,
              "crn" to referral.serviceUserCRN,
            ),
          )
        }
        else -> {}
      }
    }
  }
}
