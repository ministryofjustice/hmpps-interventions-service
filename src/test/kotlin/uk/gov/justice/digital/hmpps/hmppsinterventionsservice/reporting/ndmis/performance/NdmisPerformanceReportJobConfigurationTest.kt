package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.reporting.ndmis.performance

import mu.KLogging
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.batch.core.ExitStatus
import org.springframework.batch.core.Job
import org.springframework.batch.core.JobExecution
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.batch.core.launch.support.SimpleJobLauncher
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.test.JobLauncherTestUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Attended
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.reporting.TimestampIncrementer
import java.time.OffsetDateTime
import kotlin.io.path.createTempDirectory
import kotlin.io.path.pathString

@Component
class NdmisPerformanceJobLauncherTestUtils : JobLauncherTestUtils() {

  @Autowired
  override fun setJobRepository(@Qualifier("batchJobRepository") jobRepository: JobRepository) = super.setJobRepository(jobRepository)

  @Autowired
  fun testJobLauncher(@Qualifier("jobRepository") jobRepository: JobRepository) {
    val testJobLauncher = SimpleJobLauncher()
    testJobLauncher.setJobRepository(jobRepository)
    testJobLauncher.afterPropertiesSet()
    super.setJobLauncher(testJobLauncher)
  }

  @Autowired
  override fun setJob(@Qualifier("ndmisPerformanceReportJob") job: Job) {
    super.setJob(job)
  }
}

class NdmisPerformanceReportJobConfigurationTest : IntegrationTestBase() {

  companion object : KLogging()

  @Autowired
  lateinit var jobLauncher: NdmisPerformanceJobLauncherTestUtils

  private val outputDir = createTempDirectory("test")

  fun executeJob(): JobExecution {
    val parameters = JobParametersBuilder()
      .addString("outputPath", outputDir.pathString)
      .toJobParameters()
    val parametersWithTimestamp = TimestampIncrementer().getNext(parameters)
    return jobLauncher.launchJob(parametersWithTimestamp)
  }

  @Test
  fun jobWritesNonEmptyCsvExportFiles() {
    // job writes non-empty CSV export files
    val referral = setupAssistant.createSentReferral()
      .also { setupAssistant.fillReferralFields(it) }
      .also { setupAssistant.addEndOfServiceReportWithOutcome(referral = it) }
    setupAssistant.createDeliverySession(
      sessionNumber = 1,
      duration = 60,
      appointmentTime = OffsetDateTime.now(),
      attended = Attended.YES,
      referral = referral,
    )

    logger.info { "We are setup" }

    val parameters = JobParametersBuilder()
      .addString("outputPath", outputDir.pathString)
      .toJobParameters()
    val parametersWithTimestamp = TimestampIncrementer().getNext(parameters)

    val execution = executeJob()

    assertThat(execution.exitStatus).isEqualTo(ExitStatus.COMPLETED)

    assertThat(outputDir.resolve("crs_performance_report-v2-referrals.csv"))
      .content().contains(referral.referenceNumber)
    assertThat(outputDir.resolve("crs_performance_report-v2-complexity.csv"))
      .content().contains(referral.referenceNumber)
    assertThat(outputDir.resolve("crs_performance_report-v2-appointments.csv"))
      .content().contains(referral.referenceNumber)
    assertThat(outputDir.resolve("crs_performance_report-v2-outcomes.csv"))
      .content().contains(referral.referenceNumber)
  }
}
