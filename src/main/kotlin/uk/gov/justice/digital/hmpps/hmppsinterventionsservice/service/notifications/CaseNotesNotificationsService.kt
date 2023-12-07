package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.notifications

import mu.KLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationListener
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.component.EmailSender
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.CreateCaseNoteEvent
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.exception.AsyncEventExceptionHandling
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AuthUser
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Referral
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.HMPPSAuthService
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.NotifyService
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.ReferralService
import java.util.UUID

@Service
@Transactional
class CaseNotesNotificationsService(
  @Value("\${notify.templates.case-note-sent}") private val sentTemplate: String,
  @Value("\${notify.templates.case-note-sent-pp}") private val ppSentTemplate: String,
  @Value("\${interventions-ui.baseurl}") private val interventionsUIBaseURL: String,
  @Value("\${interventions-ui.locations.service-provider.case-note-details}") private val spCaseNoteLocation: String,
  @Value("\${interventions-ui.locations.probation-practitioner.case-note-details}") private val ppCaseNoteLocation: String,
  private val emailSender: EmailSender,
  private val referralService: ReferralService,
  private val hmppsAuthService: HMPPSAuthService,
) : ApplicationListener<CreateCaseNoteEvent>, NotifyService {
  companion object : KLogging()

  @AsyncEventExceptionHandling
  override fun onApplicationEvent(event: CreateCaseNoteEvent) {
    referralService.getSentReferralForUser(event.referralId, event.sentBy)?.let { referral ->
      emailAssignedCaseWorker(referral, event.sentBy, event.caseNoteId)
      if (event.sendEmail == null || event.sendEmail) {
        emailResponsibleProbationPractitioner(referral, event.sentBy, event.caseNoteId)
      }
    } ?: run {
      throw RuntimeException("Unable to retrieve referral for id ${event.referralId}")
    }
  }

  private fun emailResponsibleProbationPractitioner(referral: Referral, sender: AuthUser, caseNoteId: UUID) {
    val responsibleOfficer = referralService.getResponsibleProbationPractitioner(referral)
    val senderIsResponsibleOfficer = referralService.isUserTheResponsibleOfficer(responsibleOfficer, sender)

    // if the case note was sent by someone other than the RO, email the RO
    if (!senderIsResponsibleOfficer) {
      val popFirstName = referral.serviceUserData!!.firstName?.lowercase()?.replaceFirstChar { it.uppercase() }
      val popLastName = referral.serviceUserData!!.lastName?.lowercase()?.replaceFirstChar { it.uppercase() }
      val popFullName = "$popFirstName $popLastName"

      emailSender.sendEmail(
        ppSentTemplate,
        responsibleOfficer.email,
        mapOf(
          "pp_first_name" to responsibleOfficer.firstName,
          "referralNumber" to referral.referenceNumber!!,
          "caseNoteUrl" to generateResourceUrl(interventionsUIBaseURL, ppCaseNoteLocation, caseNoteId).toString(),
          "popFullName" to popFullName,
        ),
      )
    }
  }

  private fun emailAssignedCaseWorker(referral: Referral, sender: AuthUser, caseNoteId: UUID) {
    referral.currentAssignee?.let { assignee ->
      val senderIsAssignee = sender == assignee

      // if the case note was sent by someone other than the assignee, email the assignee
      if (!senderIsAssignee) {
        val assigneeDetails = hmppsAuthService.getUserDetail(assignee)
        val popFirstName = referral.serviceUserData!!.firstName?.lowercase()?.replaceFirstChar { it.uppercase() }
        val popLastName = referral.serviceUserData!!.lastName?.lowercase()?.replaceFirstChar { it.uppercase() }
        val popFullName = "$popFirstName $popLastName"

        emailSender.sendEmail(
          sentTemplate,
          assigneeDetails.email,
          mapOf(
            "recipientFirstName" to assigneeDetails.firstName,
            "referralNumber" to referral.referenceNumber!!,
            "caseNoteUrl" to generateResourceUrl(interventionsUIBaseURL, spCaseNoteLocation, caseNoteId).toString(),
            "popFullName" to popFullName,
          ),
        )
      }
    } ?: logger.warn("referral is unassigned; cannot notify case worker about case note creation")
  }
}
