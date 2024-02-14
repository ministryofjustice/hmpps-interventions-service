package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.reporting.ndmis.performance

import jakarta.persistence.EntityManagerFactory
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.item.database.JpaPagingItemReader
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Referral

@Component
@JobScope
class NdmisReader(entityManagerFactory: EntityManagerFactory) : JpaPagingItemReader<Referral>() {

  init {
    this.name = "ndmisReader"
    this.setEntityManagerFactory(entityManagerFactory)
    this.setQueryString(
      "FROM Referral",
    )
    this.pageSize = 10
  }
}
