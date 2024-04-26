package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository

import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.reporting.serviceprovider.performance.model.ReferralPerformanceReport
import java.time.OffsetDateTime

interface ReferralPerformanceReportRepository {
  // queries for reporting
  // @Query("select r from ReferralPerformanceReport r where r.dateReferralReceived > :from and r.dateReferralReceived < :to and r.contractReference in :contractReferences")
  fun serviceProviderReportReferrals(from: OffsetDateTime, to: OffsetDateTime, contractReferences: List<String>/*, pageable: Pageable*/): /*Page*/List<ReferralPerformanceReport>
}
