package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto

import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Changelog
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.AmendTopic
import java.util.UUID

data class AmendReferralsDTO(
  val values: List<String>,
  val reasonForChange: String? = null,
  val amendTopic: AmendTopic,
) {
  companion object {
    fun from(changelog: Changelog): AmendReferralsDTO {
      return AmendReferralsDTO(
        values = changelog.newVal.values,
        reasonForChange = changelog.reasonForChange,
        amendTopic = changelog.topic,
      )
    }
  }
}

data class AmendComplexityLevelDTO(
  val complexityLevelId: UUID,
  val reasonForChange: String,
)

data class AmendDesiredOutcomesDTO(
  val desiredOutcomesIds: List<UUID>,
  val reasonForChange: String,
)

data class AmendNeedsAndRequirementsDTO(
  val hasAdditionalResponsibilities: Boolean? = null,
  val whenUnavailable: String? = null,
  var accessibilityNeeds: String? = null,
  var additionalNeedsInformation: String? = null,
  var needsInterpreter: Boolean? = null,
  var interpreterLanguage: String? = null,
  val reasonForChange: String,
)
