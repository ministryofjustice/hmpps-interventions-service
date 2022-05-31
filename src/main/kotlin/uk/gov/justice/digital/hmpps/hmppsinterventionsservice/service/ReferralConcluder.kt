package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service

import mu.KotlinLogging
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.ReferralEventPublisher
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.ReferralEventType
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.ReferralEventType.CANCELLED
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.ReferralEventType.COMPLETED
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.ReferralEventType.PREMATURELY_ENDED
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Referral
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ActionPlanRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ReferralRepository
import java.time.OffsetDateTime
import java.util.Objects.nonNull
import javax.transaction.Transactional

@Service
@Transactional
class ReferralConcluder(
  val referralRepository: ReferralRepository,
  val actionPlanRepository: ActionPlanRepository,
  val referralEventPublisher: ReferralEventPublisher,
) {
  companion object {
    private val logger = KotlinLogging.logger {}
  }

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

    val numberOfSessionsAttended = countSessionsAttended(referral)
    val allSessionsAttended = totalNumberOfSessions == numberOfSessionsAttended
    if (allSessionsAttended)
      return true

    val deliveredFirstSubstantiveAppointment = numberOfSessionsAttended > 0
    if (deliveredFirstSubstantiveAppointment && referral.endRequestedAt != null)
      return true

    return false
  }

  private fun getConcludedEventType(referral: Referral): ReferralEventType? {

    val hasActionPlan = nonNull(referral.currentActionPlan)

    val numberOfAttendedSessions = countSessionsAttended(referral)
    val hasAttendedNoSessions = numberOfAttendedSessions == 0

    val totalNumberOfSessions = referral.currentActionPlan?.numberOfSessions ?: 0
    val hasAttendedSomeSessions = totalNumberOfSessions > numberOfAttendedSessions
    val hasAttendedAllSessions = totalNumberOfSessions == numberOfAttendedSessions

    val hasSubmittedEndOfServiceReport = referral.endOfServiceReport?.submittedAt?.let { true } ?: false

    if (!hasActionPlan)
      return CANCELLED

    if (hasAttendedNoSessions)
      return CANCELLED

    if (hasAttendedSomeSessions && hasSubmittedEndOfServiceReport)
      return PREMATURELY_ENDED

    if (hasAttendedAllSessions && hasSubmittedEndOfServiceReport)
      return COMPLETED

    return null
  }

  private fun countSessionsAttended(referral: Referral): Int {
    return actionPlanRepository.countNumberOfAttendedSessions(referral.id)
  }
}
