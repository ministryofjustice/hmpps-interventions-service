package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.specification

import org.springframework.data.jpa.domain.Specification
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AuthUser
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.DynamicFrameworkContract
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.EndOfServiceReport
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Intervention
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ReferralAssignment
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ServiceUserData
import java.time.OffsetDateTime
import java.util.UUID
import javax.persistence.criteria.JoinType

class ReferralSpecifications {
  companion object {

    fun <T> sent(): Specification<T> {
      return Specification<T> { root, query, cb ->
        cb.isNotNull(root.get<OffsetDateTime>("sentAt"))
      }
    }

    fun <T> concluded(): Specification<T> {
      return Specification<T> { root, query, cb ->
        cb.isNotNull(root.get<OffsetDateTime>("concludedAt"))
      }
    }

    fun <T> unassigned(): Specification<T> {
      return Specification<T> { root, query, cb ->
        cb.isEmpty(root.get<List<ReferralAssignment>>("assignments"))
      }
    }

    fun <T> search(searchText: String): Specification<T> {
      return Specification<T> { root, query, cb ->
        val serviceUserDataJoin = root.join<T, ServiceUserData>("serviceUserData", JoinType.INNER)
        val exp1 = cb.concat(cb.upper(serviceUserDataJoin.get("firstName")), " ")
        val exp2 = cb.concat(exp1, cb.upper(serviceUserDataJoin.get("lastName")))
        cb.equal(exp2, searchText.uppercase())
      }
    }

    /**
     * This cancellation logic is duplicated from Referral.kt. We have to duplicate it because the logic within the
     * entity is not accessible via JPQL.
     * The official definition of cancelled referral is where the referral is concluded and no session was attended;
     * however, the logic below is a proxy for the same thing since only cancelled referrals have null EOSR from the
     * ReferralConcluder.kt class.
     */
    fun <T> cancelled(): Specification<T> {
      return Specification<T> { root, query, cb ->
        cb.and(
          cb.isNotNull(root.get<OffsetDateTime>("endRequestedAt")),
          cb.isNotNull(root.get<OffsetDateTime>("concludedAt")),
          root.join<T, EndOfServiceReport>("endOfServiceReport", JoinType.LEFT).isNull
        )
      }
    }

    fun <T> withSPAccess(contracts: Set<DynamicFrameworkContract>): Specification<T> {
      return Specification<T> { root, query, cb ->
        val interventionJoin = root.join<T, Intervention>("intervention", JoinType.INNER)
        val dynamicContractJoin = interventionJoin.join<Intervention, DynamicFrameworkContract>("dynamicFrameworkContract", JoinType.LEFT)
        dynamicContractJoin.`in`(contracts)
      }
    }

    fun <T> createdBy(authUser: AuthUser): Specification<T> {
      return Specification<T> { root, query, cb -> cb.equal(root.get<String>("createdBy"), authUser) }
    }

    fun <T> matchingServiceUserReferrals(serviceUserCRNs: List<String>): Specification<T> {
      return Specification<T> { root, query, cb -> root.get<String>("serviceUserCRN").`in`(serviceUserCRNs) }
    }

    /**
     * This is performing the equivalent of:
     *
     * SELECT r FROM referral r
     * inner join referral_assignments ra
     *    inner join auth_user au on ra.assigned_to_id = au.id
     *  on ra.referral_id = r.id
     * where ra.assigned_at = (SELECT MAX(assigned_at) FROM referral_assignments WHERE referral_id = ra.referral_id)
     * and au.id = $USER_ID
     */
    inline fun <reified T> currentlyAssignedTo(authUserId: String): Specification<T> {
      return Specification<T> { root, query, cb ->
        // INNER JOIN
        val referralAssignmentJoin = root.join<T, ReferralAssignment>("assignments", JoinType.INNER)

        // WHERE assigned_at = max(assigned_at)
        val subquery = query.subquery(OffsetDateTime::class.java)
        val subQueryReferral = subquery.from(T::class.java)
        val subQueryReferralAssignmentJoin = subQueryReferral.join<T, ReferralAssignment>("assignments", JoinType.INNER)
        val maxAssignedAt = cb.greatest(subQueryReferralAssignmentJoin.get<OffsetDateTime>("assignedAt"))
        subquery.select(maxAssignedAt)
        subquery.where(cb.equal(root.get<UUID>("id"), subQueryReferral.get<UUID>("id")))
        val maxAssignedAtMatch = cb.equal(referralAssignmentJoin.get<OffsetDateTime>("assignedAt"), subquery)

        // AND assigned_to = authUser
        val authUserJoin = referralAssignmentJoin.join<ReferralAssignment, AuthUser>("assignedTo", JoinType.LEFT)
        cb.and(maxAssignedAtMatch, cb.equal(authUserJoin.get<String>("id"), authUserId))
      }
    }
  }
}
