package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto

import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Changelog
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.AmendTopic
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.UserDetail
import java.time.format.DateTimeFormatter
import java.util.Map
import java.util.UUID

data class ChangelogValuesDTO(
  val changelogId: UUID,
  val referralId: UUID,
  val topic: AmendTopic,
  val description: String,
  val changedAt: String,
  val name: String,
  val reasonForChange: String
) {

  companion object {
    private var amendTopicDescription: MutableMap<AmendTopic, String> = Map.of(
      AmendTopic.COMPLEXITY_LEVEL, "Complexity level was changed",
      AmendTopic.DESIRED_OUTCOMES, "Desired outcome was changed",
      AmendTopic.COMPLETION_DATETIME, "Completion date time was changed",
      AmendTopic.MAXIMUM_ENFORCEABLE_DAYS, "Maximum enforceable days was changed",
      AmendTopic.NEEDS_AND_REQUIREMENTS_ACCESSIBILITY_NEEDS, "Accessibility needs was changed",
      AmendTopic.NEEDS_AND_REQUIREMENTS_ADDITIONAL_INFORMATION, "Additional information was changed",
      AmendTopic.NEEDS_AND_REQUIREMENTS_HAS_ADDITIONAL_RESPONSIBILITIES, "Additional responsibilities was changed",
      AmendTopic.NEEDS_AND_REQUIREMENTS_INTERPRETER_REQUIRED, "Interpreter required was changed"
    )
    fun from(changelog: Changelog, userDetail: UserDetail): ChangelogValuesDTO {
      val dateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy 'at' HH:mma")
      return ChangelogValuesDTO(
        changelogId = changelog.id,
        referralId = changelog.referralId,
        topic = changelog.topic,
        description = amendTopicDescription[changelog.topic]!!,
        changedAt = changelog.changedAt.format(dateTimeFormatter),
        name = userDetail.firstName + ' ' + userDetail.lastName,
        reasonForChange = changelog.reasonForChange
      )
    }
  }
}
