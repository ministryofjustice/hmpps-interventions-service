package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jobs.oneoff.loadstatus

import org.hibernate.SessionFactory
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.item.database.HibernateCursorItemReader
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Referral

@Component
@JobScope
class LoadStatusReader(
  sessionFactory: SessionFactory,
) : HibernateCursorItemReader<Referral>() {
  init {
    this.setName("loadStatusReader")
    this.setSessionFactory(sessionFactory)
    this.setQueryString(
      "SELECT r FROM Referral r where r.status is null",
    )
  }
}
