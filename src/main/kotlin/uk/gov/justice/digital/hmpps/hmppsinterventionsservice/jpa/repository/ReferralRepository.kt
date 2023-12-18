package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.DynamicFrameworkContract
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Referral
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.reporting.serviceprovider.performance.model.PerformanceReportReferral
import java.time.OffsetDateTime
import java.util.UUID

interface ReferralRepository : JpaRepository<Referral, UUID>, JpaSpecificationExecutor<Referral> {
  // queries for sent referrals
  fun findByIdAndSentAtIsNotNull(id: UUID): Referral?
  fun existsByReferenceNumber(reference: String): Boolean

  // queries for reporting
  @Query("select r from Referral r where r.sentAt > :from and r.sentAt < :to and r.intervention.dynamicFrameworkContract in :contracts")
  fun serviceProviderReportReferrals(from: OffsetDateTime, to: OffsetDateTime, contracts: Set<DynamicFrameworkContract>, pageable: Pageable): Page<Referral>

  @Query("select r from Referral r where r.sentAt > :from and r.sentAt < :to and r.intervention.dynamicFrameworkContract in :contracts")
  fun serviceProviderReportReferralsList(from: OffsetDateTime, to: OffsetDateTime, contracts: Set<DynamicFrameworkContract>): List<Referral>

  fun findByServiceUserCRN(crn: String): List<Referral>

  @Query(name = "find_performance_report_referral", nativeQuery = true)
  fun findPerformanceReportReferral(from: OffsetDateTime, to: OffsetDateTime, contractReferences: List<String>, pageable: Pageable): Slice<PerformanceReportReferral>

  @Query(name = "find_performance_report_referral", nativeQuery = true)
  fun findPerformanceReportReferralList(from: OffsetDateTime, to: OffsetDateTime, contractReferences: List<String>): List<PerformanceReportReferral>
}
