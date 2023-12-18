package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.reporting.serviceprovider
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.component.EmailSender
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.config.S3Bucket
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ReferralRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.reporting.BatchUtils
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.reporting.serviceprovider.performance.PerformanceReportHandler
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.reporting.serviceprovider.performance.PerformanceReportProcessor
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.S3Service
import java.nio.file.Path
internal class PerformanceReportHandlerTest {
  private val s3Service = mock<S3Service>()
  private val s3Bucket = mock<S3Bucket>()
  private val emailSender = mock<EmailSender>()
  private val processor = mock<PerformanceReportProcessor>()
  private val referralRepository = mock<ReferralRepository>()
  private val batchUtils = mock<BatchUtils>()
  private val path = mock<Path>()

  // class under test
  private val handler = PerformanceReportHandler(
    referralRepository,
    batchUtils,
    processor,
    s3Service,
    s3Bucket,
    emailSender,
    "success-email-template",
    "failure-email-template",
    "https://interventions.com",
    "/sp-report-download?file={filename}",
  )

  @Test
  fun `afterProcessSuccess sends email with correct name and url on task completion`() {
    handler.afterProcessSuccess(path, "tom@tom.tom", "tom")
    verify(emailSender).sendEmail(
      "success-email-template",
      "tom@tom.tom",
      mapOf("serviceProviderFirstName" to "tom", "reportUrl" to "https://interventions.com/sp-report-download?file="),
    )
  }

  @Test
  fun `afterProcessFail sends email with correct name on task failure`() {
    handler.afterProcessFail("tom@tom.tom", "tom")
    verify(emailSender).sendEmail(
      "failure-email-template",
      "tom@tom.tom",
      mapOf("serviceProviderFirstName" to "tom"),
    )
  }
}
