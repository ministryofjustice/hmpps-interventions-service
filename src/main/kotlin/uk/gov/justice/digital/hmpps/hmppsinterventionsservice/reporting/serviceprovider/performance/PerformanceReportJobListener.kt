package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.reporting.serviceprovider.performance

import mu.KLogging
import net.logstash.logback.argument.StructuredArguments.kv
import org.springframework.batch.core.BatchStatus
import org.springframework.batch.core.job.JobExecution
import org.springframework.batch.core.listener.JobExecutionListener
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
class PerformanceReportJobListener(
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
    // create a temp file for the job to write to and store the path in the execution context
    val params = jobExecution.jobParameters
    val id = requireNotNull(params.getString("user.id")) { "Missing user.id job parameter" }
    val timestamp = requireNotNull(params.getString("timestamp")) { "Missing timestamp job parameter" }
    val path = createTempDirectory().resolve(id + "_" + timestamp + ".csv")

    logger.debug("creating csv file for service provider performance report {}", kv("path", path))

    jobExecution.executionContext.put("output.file.path", path.toString())
  }

  override fun afterJob(jobExecution: JobExecution) {
    val path = Path(requireNotNull(jobExecution.executionContext.getString("output.file.path")) { "Missing output.file.path job context value" })

    when (jobExecution.status) {
      BatchStatus.COMPLETED -> {
        s3Service.publishFileToS3(storageS3Bucket, path, "reports/service-provider/performance/")

        val reportURL = UriComponentsBuilder.fromUriString(interventionsUiBaseUrl)
          .path(downloadLocation)
          .buildAndExpand(path.fileName)
          .toString()

        emailSender.sendEmail(
          successNotifyTemplateId,
          requireNotNull(jobExecution.jobParameters.getString("user.email")) { "Missing user.email job parameter" },
          mapOf(
            "serviceProviderFirstName" to requireNotNull(jobExecution.jobParameters.getString("user.firstName")) { "Missing user.firstName job parameter" },
            "reportUrl" to reportURL,
          ),
        )
      }
      BatchStatus.FAILED -> {
        logger.error(
          "status FAILED for performance report {}",
          kv("exitDescription", jobExecution.exitStatus.exitDescription),
        )
        emailSender.sendEmail(
          failureNotifyTemplateId,
          requireNotNull(jobExecution.jobParameters.getString("user.email")) { "Missing user.email job parameter" },
          mapOf(
            "serviceProviderFirstName" to requireNotNull(jobExecution.jobParameters.getString("user.firstName")) { "Missing user.firstName job parameter" },
            "jobInstanceId" to jobExecution.jobInstance.id.toString(),
          ),
        )
      }
      else -> {
        logger.warn(
          "unexpected status encountered for performance report {} {}",
          kv("status", jobExecution.status),
          kv("exitDescription", jobExecution.exitStatus.exitDescription),
        )
      }
    }

    // delete the temporary csv file regardless of the job status
    logger.debug("deleting csv file for service provider report {}", kv("path", path))
    // comment the below line, then get file location from logs to test locally.
    File(path.toString()).delete()
  }
}
