package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AuthUser
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.DynamicFrameworkContract
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Referral
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ServiceProvider
import java.time.OffsetDateTime
import java.util.UUID

interface ReferralRepository : JpaRepository<Referral, UUID> {
  // queries for service providers
  fun findAllByInterventionDynamicFrameworkContractPrimeProviderInAndSentAtIsNotNull(providers: Iterable<ServiceProvider>): List<Referral>
  fun findAllByInterventionDynamicFrameworkContractSubcontractorProvidersInAndSentAtIsNotNull(providers: Iterable<ServiceProvider>): List<Referral>

  // queries for sent referrals
  fun findByIdAndSentAtIsNotNull(id: UUID): Referral?
  fun findByCreatedByAndSentAtIsNotNull(user: AuthUser): List<Referral>
  fun existsByReferenceNumber(reference: String): Boolean
  fun findByServiceUserCRNAndSentAtIsNotNull(crn: String): List<Referral>

  // queries for draft referrals
  fun findByIdAndSentAtIsNull(id: UUID): Referral?
  fun findByCreatedByIdAndSentAtIsNull(userId: String): List<Referral>

  // queries for reporting
  @Query("select r.id from Referral r where r.sentAt > :from and r.sentAt < :to and r.intervention.dynamicFrameworkContract in :contracts")
  fun serviceProviderReportReferralIds(from: OffsetDateTime, to: OffsetDateTime, contracts: Set<DynamicFrameworkContract>, pageable: Pageable): Page<UUID>
}
