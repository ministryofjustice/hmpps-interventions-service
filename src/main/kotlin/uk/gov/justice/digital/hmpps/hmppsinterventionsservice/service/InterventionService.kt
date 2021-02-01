package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Intervention
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.InterventionRepository
import java.util.UUID

@Service
class InterventionService(val repository: InterventionRepository) {

  fun getIntervention(id: UUID): Intervention? {
    return repository.findByIdOrNull(id)
  }
  fun getInterventionsForServiceProvider(id: String): List<Intervention> {
    return repository.findByDynamicFrameworkContractServiceProviderId(id)
  }
  fun createIntervention(intervention: Intervention): Intervention {
    return repository.save(intervention)
  }
}
