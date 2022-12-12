package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jobs.movereferrals

import mu.KLogging
import net.logstash.logback.argument.StructuredArguments.kv
import org.springframework.batch.core.BatchStatus
import org.springframework.batch.core.JobExecution
import org.springframework.batch.core.JobExecutionListener
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.util.UriComponentsBuilder
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.component.EmailSender
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.config.S3Bucket
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.S3Service
import java.io.File
import kotlin.io.path.Path
import kotlin.io.path.createTempDirectory

@Component
class MoveReferralsJobListener(
  private val s3Service: S3Service,
  private val storageS3Bucket: S3Bucket,
  private val emailSender: EmailSender,
  @Value("\${notify.templates.service-provider-performance-report-ready}") private val successNotifyTemplateId: String,
  @Value("\${notify.templates.service-provider-performance-report-failed}") private val failureNotifyTemplateId: String,
  @Value("\${interventions-ui.baseurl}") private val interventionsUiBaseUrl: String,
  @Value("\${interventions-ui.locations.service-provider.download-report}") private val downloadLocation: String,
) : JobExecutionListener {
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
          "aaaunexpected status encountered for performance report {} {}",
          kv("status", jobExecution.status),
          kv("exitDescription", jobExecution.exitStatus.exitDescription)
        )
      }
    }

  }
}
