package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jobs.oneoff.transferreferrals

import mu.KLogging
import net.logstash.logback.argument.StructuredArguments.kv
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.item.ItemProcessor
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.DraftReferral
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.DraftOasysRiskInformationRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.DraftReferralRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ReferralDetailsRepository

@Component
@JobScope
@Transactional
class DeleteOldDraftReferralsProcessor(
  private val draftOasysRiskInformationRepository: DraftOasysRiskInformationRepository,
  private val referralDetailsRepository: ReferralDetailsRepository,
  private val draftReferralRepository: DraftReferralRepository,
) : ItemProcessor<DraftReferral, DraftReferral> {
  companion object : KLogging()

  override fun process(draftReferral: DraftReferral): DraftReferral {
    logger.info("deleting stale draft referral {} which is 90 days old", kv("referralId", draftReferral.id))
    draftOasysRiskInformationRepository.deleteByReferralId(draftReferral.id)
    referralDetailsRepository.deleteByReferralId(draftReferral.id)
    draftReferralRepository.delete(draftReferral)
    return draftReferral
  }
}
