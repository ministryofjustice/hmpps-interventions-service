package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto

import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Changelog
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.AmendTopic
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.UserDetail
import java.time.format.DateTimeFormatter
import java.util.UUID

data class ChangelogValuesDTO(
  val changelogId: UUID,
  val referralId: UUID,
  val topic: AmendTopic,
  val description: String,
  val changedAt: String,
  val name: String,
  val reasonForChange: String,
) {

  companion object {
    private val amendTopicDescription: Map<AmendTopic, String> = mapOf(
      AmendTopic.COMPLEXITY_LEVEL to "Complexity level was changed",
      AmendTopic.DESIRED_OUTCOMES to "Desired outcome was changed",
      AmendTopic.COMPLETION_DATETIME to "Completion date time was changed",
      AmendTopic.MAXIMUM_ENFORCEABLE_DAYS to "Maximum enforceable days was changed",
      AmendTopic.NEEDS_AND_REQUIREMENTS_ACCESSIBILITY_NEEDS to "Accessibility needs was changed",
      AmendTopic.NEEDS_AND_REQUIREMENTS_ADDITIONAL_INFORMATION to "Additional information was changed",
      AmendTopic.NEEDS_AND_REQUIREMENTS_HAS_ADDITIONAL_RESPONSIBILITIES to "Additional responsibilities was changed",
      AmendTopic.NEEDS_AND_REQUIREMENTS_INTERPRETER_REQUIRED to "Interpreter required was changed",
      AmendTopic.REASON_FOR_REFERRAL to "Reason for this referral and further information has changed",
      AmendTopic.PRISON_ESTABLISHMENT to "Prison establishment has changed",
      AmendTopic.EXPECTED_RELEASE_DATE to "Expected release date has changed",
      AmendTopic.EXPECTED_PROBATION_OFFICE to "Expected probation office has changed",
      AmendTopic.PROBATION_PRACTITIONER_PROBATION_OFFICE to "Probation practitioner probation office has changed",
      AmendTopic.PROBATION_PRACTITIONER_NAME to "Probation practitioner name has changed",
    )
    fun from(changelog: Changelog, userDetail: UserDetail): ChangelogValuesDTO {
      val dateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy 'at' h.mma")
      return ChangelogValuesDTO(
        changelogId = changelog.id,
        referralId = changelog.referralId,
        topic = changelog.topic,
        description = amendTopicDescription[changelog.topic]!!,
        changedAt = changelog.changedAt.format(dateTimeFormatter).replace("AM", "am").replace("PM", "pm"),
        name = userDetail.firstName + ' ' + userDetail.lastName,
        reasonForChange = changelog.reasonForChange,
      )
    }
  }
}
