package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jobs.oneoff.concludereferrals

import jakarta.persistence.EntityManagerFactory
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.item.database.JpaCursorItemReader
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Referral

@Component
@JobScope
class ConcludeReferralsReader(
  entityManagerFactory: EntityManagerFactory,
) : JpaCursorItemReader<Referral>() {

  init {
    this.setName("concludeReferralsReader")
    this.setEntityManagerFactory(entityManagerFactory)
    this.setQueryString(
      "SELECT re FROM Referral re " +
        "LEFT JOIN EndOfServiceReport e ON re.id = e.referral.id " +
        "WHERE e.id IS null " +
        "AND re.endRequestedAt IS NOT null " +
        "AND re.concludedAt is null",
    )
  }
}
