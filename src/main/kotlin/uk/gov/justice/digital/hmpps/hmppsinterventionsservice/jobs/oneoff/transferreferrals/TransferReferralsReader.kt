package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jobs.oneoff.transferreferrals

import jakarta.persistence.EntityManagerFactory
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.item.database.JpaCursorItemReader
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Referral

@Component
@JobScope
class TransferReferralsReader(
  @Value("#{jobParameters['fromContract']}") fromContract: String,
  entityManagerFactory: EntityManagerFactory,
) : JpaCursorItemReader<Referral>() {
  private val transferSignalText = "%Authority to transfer case- On behalf of Advance Charity Ltd, I request and give permission for this referral to be transferred to Women in Prison, as of December 2022.%"

  init {
    this.setName("transferReferralsReader")
    this.setEntityManagerFactory(entityManagerFactory)
    this.setQueryString(
      "SELECT r FROM Referral r JOIN CaseNote n ON n.referral = r " +
        "WHERE r.intervention.dynamicFrameworkContract.contractReference = :fromContract " +
        "  AND n.body like :transferSignalText",
    )
    this.setParameterValues(
      mapOf(
        "fromContract" to fromContract,
        "transferSignalText" to transferSignalText,
      ),
    )
  }
}
