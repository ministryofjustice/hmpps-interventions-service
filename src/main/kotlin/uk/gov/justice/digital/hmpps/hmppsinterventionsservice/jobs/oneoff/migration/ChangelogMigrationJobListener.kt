package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jobs.oneoff.migration

import mu.KLogging
import org.springframework.batch.core.JobExecution
import org.springframework.batch.core.JobExecutionListener
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ChangelogRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.AmendTopic
import javax.transaction.Transactional

@Component
@Transactional
class ChangelogMigrationJobListener(
  val changelogRepository: ChangelogRepository
) : JobExecutionListener {
  companion object : KLogging()

  override fun beforeJob(jobExecution: JobExecution) {
    logger.info("Referral Details to Change log Batch Job Starts")
    logger.info("Number of change log items before Job is = {}", changelogRepository.count())
    val changeLogs = changelogRepository.findByTopicsIn(listOf(AmendTopic.COMPLETION_DATETIME, AmendTopic.MAXIMUM_ENFORCEABLE_DAYS))
    logger.info("Number of change log items that needs migration is = {}", changeLogs.size)
    val completionDeadLineDeleted = changelogRepository.deleteByTopic(AmendTopic.COMPLETION_DATETIME)
    logger.info("No of completion dead lines deleted = {}", completionDeadLineDeleted)
    val maxEnforceableDaysDeleted = changelogRepository.deleteByTopic(AmendTopic.MAXIMUM_ENFORCEABLE_DAYS)
    logger.info("No of max enforceable days deleted = {}", maxEnforceableDaysDeleted)
  }

  override fun afterJob(jobExecution: JobExecution) {
    logger.info("Referral Details to Change log Batch Job Ends")
    logger.info("Number of change log items after Job is = {}", changelogRepository.count())
    val changeLogs = changelogRepository.findByTopicsIn(listOf(AmendTopic.COMPLETION_DATETIME, AmendTopic.MAXIMUM_ENFORCEABLE_DAYS))
    logger.info("Number of change log items that was migrated is = {}", changeLogs.size)
  }
}
