package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto

import java.time.OffsetDateTime
import java.util.UUID

data class ChangelogValuesDTO(
  val changelogId: UUID,
  val referralId: UUID,
  val topic: String,
  val changedAt: OffsetDateTime,
  val name: String,
  val reasonForChange: String
)
