package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.ReferralAmendmentDetails
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AuthUser
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Changelog
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ReferralDetails
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ChangelogRepository
import java.time.OffsetDateTime
import java.util.UUID

@Component
class ChangeLogMigrationService(val changelogRepository: ChangelogRepository) {

  fun logChanges(referralDetails: ReferralDetails, amendValue: String, topic: AmendTopic, actor: AuthUser, reasonForChange: String): Changelog {
    return if (topic == AmendTopic.COMPLETION_DATETIME) {
      val oldValue = ReferralAmendmentDetails(listOf(referralDetails.completionDeadline.toString()))
      val newValue = ReferralAmendmentDetails(listOf(amendValue))
      processChangeLog(topic, oldValue, newValue, referralDetails.referralId, actor, reasonForChange)
    } else {
      val oldValue = ReferralAmendmentDetails(listOf(referralDetails.maximumEnforceableDays.toString()))
      val newValue = ReferralAmendmentDetails(listOf(amendValue))
      processChangeLog(AmendTopic.MAXIMUM_ENFORCEABLE_DAYS, oldValue, newValue, referralDetails.referralId, actor, reasonForChange)
    }
  }

  private fun processChangeLog(
    amendTopic: AmendTopic,
    oldValue: ReferralAmendmentDetails,
    newValue: ReferralAmendmentDetails,
    referralId: UUID,
    actor: AuthUser,
    reasonForChange: String
  ): Changelog {
    val changelog = Changelog(
      referralId,
      UUID.randomUUID(),
      amendTopic,
      oldValue,
      newValue,
      reasonForChange,
      OffsetDateTime.now(),
      actor
    )

    return changelogRepository.save(changelog)
  }
}
