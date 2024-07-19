package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jobs.oneoff.transferreferrals

import mu.KLogging
import net.logstash.logback.argument.StructuredArguments.kv
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.item.ItemProcessor
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Referral
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ReferralRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.RelevantEndOfSentenceDataloadRepository

@Component
@JobScope
class LoadEndOfSentenceProcessor(
  private val relevantEndOfSentenceDataloadRepository: RelevantEndOfSentenceDataloadRepository,
  private val referralRepository: ReferralRepository,
) : ItemProcessor<Referral, Referral> {
  companion object : KLogging()

  override fun process(referral: Referral): Referral {
    logger.info("processing referral {} for endOfSentenceDate", kv("referralId", referral.id))
    return updateReferralEndOfSentence(referral)
  }

  fun updateReferralEndOfSentence(referral: Referral): Referral {
    val endOfSentenceDataload = referral.relevantSentenceId?.let { relevantEndOfSentenceDataloadRepository.findByRelevantSentenceId(it) }
    referral.relevantSentenceEndDate = endOfSentenceDataload?.relevantSentenceEndDate
    return referralRepository.save(referral)
  }
}
