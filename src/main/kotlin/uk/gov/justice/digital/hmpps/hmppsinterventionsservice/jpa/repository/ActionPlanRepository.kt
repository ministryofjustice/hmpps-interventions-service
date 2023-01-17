package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ActionPlan
import java.util.UUID

interface ActionPlanRepository : JpaRepository<ActionPlan, UUID> {
  fun findByIdAndSubmittedAtIsNull(id: UUID): ActionPlan?
  fun findAllByReferralIdAndApprovedAtIsNotNull(referralId: UUID): List<ActionPlan>
  @Query("select ap from ActionPlan ap where ap.referral.id = :referralId order by ap.approvedAt desc")
  fun findLatestApprovedActionPlan(referralId: UUID): ActionPlan?
}
