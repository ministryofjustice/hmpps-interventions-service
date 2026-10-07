package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.DraftReferral
import java.time.OffsetDateTime
import java.util.UUID

interface DraftReferralRepository : JpaRepository<DraftReferral, UUID> {

  @Query(
    value =
    """select dr from DraftReferral dr where dr.createdBy.id = :userId 
      and NOT EXISTS 
      (select r from Referral r where r.id = dr.id)""",
  )
  fun findByCreatedById(userId: String): List<DraftReferral>

  fun findByServiceUserCRN(crn: String): List<DraftReferral>

  @Query(
    value =
    """select dr from DraftReferral dr where dr.serviceUserCRN = :serviceUserCRN
      and dr.createdAt >= :from and dr.createdAt <= :to
      and dr.id not in (select r.id from Referral r where r.serviceUserCRN = :serviceUserCRN)""",
  )
  fun draftReferralForSar(serviceUserCRN: String, from: OffsetDateTime, to: OffsetDateTime): List<DraftReferral>

  @Query(
    value =
    """select dr from DraftReferral dr where dr.serviceUserCRN = :serviceUserCRN
      and dr.id not in (select r.id from Referral r where r.serviceUserCRN = :serviceUserCRN)""",
  )
  fun findDraftOnlyByServiceUserCRN(serviceUserCRN: String): List<DraftReferral>

  /**
   * SAS-specific query: uses NOT EXISTS (faster than NOT IN on large tables), eagerly fetches
   * selectedServiceCategories and the intervention → dynamicFrameworkContract → primeProvider
   * chain in a single query to avoid N+1 lazy-load round-trips.
   * subcontractorProviders is left for BatchSize-based batch loading.
   */
  @Query(
    value =
    """select distinct dr from DraftReferral dr
      left join fetch dr.selectedServiceCategories
      join fetch dr.intervention i
      join fetch i.dynamicFrameworkContract dc
      join fetch dc.primeProvider
      where dr.serviceUserCRN = :serviceUserCRN
        and not exists (select r from Referral r where r.id = dr.id)""",
  )
  fun findDraftOnlyByServiceUserCRNForSAS(serviceUserCRN: String): List<DraftReferral>
}
