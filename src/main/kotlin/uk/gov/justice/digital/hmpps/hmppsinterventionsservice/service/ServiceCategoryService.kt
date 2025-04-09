package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.DesiredOutcome
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.RuleType
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ServiceCategory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ServiceCategoryRepository
import java.util.UUID

@Service
@Transactional
class ServiceCategoryService(val repository: ServiceCategoryRepository) {
  fun getServiceCategoryByID(id: UUID): ServiceCategory? = repository.findByIdOrNull(id)
  fun getServiceCategoryByIDAndContractReference(id: UUID, contractReference: String): ServiceCategory? {
    val serviceCategoryWithAllDesiredOutcomes = repository.findByIdOrNull(id)
    val serviceCategoryWithFilteredOutcomes = serviceCategoryWithAllDesiredOutcomes?.copy()
    serviceCategoryWithFilteredOutcomes?.desiredOutcomes = serviceCategoryWithFilteredOutcomes?.desiredOutcomes?.filter { isDesiredOutcomeValidForContract(it, contractReference) }.orEmpty()

    return serviceCategoryWithFilteredOutcomes
  }

  fun isDesiredOutcomeValidForContract(desiredOutcome: DesiredOutcome, contractReference: String): Boolean {
    if (desiredOutcome.deprecatedAt != null) {
      return false
    }

    desiredOutcome.desiredOutcomeFilterRules.forEach {
      return when (it.ruleType) {
        RuleType.INCLUDE -> it.matchData.contains(contractReference)
        RuleType.EXCLUDE -> !it.matchData.contains(contractReference)
      }
    }
    return true
  }
}
