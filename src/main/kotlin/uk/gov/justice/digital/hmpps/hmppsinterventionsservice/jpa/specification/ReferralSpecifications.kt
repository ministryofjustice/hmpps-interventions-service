package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.specification

import jakarta.persistence.criteria.JoinType
import org.springframework.data.jpa.domain.Specification
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.DynamicFrameworkContract
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Intervention
class ReferralSpecifications {
  companion object {

    fun <T> withSPAccess(contracts: Set<DynamicFrameworkContract>): Specification<T> {
      return Specification<T> { root, _, _ ->
        val interventionJoin = root.join<T, Intervention>("intervention", JoinType.INNER)
        val dynamicContractJoin = interventionJoin.join<Intervention, DynamicFrameworkContract>("dynamicFrameworkContract", JoinType.LEFT)
        dynamicContractJoin.`in`(contracts)
      }
    }
  }
}
