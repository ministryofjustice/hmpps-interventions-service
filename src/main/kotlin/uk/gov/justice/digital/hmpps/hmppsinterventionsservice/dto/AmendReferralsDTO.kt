package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto

import java.util.UUID

data class AmendReferralsDTO(
  val complexityLevelId: UUID,
  val serviceCategoryId: UUID,
  val reasonForChange: String
) {
  // nothing to do
}

data class AmendComplexityLevelDTO(
  val complexityLevelId: UUID,
  val serviceCategoryId: UUID,
  val reasonForChange: String
) {
  companion object {
    fun from(amendComplexityLevelDTO: AmendComplexityLevelDTO): AmendComplexityLevelDTO {
      return AmendComplexityLevelDTO(
        complexityLevelId = amendComplexityLevelDTO.complexityLevelId,
        serviceCategoryId = amendComplexityLevelDTO.serviceCategoryId,
        reasonForChange = amendComplexityLevelDTO.reasonForChange
      )
    }
  }
}
data class AmendNeedAndRequirementDTO(
  var furtherInformation: String? = null,
  var additionalNeedsInformation: String? = null,
  var accessibilityNeeds: String? = null,
  var needsInterpreter: Boolean? = null,
  var interpreterLanguage: String? = null,
  var hasAdditionalResponsibilities: Boolean? = null,
  var whenUnavailable: String? = null,
  val reasonForChange: String
) {
  companion object {
    fun from(amendNeedAndRequirementDTO: AmendNeedAndRequirementDTO): AmendNeedAndRequirementDTO {
      return AmendNeedAndRequirementDTO(
        furtherInformation = amendNeedAndRequirementDTO.furtherInformation,
        additionalNeedsInformation = amendNeedAndRequirementDTO.additionalNeedsInformation,
        accessibilityNeeds = amendNeedAndRequirementDTO.accessibilityNeeds,
        needsInterpreter = amendNeedAndRequirementDTO.needsInterpreter,
        interpreterLanguage = amendNeedAndRequirementDTO.interpreterLanguage,
        hasAdditionalResponsibilities = amendNeedAndRequirementDTO.hasAdditionalResponsibilities,
        whenUnavailable = amendNeedAndRequirementDTO.whenUnavailable,
        reasonForChange = amendNeedAndRequirementDTO.reasonForChange

      )
    }
  }
}
