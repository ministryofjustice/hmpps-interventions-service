package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entities.Referral
import java.util.UUID

@Repository
interface ReferralRepository : JpaRepository<Referral, Long> {

  fun findByReferralUuid(referralUuid: UUID): Referral?
}
