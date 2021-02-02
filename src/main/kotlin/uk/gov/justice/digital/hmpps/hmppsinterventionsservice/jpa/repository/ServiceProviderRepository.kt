package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository

import org.springframework.data.repository.CrudRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ServiceProvider
import java.util.UUID

interface ServiceProviderRepository : CrudRepository<ServiceProvider, UUID> {
  fun findById(name: String): ServiceProvider?
}
