package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.ReferralEventPublisher
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.EndOfServiceReport
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Referral
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ActionPlanRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.DeliverySessionRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ReferralRepository
import java.time.OffsetDateTime
import javax.transaction.Transactional

enum class DeliveryState(
  val requiresEndOfServiceReport: Boolean,
  val inProgress: Boolean,
  val concludedState: ReferralConcludedState
) {
  NOT_DELIVERING_YET(false, true, ReferralConcludedState.CANCELLED),
  MISSING_FIRST_SUBSTANTIVE_APPOINTMENT(false, true, ReferralConcludedState.CANCELLED),
  IN_PROGRESS(true, true, ReferralConcludedState.PREMATURELY_ENDED),
  COMPLETED(true, false, ReferralConcludedState.COMPLETED),
}

enum class ReferralEndState {
  AWAITING_END_OF_SERVICE_REPORT, CAN_CONCLUDE
}

enum class ReferralConcludedState {
  CANCELLED, PREMATURELY_ENDED, COMPLETED
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

    val endState = endState(deliveryState, referral.endOfServiceReport)
    if (endState == ReferralEndState.CAN_CONCLUDE) {
      conclude(referral, deliveryState.concludedState)
    }
  }

  private fun signalEnding(referral: Referral, concludedState: ReferralConcludedState) {
    referralEventPublisher.referralEndingEvent(referral, concludedState)
  }

  private fun conclude(referral: Referral, concludedState: ReferralConcludedState) {
    referral.concludedAt = OffsetDateTime.now()
    referralRepository.save(referral)
    referralEventPublisher.referralConcludedEvent(referral, concludedState)
  }

  private fun deliveryState(referral: Referral): DeliveryState {
    val approvedActionPlan =
      actionPlanRepository.findLatestApprovedActionPlan(referral.id) ?: return DeliveryState.NOT_DELIVERING_YET
    if (!deliveredFirstSubstantiveAppointment(referral)) return DeliveryState.MISSING_FIRST_SUBSTANTIVE_APPOINTMENT

    val numberOfSessionsWithAttendanceRecord = countSessionsWithAttendanceRecord(referral)
    val allSessionsHaveAttendance = approvedActionPlan.numberOfSessions == numberOfSessionsWithAttendanceRecord
    return if (allSessionsHaveAttendance)
      DeliveryState.COMPLETED
    else
      DeliveryState.IN_PROGRESS
  }

  fun requiresEndOfServiceReportCreation(referral: Referral): Boolean {
    // integration smell: we disable 'creation' for the UI if there is a started end-of-service report
    if (referral.endOfServiceReport != null) return false

    val state = deliveryState(referral)
    val notCancelled = referral.endRequestedAt == null
    if (state.inProgress && notCancelled) return false

    return state.requiresEndOfServiceReport
  }

  private fun endState(deliveryState: DeliveryState, endOfServiceReport: EndOfServiceReport?): ReferralEndState {
    return if (deliveryState.requiresEndOfServiceReport && endOfServiceReport?.submittedAt == null)
      ReferralEndState.AWAITING_END_OF_SERVICE_REPORT
    else
      ReferralEndState.CAN_CONCLUDE
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
