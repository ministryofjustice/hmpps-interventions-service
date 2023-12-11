package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.specification

import org.springframework.data.jpa.domain.Specification
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AuthUser
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.model.ReferralSummary
import java.time.OffsetDateTime
import java.util.UUID

class ReferralSummarySpecifications {
  companion object {

    fun concluded(concluded: Boolean?): Specification<ReferralSummary> {
      return if (concluded == true) {
        Specification<ReferralSummary> { root, _, cb ->
          cb.and(
            cb.isNotNull(root.get<OffsetDateTime>("concludedAt")),
            cb.isNotNull(root.get<UUID>("appointmentId")),
          )
        }
      } else {
        Specification<ReferralSummary> { root, _, cb ->
          cb.isNotNull(root.get<OffsetDateTime>("concludedAt"))
        }
      }
    }

    fun unassigned(): Specification<ReferralSummary> {
      return Specification<ReferralSummary> { root, _, cb ->
        cb.isNull(root.get<String>("assignedToId"))
      }
    }

    fun searchByPoPName(searchText: String): Specification<ReferralSummary> {
      return Specification<ReferralSummary> { root, _, cb ->
        val exp1 = cb.concat(cb.upper(root.get("serviceUserFirstName")), " ")
        val exp2 = cb.concat(exp1, cb.upper(root.get("serviceUserLastName")))
        cb.equal(exp2, searchText.uppercase())
      }
    }

    fun searchByReferenceNumber(referenceNumber: String): Specification<ReferralSummary> {
      return Specification<ReferralSummary> { root, _, cb -> cb.equal(root.get<String>("referenceNumber"), referenceNumber.uppercase()) }
    }

    fun cancelled(): Specification<ReferralSummary> {
      return Specification<ReferralSummary> { root, _, cb ->
        cb.and(
          cb.isNotNull(root.get<OffsetDateTime>("endRequestedAt")),
          cb.isNotNull(root.get<OffsetDateTime>("concludedAt")),
          cb.isNull(root.get<UUID>("eosrId")),
        )
      }
    }

    fun withSPAccess(contracts: Set<UUID>): Specification<ReferralSummary> {
      return Specification<ReferralSummary> { root, _, _ ->
        root.get<ReferralSummary>("contractId").`in`(contracts)
      }
    }

    fun createdBy(authUser: AuthUser): Specification<ReferralSummary> {
      return Specification<ReferralSummary> { root, _, cb -> cb.equal(root.get<String>("createdBy"), authUser.id) }
    }

    fun matchingServiceUserReferrals(serviceUserCRNs: List<String>): Specification<ReferralSummary> {
      return Specification<ReferralSummary> { root, _, _ -> root.get<String>("crn").`in`(serviceUserCRNs) }
    }

    fun currentlyAssignedTo(authUserId: String): Specification<ReferralSummary> {
      return Specification<ReferralSummary> { root, _, cb ->
        cb.equal(root.get<String>("assignedToId"), authUserId)
      }
    }
  }
}
