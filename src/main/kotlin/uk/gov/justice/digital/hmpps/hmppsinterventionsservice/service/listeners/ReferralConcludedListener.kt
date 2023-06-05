package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.listeners

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationListener
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.component.EmailSender
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.component.SNSPublisher
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.EventDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.PersonReference
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.ReferralConcludedEvent
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.exception.AsyncEventExceptionHandling
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AuthUser
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.HMPPSAuthService
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.NotifyService
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.ReferralConcludedState
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.SNSService

@Service
class ReferralConcludedListener(
  private val snsPublisher: SNSPublisher,
) : ApplicationListener<ReferralConcludedEvent>, SNSService {
  @AsyncEventExceptionHandling
  override fun onApplicationEvent(event: ReferralConcludedEvent) {
    val snsEvent = EventDTO(
      "intervention.referral.concluded",
      "The referral has concluded, no more work is needed",
      event.detailUrl,
      event.referral.concludedAt!!,
      mapOf(
        "deliveryState" to event.type.name,
        "referralId" to event.referral.id,
      ),
      PersonReference.crn(event.referral.serviceUserCRN),
    )
    snsPublisher.publish(event.referral.id, AuthUser.interventionsServiceUser, snsEvent)
  }
}

@Service
class ReferralConcludedNotificationListener(
  @Value("\${notify.templates.referral-cancelled}") private val cancelledReferralTemplateID: String,
  private val emailSender: EmailSender,
  private val hmppsAuthService: HMPPSAuthService,
) : ApplicationListener<ReferralConcludedEvent>, NotifyService {
  @AsyncEventExceptionHandling
  override fun onApplicationEvent(event: ReferralConcludedEvent) {
    when (event.type) {
      ReferralConcludedState.CANCELLED -> {
        val userDetails = hmppsAuthService.getUserDetail(event.referral.currentAssignee!!)
        emailSender.sendEmail(
          cancelledReferralTemplateID,
          userDetails.email,
          mapOf(
            "sp_first_name" to userDetails.firstName,
            "referral_number" to event.referral.referenceNumber!!,
          ),
        )
      }
      ReferralConcludedState.PREMATURELY_ENDED,
      ReferralConcludedState.COMPLETED,
      -> {}
    }
  }
}
