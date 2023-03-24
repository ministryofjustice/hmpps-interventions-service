package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto

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
  val reasonForChange: String,
) {
  companion object {
    fun from(changelogUpdateDTO: ChangelogUpdateDTO, userDetail: UserDetail): ChangelogDetailsDTO {
      val changelog = changelogUpdateDTO.changelog
      var oldValue: List<String> = emptyList()
      var newValue: List<String> = emptyList()
      when (changelog.topic) {
        AmendTopic.COMPLEXITY_LEVEL -> {
          oldValue = listOf(changelogUpdateDTO.oldValue!!.uppercase())
          newValue = listOf(changelogUpdateDTO.newValue!!.uppercase())
        }
        AmendTopic.DESIRED_OUTCOMES -> {
          oldValue = changelogUpdateDTO.oldValues
          newValue = changelogUpdateDTO.newValues
        }
        AmendTopic.COMPLETION_DATETIME -> {
          oldValue = listOf(changelogUpdateDTO.oldValue!!)
          newValue = listOf(changelogUpdateDTO.newValue!!)
        }
        AmendTopic.NEEDS_AND_REQUIREMENTS_INTERPRETER_REQUIRED, AmendTopic.NEEDS_AND_REQUIREMENTS_HAS_ADDITIONAL_RESPONSIBILITIES -> {
          oldValue = listOf(changelogUpdateDTO.oldValue!!)
          newValue = listOf(changelogUpdateDTO.newValue!!)
        }
        else -> {}
      }
      return ChangelogDetailsDTO(
        changelogId = changelog.id,
        referralId = changelog.referralId,
        topic = changelog.topic,
        oldValue = oldValue.ifEmpty { changelog.oldVal.values },
        newValue = newValue.ifEmpty { changelog.newVal.values },
        name = userDetail.firstName + ' ' + userDetail.lastName,
        reasonForChange = changelog.reasonForChange,
      )
    }
  }
}
