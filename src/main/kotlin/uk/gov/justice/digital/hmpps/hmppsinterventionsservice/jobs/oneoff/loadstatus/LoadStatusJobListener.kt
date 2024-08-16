package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jobs.oneoff.loadstatus

import mu.KLogging
import net.logstash.logback.argument.StructuredArguments.kv
import org.springframework.batch.core.BatchStatus
import org.springframework.batch.core.JobExecution
import org.springframework.batch.core.JobExecutionListener
import org.springframework.stereotype.Component

@Component
class LoadStatusJobListener : JobExecutionListener {
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
          "unexpected status encountered for load status {} {}",
          kv("status", jobExecution.status),
          kv("exitDescription", jobExecution.exitStatus.exitDescription),
        )
      }
    }
  }
}
