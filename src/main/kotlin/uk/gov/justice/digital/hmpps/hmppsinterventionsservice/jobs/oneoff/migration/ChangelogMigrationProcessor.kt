package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jobs.oneoff.migration

import mu.KLogging
import org.springframework.batch.item.ItemProcessor
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Changelog
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ReferralDetails
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.AuthUserRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ChangelogRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ReferralDetailsRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.AmendTopic
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.ChangeLogMigrationService

@Component
class ChangelogMigrationProcessor(
  private val changeLogMigrationService: ChangeLogMigrationService,
  private val changelogRepository: ChangelogRepository,
  private val referralDetailsRepository: ReferralDetailsRepository,
  private val authUserRepository: AuthUserRepository
) : ItemProcessor<ReferralDetails, Changelog?> {
  companion object : KLogging()

  override fun process(referralDetails: ReferralDetails): Changelog? {
    logger.info("processing referral details {} for referral id {}", referralDetails.id, referralDetails.referralId)
    // check if any changlog is null
    return try {
      // Do this migration when there is no change log data
      migrateReferralToChangelogTable(referralDetails)
    } catch (e: Exception) {
      // log and continue
      logger.error("logging the referral details that failed migration {}, {}", referralDetails.id, e.message)
      throw e
    }
  }

  private fun migrateReferralToChangelogTable(referralDetails: ReferralDetails): Changelog {
    logger.info("referral details that needs migrations is = {}", referralDetails.id)
    val supersededId = referralDetails.supersededById!!
    val supersededReferralDetails = referralDetailsRepository.findById(supersededId).get()

    // compare what is changed between the two referral details
    logger.info("superseded referral details is = {}", supersededReferralDetails.id)

    return changeLogMigrationService.logChanges(
      referralDetails,
      if (referralDetails.completionDeadline != supersededReferralDetails.completionDeadline) supersededReferralDetails.completionDeadline.toString() else supersededReferralDetails.maximumEnforceableDays.toString(),
      if (referralDetails.completionDeadline != supersededReferralDetails.completionDeadline) AmendTopic.COMPLETION_DATETIME else AmendTopic.MAXIMUM_ENFORCEABLE_DAYS,
      authUserRepository.findById(supersededReferralDetails.createdByUserId).get(),
      supersededReferralDetails.reasonForChange
    )
  }
}
