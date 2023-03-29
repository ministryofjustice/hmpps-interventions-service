package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto

import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Changelog

data class ChangelogUpdateDTO(
  val changelog: Changelog,
  val oldValue: String? = null,
  val newValue: String? = null,
  val oldValues: List<String> = emptyList(),
  val newValues: List<String> = emptyList(),
)
