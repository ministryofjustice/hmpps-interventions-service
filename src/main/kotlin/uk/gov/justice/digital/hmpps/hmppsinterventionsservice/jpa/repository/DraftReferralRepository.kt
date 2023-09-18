package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.DraftReferral
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
}
