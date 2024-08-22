package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jobs.oneoff.transferreferrals

import jakarta.persistence.EntityManagerFactory
import org.hibernate.SessionFactory
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.item.database.JpaCursorItemReader
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ActionPlan

@Component
@JobScope
class ApproveActionPlansReader(
  sessionFactory: SessionFactory,
  entityManagerFactory: EntityManagerFactory,
) : JpaCursorItemReader<ActionPlan>() {
  init {
    this.setName("approveActionPlansReader")
    this.setEntityManagerFactory(entityManagerFactory)
//    this.setSessionFactory(sessionFactory)
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
//    this.setParameterValues(
//      mapOf(
//        "fromContract" to fromContract,
//        "transferSignalText" to transferSignalText,
//      ),
//    )
  }
//  init {
//    this.setName("approveActionPlansReader")
//    this.setSessionFactory(sessionFactory)
//    this.setQueryString(
//      "SELECT ap.id, ap.referral_id, ap.number_of_sessions, ap.created_by_id, ap.created_at, ap.submitted_by_id, ap.submitted_at, ap.approved_at, ap.approved_by_id " +
//        "FROM ActionPlan ap " +
//        "INNER JOIN( " +
//        "SELECT referral_id, MAX(submitted_at) AS submitted_at " +
//        "FROM ActionPlan aa " +
//        "GROUP BY referral_id " +
//        ") AS t1 on (t1.referral_id = ap.referral_id and t1.submitted_at = ap.submitted_at) " +
//        "INNER JOIN referral r " +
//        "ON r.id = ap.referral_id " +
//        "WHERE ap.submitted_at is not null " +
//        "AND ap.approved_at is null " +
//        "AND r.concluded_at is null ",
//    )
// //    this.setParameterValues(
// //      mapOf(
// //        "fromContract" to fromContract,
// //        "transferSignalText" to transferSignalText,
// //      ),
// //    )
//  }
}
