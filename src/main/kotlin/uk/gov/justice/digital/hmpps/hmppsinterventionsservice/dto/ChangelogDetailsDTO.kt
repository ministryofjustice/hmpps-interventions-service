package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto

import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Changelog
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.AmendTopic
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.UserDetail
import java.util.UUID

data class ChangelogDetailsDTO(
  val changelogId: UUID,
  val referralId: UUID,
  val topic: AmendTopic,
  val oldValue: List<String>,
  val newValue: List<String>,
  val name: String,
  val reasonForChange: String
) {
  companion object {
    fun from(changelog: Changelog, userDetail: UserDetail): ChangelogDetailsDTO {
      return ChangelogDetailsDTO(
        changelogId = changelog.id,
        referralId = changelog.referralId,
        topic = changelog.topic,
        oldValue = changelog.oldVal.values,
        newValue = changelog.newVal.values,
        name = userDetail.firstName + ' ' + userDetail.lastName,
        reasonForChange = changelog.reasonForChange
      )
    }
  }
}
