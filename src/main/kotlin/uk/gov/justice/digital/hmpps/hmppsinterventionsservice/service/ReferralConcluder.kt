package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.ReferralEventPublisher
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Referral
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.DeliverySessionRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ReferralRepository
import java.time.OffsetDateTime
import javax.transaction.Transactional

enum class DeliveryState(val requiresEndOfServiceReport: Boolean) {
  NOT_DELIVERING_YET(false),
  MISSING_FIRST_SUBSTANTIVE_APPOINTMENT(false),
  IN_PROGRESS(true),
  COMPLETED(true),
}

enum class ReferralConcludedState {
  CANCELLED, PREMATURELY_ENDED, COMPLETED
}

@Service
@Transactional
class ReferralConcluder(
  val referralRepository: ReferralRepository,
  val deliverySessionRepository: DeliverySessionRepository,
  val referralEventPublisher: ReferralEventPublisher,
) {
  fun concludeIfEligible(referral: Referral) {
    val concludedEventType = getConcludedEventType(referral)

    if (concludedEventType != null) {
      referral.concludedAt = OffsetDateTime.now()
      referralRepository.save(referral)
      referralEventPublisher.referralConcludedEvent(referral, concludedEventType)
    }
  }

  private fun deliveryState(referral: Referral): DeliveryState {
    val approvedActionPlan = referral.currentActionPlan ?: return DeliveryState.NOT_DELIVERING_YET
    if (!deliveredFirstSubstantiveAppointment(referral)) return DeliveryState.MISSING_FIRST_SUBSTANTIVE_APPOINTMENT

    val numberOfSessionsWithAttendanceRecord = countSessionsWithAttendanceRecord(referral)
    val allSessionsHaveAttendance = approvedActionPlan.numberOfSessions == numberOfSessionsWithAttendanceRecord
    return if (allSessionsHaveAttendance)
      DeliveryState.COMPLETED
    else
      DeliveryState.IN_PROGRESS
  }

  fun requiresEndOfServiceReportCreation(referral: Referral): Boolean {
    if (referral.endOfServiceReport != null) return false
    return deliveryState(referral).requiresEndOfServiceReport
  }

  private fun getConcludedEventType(referral: Referral): ReferralConcludedState? {
    val hasSubmittedEndOfServiceReport = referral.endOfServiceReport?.submittedAt?.let { true } ?: false

    return when (deliveryState(referral)) {
      DeliveryState.NOT_DELIVERING_YET,
      DeliveryState.MISSING_FIRST_SUBSTANTIVE_APPOINTMENT -> ReferralConcludedState.CANCELLED

      DeliveryState.IN_PROGRESS -> if (hasSubmittedEndOfServiceReport) ReferralConcludedState.PREMATURELY_ENDED else null
      DeliveryState.COMPLETED -> if (hasSubmittedEndOfServiceReport) ReferralConcludedState.COMPLETED else null
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
