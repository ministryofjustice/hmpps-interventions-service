package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.reporting.serviceprovider.performance.model.ReferralPerformanceReport
import java.time.OffsetDateTime
import java.util.UUID

interface ReferralPerformanceReportRepository : JpaRepository<ReferralPerformanceReport, UUID> {
  // queries for reporting
  @Query("select r from ReferralPerformanceReport r where r.dateReferralReceived > :from and r.dateReferralReceived < :to and r.contractReference in :contractReferences")
  fun serviceProviderReportReferrals(from: OffsetDateTime, to: OffsetDateTime, contractReferences: List<String>, pageable: Pageable): Page<ReferralPerformanceReport>
}
