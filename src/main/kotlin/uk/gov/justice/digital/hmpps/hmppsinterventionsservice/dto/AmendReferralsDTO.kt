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
