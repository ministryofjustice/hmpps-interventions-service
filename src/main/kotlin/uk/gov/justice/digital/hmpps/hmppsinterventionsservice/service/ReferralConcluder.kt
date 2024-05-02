package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.ReferralEventPublisher
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Referral
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ActionPlanRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.DeliverySessionRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ReferralRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.findLatestApprovedActionPlan
import java.time.OffsetDateTime

enum class DeliveryState(
  val requiresEndOfServiceReport: Boolean,
  val inProgress: Boolean,
  val concludedState: ReferralConcludedState,
  val referralWithdrawalState: ReferralWithdrawalState,
) {
  NOT_DELIVERING_YET(false, true, ReferralConcludedState.CANCELLED, ReferralWithdrawalState.PRE_ICA_WITHDRAWAL),
  MISSING_FIRST_SUBSTANTIVE_APPOINTMENT(false, true, ReferralConcludedState.CANCELLED, ReferralWithdrawalState.PRE_ICA_WITHDRAWAL),
  IN_PROGRESS(true, true, ReferralConcludedState.PREMATURELY_ENDED, ReferralWithdrawalState.POST_ICA_CLOSE_REFERRAL_EARLY),
  COMPLETED(true, false, ReferralConcludedState.COMPLETED, ReferralWithdrawalState.POST_ICA_WITHDRAWAL),
}

enum class ReferralConcludedState {
  CANCELLED,
  PREMATURELY_ENDED,
  COMPLETED,
}

enum class ReferralWithdrawalState {
  PRE_ICA_WITHDRAWAL,
  POST_ICA_WITHDRAWAL,
  POST_ICA_CLOSE_REFERRAL_EARLY,
}

@Service
@Transactional
class ReferralConcluder(
  val referralRepository: ReferralRepository,
  val deliverySessionRepository: DeliverySessionRepository,
  val actionPlanRepository: ActionPlanRepository,
  val referralEventPublisher: ReferralEventPublisher,
) {
  fun concludeIfEligible(referral: Referral) {
    val deliveryState = deliveryState(referral)
    signalEnding(referral, deliveryState.concludedState)

    val referralWithdrawalState = withdrawalState(referral)
    conclude(referral, deliveryState.concludedState, referralWithdrawalState)
  }

  private fun signalEnding(referral: Referral, concludedState: ReferralConcludedState) {
    referralEventPublisher.referralEndingEvent(referral, concludedState)
  }

  private fun conclude(
    referral: Referral,
    concludedState: ReferralConcludedState,
    referralWithdrawalState: ReferralWithdrawalState,
  ) {
    referral.concludedAt = OffsetDateTime.now()
    referralRepository.save(referral)
    referralEventPublisher.referralConcludedEvent(referral, concludedState, referralWithdrawalState)
  }

  private fun deliveryState(referral: Referral): DeliveryState {
    val approvedActionPlan =
      actionPlanRepository.findLatestApprovedActionPlan(referral.id) ?: return DeliveryState.NOT_DELIVERING_YET
    if (!deliveredFirstSubstantiveAppointment(referral)) return DeliveryState.MISSING_FIRST_SUBSTANTIVE_APPOINTMENT

    val numberOfSessionsWithAttendanceRecord = countSessionsWithAttendanceRecord(referral)
    val allSessionsHaveAttendance = approvedActionPlan.numberOfSessions == numberOfSessionsWithAttendanceRecord
    return if (allSessionsHaveAttendance) {
      DeliveryState.COMPLETED
    } else {
      DeliveryState.IN_PROGRESS
    }
  }

  fun requiresEndOfServiceReportCreation(referral: Referral): Boolean {
    // integration smell: we disable 'creation' for the UI if there is a started end-of-service report
    if (referral.endOfServiceReport != null) return false

    val state = deliveryState(referral)
    val notCancelled = referral.endRequestedAt == null
    if (state.inProgress && notCancelled) return false

    return state.requiresEndOfServiceReport
  }

  fun withdrawalState(referral: Referral): ReferralWithdrawalState {
    val endOfServiceReportRequired = requiresEndOfServiceReportCreation(referral)
    return if (deliverySessionRepository.countNumberOfAttendedSessions(referral.id) == 0) {
      ReferralWithdrawalState.PRE_ICA_WITHDRAWAL
    } else if (deliverySessionRepository.countNumberOfAttendedSessions(referral.id) == 1 && endOfServiceReportRequired) {
      ReferralWithdrawalState.POST_ICA_WITHDRAWAL
    } else {
      ReferralWithdrawalState.POST_ICA_CLOSE_REFERRAL_EARLY
    }
  }

  private fun countSessionsWithAttendanceRecord(referral: Referral): Int {
    return deliverySessionRepository.countNumberOfSessionsWithAttendanceRecord(referral.id)
  }

  private fun countSessionsAttended(referral: Referral): Int {
    return deliverySessionRepository.countNumberOfAttendedSessions(referral.id)
  }

  private fun deliveredFirstSubstantiveAppointment(referral: Referral): Boolean {
    return countSessionsAttended(referral) > 0
  }
}
