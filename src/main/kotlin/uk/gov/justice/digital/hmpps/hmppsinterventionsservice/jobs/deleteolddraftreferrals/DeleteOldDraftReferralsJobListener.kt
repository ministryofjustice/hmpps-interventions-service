package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jobs.oneoff.transferreferrals

import mu.KLogging
import net.logstash.logback.argument.StructuredArguments.kv
import org.springframework.batch.core.BatchStatus
import org.springframework.batch.core.JobExecution
import org.springframework.batch.core.JobExecutionListener
import org.springframework.stereotype.Component

@Component
class DeleteOldDraftReferralsJobListener : JobExecutionListener {
  companion object : KLogging()

  override fun beforeJob(jobExecution: JobExecution) {
  }

  override fun afterJob(jobExecution: JobExecution) {
    when (jobExecution.status) {
      BatchStatus.COMPLETED -> {
      }

      BatchStatus.FAILED -> {
      }

      else -> {
        logger.warn(
          "unexpected status encountered for deleting draft referrals which are 90 days old {} {}",
          kv("status", jobExecution.status),
          kv("exitDescription", jobExecution.exitStatus.exitDescription),
        )
      }
    }
  }
}
