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
) {
  companion object {
    fun from(changelogValuesDTO: ChangelogValuesDTO): ChangelogValuesDTO {
      return ChangelogValuesDTO(
        changelogId = changelogValuesDTO.changelogId,
        referralId = changelogValuesDTO.referralId,
        topic = changelogValuesDTO.topic,
        changedAt = changelogValuesDTO.changedAt,
        name = changelogValuesDTO.name,
        reasonForChange = changelogValuesDTO.reasonForChange,
      )
    }
  }
}
