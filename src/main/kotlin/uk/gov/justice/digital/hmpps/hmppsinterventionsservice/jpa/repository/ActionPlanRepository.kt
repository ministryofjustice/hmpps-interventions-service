package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ActionPlan
import java.util.UUID

interface ActionPlanRepository : JpaRepository<ActionPlan, UUID> {
  fun findByIdAndSubmittedAtIsNull(id: UUID): ActionPlan?
  fun findAllByReferralIdAndApprovedAtIsNotNull(referralId: UUID): List<ActionPlan>
  fun findTopByReferralIdAndApprovedAtIsNotNullOrderByApprovedAtDesc(referralId: UUID): ActionPlan?
}

fun ActionPlanRepository.findLatestApprovedActionPlan(referralId: UUID): ActionPlan? =
  findTopByReferralIdAndApprovedAtIsNotNullOrderByApprovedAtDesc(referralId)
