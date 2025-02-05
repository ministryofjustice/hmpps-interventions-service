package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jobs.scheduled.concludereferrals

import mu.KLogging
import net.logstash.logback.argument.StructuredArguments.kv
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.item.ItemProcessor
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Referral
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.DeliverySessionRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.ReferralConcluder

@Component
@JobScope
class ConcludeReferralsProcessor(
  private val referralConcluder: ReferralConcluder,
  private val deliverySessionRepository: DeliverySessionRepository,
) : ItemProcessor<Referral, Referral> {
  companion object : KLogging()

  override fun process(referral: Referral): Referral {
    logger.info("processing referral {} for conclude", kv("referralId", referral.id))
    val numberOfSessionsWithAttendanceRecord = deliverySessionRepository.countNumberOfSessionsWithAttendanceRecord(referral.id)
    val numberOfSessionsWithNoAttendance = deliverySessionRepository.countNumberOfNotAttendedSessions(referral.id)
    if (numberOfSessionsWithAttendanceRecord == numberOfSessionsWithNoAttendance) {
      referralConcluder.concludeIfEligible(referral)
    }
    return referral
  }
}
