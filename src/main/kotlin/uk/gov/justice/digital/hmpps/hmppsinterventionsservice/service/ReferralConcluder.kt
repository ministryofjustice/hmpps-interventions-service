package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.ReferralEventPublisher
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.ReferralEventType
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.ReferralEventType.CANCELLED
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.ReferralEventType.COMPLETED
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.ReferralEventType.PREMATURELY_ENDED
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Referral
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.DeliverySessionRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ReferralRepository
import java.time.OffsetDateTime
import java.util.Objects.nonNull
import javax.transaction.Transactional

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

  fun requiresEndOfServiceReportCreation(referral: Referral): Boolean {
    if (referral.endOfServiceReport != null)
      return false

    val totalNumberOfSessions = referral.currentActionPlan?.numberOfSessions ?: 0
    if (totalNumberOfSessions == 0)
      return false

    val endRequested = referral.endRequestedAt != null
    if (deliveredFirstSubstantiveAppointment(referral) && endRequested)
      return true

    val numberOfSessionsWithAttendanceRecord = countSessionsWithAttendanceRecord(referral)
    val allSessionsHaveAttendance = totalNumberOfSessions == numberOfSessionsWithAttendanceRecord
    if (allSessionsHaveAttendance)
      return true

    return false
  }

  private fun getConcludedEventType(referral: Referral): ReferralEventType? {
    val hasActionPlan = nonNull(referral.currentActionPlan)
    if (!hasActionPlan)
      return CANCELLED

    if (!deliveredFirstSubstantiveAppointment(referral))
      return CANCELLED

    val numberOfSessionsWithAttendanceRecord = countSessionsWithAttendanceRecord(referral)
    val totalNumberOfSessions = referral.currentActionPlan?.numberOfSessions ?: 0
    val someSessionsHaveNoAttendance = totalNumberOfSessions > numberOfSessionsWithAttendanceRecord
    val allSessionsHaveAttendance = totalNumberOfSessions == numberOfSessionsWithAttendanceRecord
    val hasSubmittedEndOfServiceReport = referral.endOfServiceReport?.submittedAt?.let { true } ?: false

    if (someSessionsHaveNoAttendance && hasSubmittedEndOfServiceReport)
      return PREMATURELY_ENDED

    if (allSessionsHaveAttendance && hasSubmittedEndOfServiceReport)
      return COMPLETED

    return null
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
