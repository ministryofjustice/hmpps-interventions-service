package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jobs.oneoff.migration

import mu.KLogging
import org.springframework.batch.core.JobExecution
import org.springframework.batch.core.JobExecutionListener
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ChangelogRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.AmendTopic

@Component
class ChangelogMigrationJobListener(
  val changelogRepository: ChangelogRepository
) : JobExecutionListener {
  companion object : KLogging()

  override fun beforeJob(jobExecution: JobExecution) {
    logger.info("Referral Details to Change log Batch Job Starts")
    logger.info("Number of change log items before Job is = {}", changelogRepository.count())
    val changeLogs = changelogRepository.findAll()
    val changeLogsFilteredElements = changeLogs.filter { it.topic == AmendTopic.COMPLETION_DATETIME || it.topic == AmendTopic.MAXIMUM_ENFORCEABLE_DAYS }
    val changelogWithNull = changeLogs.filter { changelog -> changelog.oldVal.values.contains("null") }
    logger.info("Number of change log items with null value before Job is = {}", changelogWithNull.size)
    logger.info("Deleting all the change log with COMPLETION_DATETIME and MAXIMUM_ENFORCEABLE_DAYS")
    changeLogsFilteredElements.forEach { changelogRepository.deleteById(it.id) }
  }

  override fun afterJob(jobExecution: JobExecution) {
    logger.info("Referral Details to Change log Batch Job Ends")
    logger.info("Number of change log items after Job is = {}", changelogRepository.count())
    val changelogWithNull = changelogRepository.findAll().filter { changelog -> changelog.oldVal.values.contains("null") }
    logger.info("Number of change log items with null value after Job is = {}", changelogWithNull.size)
  }
}
