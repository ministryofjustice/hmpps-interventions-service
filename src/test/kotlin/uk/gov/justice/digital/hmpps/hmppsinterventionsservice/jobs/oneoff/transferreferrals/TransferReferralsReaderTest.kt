package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jobs.oneoff.transferreferrals

import org.assertj.core.api.Assertions.assertThat
import org.hibernate.SessionFactory
import org.junit.jupiter.api.Test
import org.springframework.batch.item.ExecutionContext
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.CaseNote
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Referral

internal class TransferReferralsReaderTest @Autowired constructor(
  sessionFactory: SessionFactory,
) : IntegrationTestBase() {
  private val sourceContractCode = "READER_TEST_FROM"
  private val reader = TransferReferralsReader(fromContract = sourceContractCode, sessionFactory)

  fun createReferralOnSourceContract(): Referral {
    val contract = setupAssistant.createDynamicFrameworkContract(contractReference = sourceContractCode, primeProviderId = "IRRELEVANT")
    val sourceIntervention = setupAssistant.createIntervention(dynamicFrameworkContract = contract)
    return setupAssistant.createSentReferral(intervention = sourceIntervention)
  }

  fun createTransferCaseNote(referral: Referral): CaseNote {
    return setupAssistant.createCaseNote(
      referral = referral,
      subject = "Authority to transfer case",
      body = "On behalf of Advance Charity Ltd, I request and give permission for this referral to be transferred to Women in Prison, as of December 2022.",
    )
  }

  @Test
  fun `finds referrals on the source contract that have a transfer case note`() {
    val referralOnSourceContract = createReferralOnSourceContract()
    createTransferCaseNote(referralOnSourceContract)

    reader.open(ExecutionContext())
    assertThat(reader.read()?.id).isEqualTo(referralOnSourceContract.id)
  }

  @Test
  fun `skips referrals on different interventions, even if they have a trasfer case note`() {
    val otherReferral = setupAssistant.createSentReferral()
    createTransferCaseNote(otherReferral)

    reader.open(ExecutionContext())
    assertThat(reader.read()).isNull()
  }

  @Test
  fun `skips referrals on the source contract without a transfer case note`() {
    val referralOnSourceContract = createReferralOnSourceContract()
    setupAssistant.createCaseNote(referral = referralOnSourceContract, subject = "Other", body = "Test")

    reader.open(ExecutionContext())
    assertThat(reader.read()).isNull()
  }
}
