package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.specification

import org.springframework.data.jpa.domain.Specification
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AuthUser
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.DynamicFrameworkContract
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.EndOfServiceReport
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Intervention
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ReferralAssignment
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ServiceUserData
import java.time.OffsetDateTime
import javax.persistence.criteria.JoinType

class ReferralSpecifications {
  companion object {

    fun <T> sent(): Specification<T> {
      return Specification<T> { root, _, cb ->
        cb.isNotNull(root.get<OffsetDateTime>("sentAt"))
      }
    }

    fun <T> concluded(): Specification<T> {
      return Specification<T> { root, _, cb ->
        cb.isNotNull(root.get<OffsetDateTime>("concludedAt"))
      }
    }

    fun <T> unassigned(): Specification<T> {
      return Specification<T> { root, _, cb ->
        cb.isEmpty(root.get<List<ReferralAssignment>>("assignments"))
      }
    }

    fun <T> search(searchText: String): Specification<T> {
      return Specification<T> { root, _, cb ->
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
      return Specification<T> { root, _, cb ->
        cb.and(
          cb.isNotNull(root.get<OffsetDateTime>("endRequestedAt")),
          cb.isNotNull(root.get<OffsetDateTime>("concludedAt")),
          root.join<T, EndOfServiceReport>("endOfServiceReport", JoinType.LEFT).isNull
        )
      }
    }

    fun <T> withSPAccess(contracts: Set<DynamicFrameworkContract>): Specification<T> {
      return Specification<T> { root, _, _ ->
        val interventionJoin = root.join<T, Intervention>("intervention", JoinType.INNER)
        val dynamicContractJoin = interventionJoin.join<Intervention, DynamicFrameworkContract>("dynamicFrameworkContract", JoinType.LEFT)
        dynamicContractJoin.`in`(contracts)
      }
    }

    fun <T> createdBy(authUser: AuthUser): Specification<T> {
      return Specification<T> { root, _, cb -> cb.equal(root.get<String>("createdBy"), authUser) }
    }

    fun <T> matchingServiceUserReferrals(serviceUserCRNs: List<String>): Specification<T> {
      return Specification<T> { root, _, _ -> root.get<String>("serviceUserCRN").`in`(serviceUserCRNs) }
    }

    inline fun <reified T> currentlyAssignedTo(authUserId: String): Specification<T> {
      return Specification<T> { root, _, cb ->
        val referralAssignmentJoin = root.join<T, ReferralAssignment>("assignments", JoinType.INNER)
        val authUserJoin = referralAssignmentJoin.join<ReferralAssignment, AuthUser>("assignedTo", JoinType.LEFT)

        val latestAssignment = cb.equal(referralAssignmentJoin.get<Boolean>("superseded"), false)
        val sameUser = cb.equal(authUserJoin.get<String>("id"), authUserId)
        cb.and(latestAssignment, sameUser)
      }
    }
  }
}
