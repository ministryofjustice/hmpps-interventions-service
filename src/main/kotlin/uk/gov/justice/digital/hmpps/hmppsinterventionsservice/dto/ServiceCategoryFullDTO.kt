package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto

import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.DesiredOutcome
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.DesiredOutcomeFilterRule
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ServiceCategory
import java.util.UUID

data class DesiredOutcomeDTO(
  val id: UUID,
  val description: String,
  val filterRules: MutableSet<DesiredOutcomeFilterRule> = mutableSetOf(),
) {
  companion object {
    fun from(desiredOutcome: DesiredOutcome): DesiredOutcomeDTO = DesiredOutcomeDTO(desiredOutcome.id, desiredOutcome.description, desiredOutcome.desiredOutcomeFilterRules)
  }
}

data class ComplexityLevelDTO(
  val id: UUID,
  val title: String,
  val description: String,
)

data class ServiceCategoryFullDTO(
  val id: UUID,
  val name: String,
  val complexityLevels: List<ComplexityLevelDTO>,
  val desiredOutcomes: List<DesiredOutcomeDTO>,
) {
  companion object {
    fun from(serviceCategory: ServiceCategory): ServiceCategoryFullDTO = ServiceCategoryFullDTO(
      id = serviceCategory.id,
      name = serviceCategory.name,
      complexityLevels = serviceCategory.complexityLevels.map { ComplexityLevelDTO(it.id, it.title, it.description) },
      desiredOutcomes = serviceCategory.desiredOutcomes.map { DesiredOutcomeDTO(it.id, it.description) },
    )
  }
}

data class ServiceCategoryWithActiveOutcomesDTO(
  val id: UUID,
  val name: String,
  val complexityLevels: List<ComplexityLevelDTO>,
  val desiredOutcomes: List<DesiredOutcomeDTO>,
) {
  companion object {
    fun from(serviceCategory: ServiceCategory): ServiceCategoryFullDTO = ServiceCategoryFullDTO(
      id = serviceCategory.id,
      name = serviceCategory.name,
      complexityLevels = serviceCategory.complexityLevels.map { ComplexityLevelDTO(it.id, it.title, it.description) },
      desiredOutcomes = serviceCategory.desiredOutcomes.filter { it.deprecatedAt == null }.map { DesiredOutcomeDTO(it.id, it.description) },
    )
  }
}

data class ServiceCategoryDTO(
  val id: UUID,
  val name: String,
) {
  companion object {
    fun from(serviceCategory: ServiceCategory): ServiceCategoryDTO = ServiceCategoryDTO(
      id = serviceCategory.id,
      name = serviceCategory.name,
    )
  }
}
