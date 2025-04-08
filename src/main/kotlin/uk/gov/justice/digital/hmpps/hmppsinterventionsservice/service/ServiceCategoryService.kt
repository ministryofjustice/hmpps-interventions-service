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
    val serviceCategory = repository.findByIdOrNull(id)

    serviceCategory?.desiredOutcomes = serviceCategory?.desiredOutcomes?.filter { shouldWeKeepDesiredOutcome(it, contractReference) }.orEmpty()

    return serviceCategory
  }

  fun shouldWeKeepDesiredOutcome(desiredOutcome: DesiredOutcome, contractReference: String): Boolean {
    if (desiredOutcome.desiredOutcomeFilterRules.size > 0) {
      desiredOutcome.desiredOutcomeFilterRules.forEach {
        if (it.ruleType == RuleType.EXCLUDE) {
          if (it.matchData.contains(contractReference)) {
            return false
          }
        }
//        if(it.ruleType == RuleType.INCLUDE){
//          if(it.matchData.contains(contractReference)){
//            return true
//          }
      }
    }
    return true
  }
}
