package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jobs.oneoff.concludereferrals

import org.hibernate.SessionFactory
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.item.database.HibernateCursorItemReader
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Referral

@Component
@JobScope
class ConcludeReferralsReader(
  sessionFactory: SessionFactory,
) : HibernateCursorItemReader<Referral>() {

  init {
    this.setName("concludeReferralsReader")
    this.setSessionFactory(sessionFactory)
    this.setQueryString(
      "SELECT r FROM Referral r " +
        "LEFT JOIN EndOfServiceReport e ON r.id = e.referral.id " +
        "WHERE e.id IS null " +
        "AND end_requested_at IS NOT null " +
        "AND concluded_at is null",
    )
  }
}
