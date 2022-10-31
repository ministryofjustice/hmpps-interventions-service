package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jobs.oneoff.migration

import mu.KLogging
import org.springframework.batch.item.ItemProcessor
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.ReferralAmendmentDetails
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Changelog
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ReferralDetails
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.AuthUserRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ChangelogRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ReferralDetailsRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.AmendTopic
import java.util.UUID

@Component
class ChangelogMigrationProcessor(
  private val changelogRepository: ChangelogRepository,
  private val referralDetailsRepository: ReferralDetailsRepository,
  private val authUserRepository: AuthUserRepository
) : ItemProcessor<ReferralDetails, Changelog?> {
  companion object : KLogging()

  override fun process(referralDetails: ReferralDetails): Changelog? {
    logger.info("processing referral details {} for referral id {}", referralDetails.id, referralDetails.referralId)
    // check if any changlog is null
    if (referralDetails.supersededById != null) {
      return try {
        // Do this migration when there is no change log data
        migrateReferralToChangelogTable(referralDetails)
      } catch (e: Exception) {
        // log and continue
        logger.info("logging the referral details that failed migration {}, {}", referralDetails.id, e.message)
        null
      }
    }
    return null
  }

  private fun migrateReferralToChangelogTable(it: ReferralDetails): Changelog {
    logger.info("referral details that needs migrations is = {}", it.id)
    val supersededId = it.supersededById!!
    val supersededReferralDetails = referralDetailsRepository.findById(supersededId).get()

    // compare what is changed between the two referral details
    logger.info("superseded referral details is = {}", supersededReferralDetails.id)

    if (it.completionDeadline != supersededReferralDetails.completionDeadline) {
      // means completion deadline got changed
      val oldValue = ReferralAmendmentDetails(listOf(it.completionDeadline.toString()))
      val newValue = ReferralAmendmentDetails(listOf(supersededReferralDetails.completionDeadline.toString()))

      val changelog = Changelog(
        supersededReferralDetails.referralId,
        UUID.randomUUID(),
        AmendTopic.COMPLETION_DATETIME,
        oldValue,
        newValue,
        supersededReferralDetails.reasonForChange,
        it.createdAt,
        authUserRepository.findById(supersededReferralDetails.createdByUserId).get()
      )
      return changelogRepository.save(changelog)
    } else {

      // means max enforceable days

      val oldValue = ReferralAmendmentDetails(listOf(it.maximumEnforceableDays.toString()))
      val newValue = ReferralAmendmentDetails(listOf(supersededReferralDetails.maximumEnforceableDays.toString()))

      val changelog = Changelog(
        supersededReferralDetails.referralId,
        UUID.randomUUID(),
        AmendTopic.MAXIMUM_ENFORCEABLE_DAYS,
        oldValue,
        newValue,
        supersededReferralDetails.reasonForChange,
        it.createdAt,
        authUserRepository.findById(supersededReferralDetails.createdByUserId).get()
      )
      return changelogRepository.save(changelog)
    }
  }
}
