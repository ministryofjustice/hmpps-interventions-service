package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jobs.oneoff.transferreferrals

import org.hibernate.SessionFactory
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.item.database.HibernateCursorItemReader
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Referral

@Component
@JobScope
class TransferReferralsReader(
  @Value("#{jobParameters['fromContract']}") fromContract: String,
  sessionFactory: SessionFactory,
) : HibernateCursorItemReader<Referral>() {
  private val transferSignalSubject = "Authority to transfer case"
  private val transferSignalText = "On behalf of Advance Charity Ltd, I request and give permission for this referral to be transferred to Women in Prison, as of December 2022."

  init {
    this.setName("transferReferralsReader")
    this.setSessionFactory(sessionFactory)
    this.setQueryString(
      "SELECT r FROM Referral r JOIN CaseNote n ON n.referralId = r.id " +
        "WHERE r.intervention.dynamicFrameworkContract.contractReference = :fromContract " +
        "  AND n.subject = :transferSignalSubject AND n.body = :transferSignalText"
    )
    this.setParameterValues(
      mapOf(
        "fromContract" to fromContract,
        "transferSignalSubject" to transferSignalSubject,
        "transferSignalText" to transferSignalText
      )
    )
  }
}
