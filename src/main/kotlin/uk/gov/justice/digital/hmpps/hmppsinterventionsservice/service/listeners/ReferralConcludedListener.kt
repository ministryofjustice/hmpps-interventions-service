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
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.WithdrawalReasonRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.HMPPSAuthService
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.NotifyService
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.ReferralConcludedState
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.ReferralWithdrawalState
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
  @Value("\${notify.templates.referral-pre-ica-withdrawn}") private val withDrawnReferralPreIcaTemplateId: String,
  @Value("\${notify.templates.referral-post-ica-withdrawn}") private val withDrawnReferralPostIcaTemplateId: String,
  @Value("\${notify.templates.referral-withdraw-early}") private val withDrawnReferralWithdrawnEarlyTemplateId: String,
  private val emailSender: EmailSender,
  private val hmppsAuthService: HMPPSAuthService,
  private val withdrawalReasonRepository: WithdrawalReasonRepository,
) : ApplicationListener<ReferralConcludedEvent>, NotifyService {
  @AsyncEventExceptionHandling
  override fun onApplicationEvent(event: ReferralConcludedEvent) {
    val popFirstName = event.referral.serviceUserData!!.firstName?.lowercase()?.replaceFirstChar { it.uppercase() }
    val popLastName = event.referral.serviceUserData!!.lastName?.lowercase()?.replaceFirstChar { it.uppercase() }
    val withdrawalReason = withdrawalReasonRepository.findByCode(event.referral.withdrawalReasonCode!!)
    val userDetails =
      event.referral.currentAssignee?.let { hmppsAuthService.getUserDetail(event.referral.currentAssignee!!) }
    val withdrawnUserDetails = hmppsAuthService.getUserDetail(event.referral.endRequestedBy!!)
    val parameters = mapOf(
      "caseworkerFirstName" to (userDetails?.firstName ?: ""),
      "referralNumber" to event.referral.referenceNumber!!,
      "popFullName" to "$popFirstName $popLastName",
      "changedByName" to withdrawnUserDetails.firstName,
      "reasonForWithdrawal" to withdrawalReason!!.description,
    )
    when (event.type) {
      ReferralConcludedState.CANCELLED -> {
        when (event.referralWithdrawalState) {
          ReferralWithdrawalState.PRE_ICA_WITHDRAWAL -> {
            event.referral.currentAssignee?.let {
              emailSender.sendEmail(
                withDrawnReferralPreIcaTemplateId,
                userDetails!!.email,
                parameters,
              )
            }
          }
          ReferralWithdrawalState.POST_ICA_WITHDRAWAL -> {
            event.referral.currentAssignee?.let {
              emailSender.sendEmail(
                withDrawnReferralPostIcaTemplateId,
                userDetails!!.email,
                parameters,
              )
            }
          }
          ReferralWithdrawalState.POST_ICA_CLOSE_REFERRAL_EARLY -> {
            event.referral.currentAssignee?.let {
              emailSender.sendEmail(
                withDrawnReferralWithdrawnEarlyTemplateId,
                userDetails!!.email,
                parameters,
              )
            }
          }
          else -> {}
        }
      }
      ReferralConcludedState.PREMATURELY_ENDED, ReferralConcludedState.COMPLETED -> {
        when (event.referralWithdrawalState) {
          ReferralWithdrawalState.POST_ICA_WITHDRAWAL -> {
            event.referral.currentAssignee?.let {
              emailSender.sendEmail(
                withDrawnReferralPostIcaTemplateId,
                userDetails!!.email,
                parameters,
              )
            }
          }
          ReferralWithdrawalState.POST_ICA_CLOSE_REFERRAL_EARLY -> {
            event.referral.currentAssignee?.let {
              emailSender.sendEmail(
                withDrawnReferralWithdrawnEarlyTemplateId,
                userDetails!!.email,
                parameters,
              )
            }
          }
          else -> {}
        }
      }
    }
  }
}
