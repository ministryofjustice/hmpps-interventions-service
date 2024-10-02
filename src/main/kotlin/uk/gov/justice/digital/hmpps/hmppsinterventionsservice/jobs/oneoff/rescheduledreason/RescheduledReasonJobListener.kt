package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jobs.oneoff.rescheduledreason

import mu.KLogging
import net.logstash.logback.argument.StructuredArguments.kv
import org.springframework.batch.core.BatchStatus
import org.springframework.batch.core.JobExecution
import org.springframework.batch.core.JobExecutionListener
import org.springframework.stereotype.Component

@Component
class RescheduledReasonJobListener : JobExecutionListener {
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
          "unexpected status encountered for rescheduled reason job {} {}",
          kv("status", jobExecution.status),
          kv("exitDescription", jobExecution.exitStatus.exitDescription),
        )
      }
    }
  }
}
