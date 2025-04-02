package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository

import org.springframework.data.repository.CrudRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ServiceCategory
import java.time.OffsetDateTime
import java.util.UUID

interface ServiceCategoryRepository : CrudRepository<ServiceCategory, UUID> {
  fun findByIdIn(serviceCategoryIds: List<UUID>): Set<ServiceCategory>
  fun findByName(name: String): ServiceCategory?
  fun findAllByIdAndDesiredOutcomesDeprecatedAtIsNotNull(id: UUID): MutableList<ServiceCategory>
}
