package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.notifications

import mu.KLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationListener
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.component.EmailSender
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.AuthUserDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.ReferralDetailsDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.ReferralEvent
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.ReferralEventType
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.exception.AsyncEventExceptionHandling
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.AuthUserRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.HMPPSAuthService
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.NotifyService
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import javax.transaction.Transactional

@Service
@Transactional
class ReferralNotificationService(
  @Value("\${notify.templates.referral-sent}") private val referralSentTemplateID: String,
  @Value("\${notify.templates.referral-assigned}") private val referralAssignedTemplateID: String,
  @Value("\${notify.templates.completion-deadline-updated}") private val completionDeadlineUpdatedTemplateID: String,
  @Value("\${interventions-ui.baseurl}") private val interventionsUIBaseURL: String,
  @Value("\${interventions-ui.locations.service-provider.referral-details}") private val spReferralDetailsLocation: String,
  private val emailSender: EmailSender,
  private val hmppsAuthService: HMPPSAuthService,
  private val authUserRepository: AuthUserRepository,
) : ApplicationListener<ReferralEvent>, NotifyService {

  companion object : KLogging()

  @AsyncEventExceptionHandling
  override fun onApplicationEvent(event: ReferralEvent) {
    when (event.type) {
      ReferralEventType.SENT -> {
        val location = generateResourceUrl(interventionsUIBaseURL, spReferralDetailsLocation, event.referral.id)
        val intervention = event.referral.intervention
        val serviceProvider = intervention.dynamicFrameworkContract.primeProvider
        emailSender.sendEmail(
          referralSentTemplateID,
          intervention.incomingReferralDistributionEmail,
          mapOf(
            "organisationName" to serviceProvider.name,
            "referenceNumber" to event.referral.referenceNumber!!,
            "referralUrl" to location.toString(),
          )
        )
      }

      ReferralEventType.ASSIGNED -> {
        val userDetails = hmppsAuthService.getUserDetail(event.referral.currentAssignee!!)
        val location = generateResourceUrl(interventionsUIBaseURL, spReferralDetailsLocation, event.referral.id)
        emailSender.sendEmail(
          referralAssignedTemplateID,
          userDetails.email,
          mapOf(
            "spFirstName" to userDetails.firstName,
            "referenceNumber" to event.referral.referenceNumber!!,
            "referralUrl" to location.toString(),
          )
        )
      }

      ReferralEventType.DETAILS_AMENDED -> {
        handleDetailsChangedEvent(event)
      }
    }
  }

  private fun handleDetailsChangedEvent(event: ReferralEvent) {
    val newDetails = event.data["newDetails"] as ReferralDetailsDTO
    val previousDetails = event.data["previousDetails"] as ReferralDetailsDTO
    val currentAssignee = event.data["currentAssignee"] as AuthUserDTO? ?: return

    val recipient = hmppsAuthService.getUserDetail(currentAssignee)
    val updater = hmppsAuthService.getUserDetail(authUserRepository.findByIdOrNull(newDetails.createdById)!!)
    val frontendUrl = generateResourceUrl(interventionsUIBaseURL, spReferralDetailsLocation, event.referral.id)

    if (newDetails.completionDeadline != previousDetails.completionDeadline) {
      emailSender.sendEmail(
        completionDeadlineUpdatedTemplateID, recipient.email,
        mapOf(
          "caseworkerFirstName" to recipient.firstName,
          "newCompletionDeadline" to formatDate(newDetails.completionDeadline!!),
          "previousCompletionDeadline" to formatDate(previousDetails.completionDeadline!!),
          "changedByName" to "${updater.firstName} ${updater.lastName}",
          "referralDetailsUrl" to frontendUrl.toString(),
        )
      )
    }
  }

  private fun formatDate(date: LocalDate): String {
    return "${date.dayOfMonth} ${date.month.getDisplayName(TextStyle.FULL, Locale.UK)} ${date.year}"
  }
}
