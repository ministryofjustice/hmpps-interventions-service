package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.DynamicFrameworkContract
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Referral
import java.time.OffsetDateTime
import java.util.UUID

interface ReferralRepository :
  JpaRepository<Referral, UUID>,
  JpaSpecificationExecutor<Referral>,
  ReferralSummaryRepository {
  // queries for sent referrals
  fun findByIdAndSentAtIsNotNull(id: UUID): Referral?
  fun existsByReferenceNumber(reference: String): Boolean

  // queries for reporting
  @Query("select r from Referral r where r.sentAt > :from and r.sentAt < :to and r.intervention.dynamicFrameworkContract in :contracts")
  fun serviceProviderReportReferrals(from: OffsetDateTime, to: OffsetDateTime, contracts: Set<DynamicFrameworkContract>, pageable: Pageable): Page<Referral>

  @Query("select r from Referral r where r.serviceUserCRN = :serviceUserCRN and r.createdAt >= :from and r.createdAt <= :to")
  fun referralForSar(serviceUserCRN: String, from: OffsetDateTime, to: OffsetDateTime): List<Referral>

  fun findByServiceUserCRN(crn: String): List<Referral>

  /**
   * SAS-specific query: eagerly fetches selectedServiceCategories and the intervention →
   * dynamicFrameworkContract → primeProvider chain in a single query to avoid N+1 lazy-load
   * round-trips.  subcontractorProviders is left for BatchSize-based batch loading.
   */
  @Query(
    """select distinct r from Referral r
      left join fetch r.selectedServiceCategories
      join fetch r.intervention i
      join fetch i.dynamicFrameworkContract dc
      join fetch dc.primeProvider
      where r.serviceUserCRN = :crn""",
  )
  fun findByServiceUserCRNForSAS(crn: String): List<Referral>
}
