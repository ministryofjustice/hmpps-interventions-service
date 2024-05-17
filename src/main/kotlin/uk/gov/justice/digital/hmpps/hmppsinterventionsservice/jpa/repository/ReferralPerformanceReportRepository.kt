package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository

import org.springframework.data.jpa.repository.Query
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.reporting.serviceprovider.performance.model.ReferralPerformanceReport
import java.time.OffsetDateTime

interface ReferralPerformanceReportRepository {
  // queries for reporting
  @Query("select * from performance_report(array[:contractReferences],:from, :to)")
  fun serviceProviderReportReferrals(from: OffsetDateTime, to: OffsetDateTime, contractReferences: List<String>/*, pageable: Pageable*/): /*Page*/List<ReferralPerformanceReport>
}
