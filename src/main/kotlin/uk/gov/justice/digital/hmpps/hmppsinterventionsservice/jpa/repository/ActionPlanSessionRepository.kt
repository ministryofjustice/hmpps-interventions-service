package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ActionPlanSession
import java.util.UUID

interface ActionPlanSessionRepository : JpaRepository<ActionPlanSession, UUID> {
  fun findAllByReferralId(referralId: UUID): List<ActionPlanSession>
  fun findByReferralIdAndSessionNumber(referralId: UUID, sessionNumber: Int): ActionPlanSession?
}
