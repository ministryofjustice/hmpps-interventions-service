package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Intervention
import java.util.UUID

interface InterventionRepository : JpaRepository<Intervention, UUID>, InterventionFilterRepository {
  fun findByDynamicFrameworkContractPrimeProviderId(id: String): List<Intervention>
  fun findByDynamicFrameworkContractIdIn(ids: Iterable<UUID>): List<Intervention>

  @Query("select i from Intervention i where i.dynamicFrameworkContract.contractReference = :contractReference")
  fun findByDynamicFrameworkContractContractReference(contractReference: String): Intervention?
}
