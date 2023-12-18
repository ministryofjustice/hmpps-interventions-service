package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.reporting.serviceprovider.performance

import mu.KLogging
import net.logstash.logback.argument.StructuredArguments
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.FileSystemResource
import org.springframework.stereotype.Component
import org.springframework.web.util.UriComponentsBuilder
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.component.EmailSender
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.config.S3Bucket
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ReferralRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.reporting.BatchUtils
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.S3Service
import java.io.File
import java.nio.file.Path
import java.time.OffsetDateTime
import kotlin.io.path.createTempDirectory

@Component
class PerformanceReportHandler(
  private val referralRepository: ReferralRepository,
  private val batchUtils: BatchUtils,
  private val processor: PerformanceReportProcessor,
  private val s3Service: S3Service,
  private val storageS3Bucket: S3Bucket,
  private val emailSender: EmailSender,
  @Value("\${notify.templates.service-provider-performance-report-ready}") private val successNotifyTemplateId: String,
  @Value("\${notify.templates.service-provider-performance-report-failed}") private val failureNotifyTemplateId: String,
  @Value("\${interventions-ui.baseurl}") private val interventionsUiBaseUrl: String,
  @Value("\${interventions-ui.locations.service-provider.download-report}") private val downloadLocation: String,
) {

  companion object : KLogging()

  fun handleReport(
    contractReferences: List<String>,
    from: OffsetDateTime,
    to: OffsetDateTime,
    userId: String,
    userEmail: String,
    userFirstname: String,
    timestamp: String,
  ) {
    val tempFilePath = beforeProcess(userId, timestamp)

    try {
      process(contractReferences, from, to, tempFilePath)
      afterProcessSuccess(tempFilePath, userEmail, userFirstname)
    } catch (e: Exception) {
      afterProcessFail(userEmail, userFirstname)
    } finally {
      cleanup(tempFilePath)
    }
  }

  private fun beforeProcess(userId: String, timestamp: String): Path {
    // create a temp file for the handler to write to and store the path in the execution context
    val path = createTempDirectory().resolve("${userId}_${timestamp}.csv")

    PerformanceReportHandler.logger.debug("creating csv file for service provider performance report {}", StructuredArguments.kv("path", path))
    return path
  }

  private fun process(contractReferences: List<String>, from: OffsetDateTime, to: OffsetDateTime, path: Path) {
    val referrals = referralRepository.findPerformanceReportReferralList(
      from = from,
      to = to,
      contractReferences = contractReferences,
    )

    val writer = batchUtils.csvFileWriter<PerformanceReportData>(
      "performanceReportWriter",
      FileSystemResource(path),
      PerformanceReportData.headers,
      PerformanceReportData.fields,
    )

    val reportReferrals = referrals.map { processor.process(it) }

    writer.write(reportReferrals)
  }

  fun afterProcessSuccess(path: Path, userEmail: String, userFirstname: String) {
    s3Service.publishFileToS3(storageS3Bucket, path, "reports/service-provider/performance/")

    emailSender.sendEmail(
      successNotifyTemplateId,
      userEmail,
      mapOf(
        "serviceProviderFirstName" to userFirstname,
        "reportUrl" to UriComponentsBuilder.fromHttpUrl(interventionsUiBaseUrl)
          .path(downloadLocation)
          .buildAndExpand(path.fileName)
          .toString(),
      ),
    )
  }

  fun afterProcessFail(userEmail: String, userFirstname: String) {
    emailSender.sendEmail(
      failureNotifyTemplateId,
      userEmail,
      mapOf(
        "serviceProviderFirstName" to userFirstname,
      ),
    )
  }

  private fun cleanup(path: Path) {
    // delete the temporary csv file regardless of the job status
    PerformanceReportHandler.logger.debug("deleting csv file for service provider report {}", StructuredArguments.kv("path", path))
    File(path.toString()).delete()
  }
}
