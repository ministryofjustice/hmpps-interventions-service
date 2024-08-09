package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jobs.deleteoldreferrals

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jobs.oneoff.transferreferrals.DeleteOldDraftReferralsProcessor
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.DraftReferral
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.DraftOasysRiskInformationRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.DraftReferralRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ReferralDetailsRepository
import java.time.OffsetDateTime

@Transactional
internal class DeleteOldDraftReferralsProcessorTest @Autowired constructor(
  draftOasysRiskInformationRepository: DraftOasysRiskInformationRepository,
  referralDetailsRepository: ReferralDetailsRepository,
  draftReferralRepository: DraftReferralRepository,
) : IntegrationTestBase() {

  private val processor = DeleteOldDraftReferralsProcessor(
    draftOasysRiskInformationRepository,
    referralDetailsRepository,
    draftReferralRepository,
  )

  fun createOldDraftReferrals(): DraftReferral {
    return setupAssistant.createDraftReferral(createdAt = OffsetDateTime.now().minusDays(100))
  }

  @Test
  fun `process referrals that is older than 90 days`() {
    val result = processor.process(createOldDraftReferrals())
    assertThat(draftReferralRepository.findById(result.id)).isEmpty
    assertThat(referralDetailsRepository.findByReferralId(result.id)).isEmpty()
    assertThat(draftOasysRiskInformationRepository.count()).isEqualTo(0)
  }
}
