package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto

import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ComplexityLevel
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.DesiredOutcome
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ServiceCategory
import java.util.UUID

data class DesiredOutcomeDTO(
  val id: UUID,
  val description: String
) {
  companion object {
    fun from(desiredOutcome: DesiredOutcome): DesiredOutcomeDTO {
      return DesiredOutcomeDTO(desiredOutcome.id, desiredOutcome.description)
    }
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
  val desiredOutcomes: List<DesiredOutcomeDTO>
) {
  companion object {
    fun from(serviceCategory: ServiceCategory): ServiceCategoryFullDTO {
      return ServiceCategoryFullDTO(
        id = serviceCategory.id,
        name = serviceCategory.name,
        complexityLevels = addComplexityLevelsInOrderOfComplexity(serviceCategory.complexityLevels),
        desiredOutcomes = serviceCategory.desiredOutcomes.map { DesiredOutcomeDTO(it.id, it.description) },
      )
    }
    private fun addComplexityLevelsInOrderOfComplexity(complexityLevels: List<ComplexityLevel>): List<ComplexityLevelDTO> {
      val complexityLevelsDTO = mutableListOf<ComplexityLevelDTO>()
      val lowComplexityLevel = complexityLevels.find { it.title == "Low complexity" }
      val mediumComplexityLevel = complexityLevels.find { it.title == "Medium complexity" }
      val highComplexityLevel = complexityLevels.find { it.title == "High complexity" }
      if (lowComplexityLevel != null)
        complexityLevelsDTO.add(ComplexityLevelDTO(lowComplexityLevel.id, lowComplexityLevel.title, lowComplexityLevel.description))
      if (mediumComplexityLevel != null)
        complexityLevelsDTO.add(ComplexityLevelDTO(mediumComplexityLevel.id, mediumComplexityLevel.title, mediumComplexityLevel.description))
      if (highComplexityLevel != null)
        complexityLevelsDTO.add(ComplexityLevelDTO(highComplexityLevel.id, highComplexityLevel.title, highComplexityLevel.description))
      return complexityLevelsDTO
    }
  }
}

data class ServiceCategoryDTO(
  val id: UUID,
  val name: String,
) {
  companion object {
    fun from(serviceCategory: ServiceCategory): ServiceCategoryDTO {
      return ServiceCategoryDTO(
        id = serviceCategory.id,
        name = serviceCategory.name,
      )
    }
  }
}
