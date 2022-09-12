package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto

import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Changelog

data class ChangelogUpdateDTO(
  val changelog: Changelog,
  val oldComplexityLevelTitle: String? = null,
  val newComplexityLevelTitle: String? = null,
  val oldDesiredOutcomes: List<String> = emptyList(),
  val newDesiredOutcomes: List<String> = emptyList(),
  val oldDescription: String? = null,
  val newDescription: String? = null
)
