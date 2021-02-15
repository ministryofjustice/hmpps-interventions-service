package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AuthGroupID
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Intervention
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.PCCRegionID
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.InterventionRepository
import java.util.UUID

@Service
class InterventionService(
  val interventionRepository: InterventionRepository,
) {
  fun getIntervention(id: UUID): Intervention? {
    return interventionRepository.findByIdOrNull(id)
  }

  fun getInterventionsForServiceProvider(id: AuthGroupID): List<Intervention> {
    return interventionRepository.findByDynamicFrameworkContractServiceProviderId(id)
  }

  fun getAllInterventions(): List<Intervention> {
    return interventionRepository.findAll()
  }

  fun getInterventions(
    pccRegionIds: List<PCCRegionID>,
    allowsFemale: Boolean?,
    allowsMale: Boolean?,
    minimumAge: Int?,
    maximumAge: Int?
  ): List<Intervention> {
    return interventionRepository.findByCriteria(pccRegionIds, allowsFemale, allowsMale, minimumAge, maximumAge)
  }
}
