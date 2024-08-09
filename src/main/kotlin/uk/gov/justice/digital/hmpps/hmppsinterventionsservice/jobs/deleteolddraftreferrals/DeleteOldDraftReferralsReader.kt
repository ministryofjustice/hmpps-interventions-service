package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jobs.deleteolddraftreferrals

import jakarta.persistence.EntityManagerFactory
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.item.database.JpaCursorItemReader
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.DraftReferral
import java.time.OffsetDateTime

@Component
@JobScope
class DeleteOldDraftReferralsReader(
  entityManagerFactory: EntityManagerFactory,
) : JpaCursorItemReader<DraftReferral>() {
  init {
    this.name = "deleteOldDraftReferralsReader"
    this.setEntityManagerFactory(entityManagerFactory)
    this.setQueryString(
      """SELECT dr FROM DraftReferral dr 
        LEFT JOIN Referral r ON dr.id = r.id  
        WHERE r.id IS NULL AND dr.createdAt <= :ninetyDaysAgo""",
    )
    this.setParameterValues(mapOf("ninetyDaysAgo" to OffsetDateTime.now().minusDays(90)))
  }
}
