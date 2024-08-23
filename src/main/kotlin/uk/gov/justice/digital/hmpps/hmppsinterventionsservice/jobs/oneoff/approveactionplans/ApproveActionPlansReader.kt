package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jobs.oneoff.transferreferrals

import jakarta.persistence.EntityManagerFactory
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.item.database.JpaCursorItemReader
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ActionPlan

@Component
@JobScope
class ApproveActionPlansReader(
  entityManagerFactory: EntityManagerFactory,
) : JpaCursorItemReader<ActionPlan>() {
  init {
    this.setName("approveActionPlansReader")
    this.setEntityManagerFactory(entityManagerFactory)
    this.setQueryString(
      "SELECT ap " +
        "FROM ActionPlan ap " +
        "INNER JOIN( " +
        "SELECT aa.referral.id AS referralId, MAX(aa.submittedAt) AS submittedAt " +
        "FROM ActionPlan aa " +
        "GROUP BY aa.referral.id " +
        ") AS t1 on (t1.referralId = ap.referral.id and t1.submittedAt = ap.submittedAt) " +
        "INNER JOIN referral r " +
        "ON r.id = ap.referral.id " +
        "WHERE ap.submittedAt is not null " +
        "AND ap.approvedAt is null " +
        "AND r.concludedAt is null ",
    )
  }
}
