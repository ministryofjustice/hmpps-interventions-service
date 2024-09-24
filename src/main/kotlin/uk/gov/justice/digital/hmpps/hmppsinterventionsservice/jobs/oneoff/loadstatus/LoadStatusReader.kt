package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jobs.oneoff.loadstatus

import jakarta.persistence.EntityManagerFactory
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.item.database.JpaCursorItemReader
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Referral

@Component
@JobScope
class LoadStatusReader(
  entityManagerFactory: EntityManagerFactory,
) : JpaCursorItemReader<Referral>() {
  init {
    this.setName("loadStatusReader")
    this.setEntityManagerFactory(entityManagerFactory)
    this.setQueryString(
      "SELECT r FROM Referral r where r.status = 'POST_ICA'",
    )
  }
}
