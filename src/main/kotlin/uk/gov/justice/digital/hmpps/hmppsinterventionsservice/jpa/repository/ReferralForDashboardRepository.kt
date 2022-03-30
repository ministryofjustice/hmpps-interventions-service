package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository

import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.repository.NoRepositoryBean
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ReferralForDashboard
import java.util.UUID

interface ReferralForDashboardRepository : JpaRepository<ReferralForDashboard, UUID>, JpaSpecificationExecutor<ReferralForDashboard> {
  //TODO- Projection with specification is not been implemented by Spring JPA yet.So have to come up with this approach of creating
  // a repository for projecting the fields we wanted. Will have to move to projection when that is ready.
  @EntityGraph(value = "entity-referral-graph")
  override fun findAll(spec: Specification<ReferralForDashboard>): List<ReferralForDashboard>
}
