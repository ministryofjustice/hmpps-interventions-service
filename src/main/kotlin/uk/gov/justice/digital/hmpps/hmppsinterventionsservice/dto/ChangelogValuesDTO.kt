package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto

import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Changelog
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.AmendTopic
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.UserDetail
import java.time.OffsetDateTime
import java.util.UUID

data class ChangelogValuesDTO(
  val changelogId: UUID,
  val referralId: UUID,
  val topic: AmendTopic,
  val changedAt: OffsetDateTime,
  val name: String,
  val reasonForChange: String
) {
  companion object {
    fun from(changelog: Changelog, userDetail: UserDetail): ChangelogValuesDTO {
      return ChangelogValuesDTO(
        changelogId = changelog.id,
        referralId = changelog.referralId,
        topic = changelog.topic,
        changedAt = changelog.changedAt,
        name = userDetail.firstName + ' ' + userDetail.lastName,
        reasonForChange = changelog.reasonForChange
      )
    }
  }
}
