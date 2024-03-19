package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.reporting.ndmis.performance

import mu.KLogging
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.batch.core.ExitStatus
import org.springframework.batch.core.Job
import org.springframework.batch.core.JobExecution
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.batch.core.launch.support.TaskExecutorJobLauncher
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
class NdmisAppointmentPerformanceJobLauncherTestUtils : JobLauncherTestUtils() {

  @Autowired
  override fun setJobRepository(jobRepository: JobRepository) = super.setJobRepository(jobRepository)

  @Autowired
  fun testJobLauncher(jobRepository: JobRepository) {
    val testJobLauncher = TaskExecutorJobLauncher()
    testJobLauncher.setJobRepository(jobRepository)
    testJobLauncher.afterPropertiesSet()
    super.setJobLauncher(testJobLauncher)
  }

  @Autowired
  override fun setJob(@Qualifier("ndmisAppointmentPerformanceReportJob") job: Job) {
    super.setJob(job)
  }
}
@Component
class NdmisReferralPerformanceJobLauncherTestUtils : JobLauncherTestUtils() {

  @Autowired
  override fun setJobRepository(jobRepository: JobRepository) = super.setJobRepository(jobRepository)

  @Autowired
  fun testJobLauncher(jobRepository: JobRepository) {
    val testJobLauncher = TaskExecutorJobLauncher()
    testJobLauncher.setJobRepository(jobRepository)
    testJobLauncher.afterPropertiesSet()
    super.setJobLauncher(testJobLauncher)
  }

  @Autowired
  override fun setJob(@Qualifier("ndmisReferralPerformanceReportJob") job: Job) {
    super.setJob(job)
  }
}
@Component
class NdmisComplexityPerformanceJobLauncherTestUtils : JobLauncherTestUtils() {

  @Autowired
  override fun setJobRepository(jobRepository: JobRepository) = super.setJobRepository(jobRepository)

  @Autowired
  fun testJobLauncher(jobRepository: JobRepository) {
    val testJobLauncher = TaskExecutorJobLauncher()
    testJobLauncher.setJobRepository(jobRepository)
    testJobLauncher.afterPropertiesSet()
    super.setJobLauncher(testJobLauncher)
  }

  @Autowired
  override fun setJob(@Qualifier("ndmisComplexityPerformanceReportJob") job: Job) {
    super.setJob(job)
  }
}
@Component
class NdmisOutcomePerformanceJobLauncherTestUtils : JobLauncherTestUtils() {

  @Autowired
  override fun setJobRepository(jobRepository: JobRepository) = super.setJobRepository(jobRepository)

  @Autowired
  fun testJobLauncher(jobRepository: JobRepository) {
    val testJobLauncher = TaskExecutorJobLauncher()
    testJobLauncher.setJobRepository(jobRepository)
    testJobLauncher.afterPropertiesSet()
    super.setJobLauncher(testJobLauncher)
  }

  @Autowired
  override fun setJob(@Qualifier("ndmisOutcomePerformanceReportJob") job: Job) {
    super.setJob(job)
  }
}

class NdmisPerformanceReportJobConfigurationTest : IntegrationTestBase() {

  companion object : KLogging()

  @Autowired
  lateinit var referralJobLauncher: NdmisReferralPerformanceJobLauncherTestUtils

  @Autowired
  lateinit var appointmentJobLauncher: NdmisAppointmentPerformanceJobLauncherTestUtils

  @Autowired
  lateinit var complexityJobLauncher: NdmisComplexityPerformanceJobLauncherTestUtils

  @Autowired
  lateinit var outcomeJobLauncher: NdmisOutcomePerformanceJobLauncherTestUtils

  private val outputDir = createTempDirectory("test")

  fun executeJob(jobLauncher: JobLauncherTestUtils): JobExecution {
    val parameters = JobParametersBuilder()
      .addString("outputPath", outputDir.pathString)
      .toJobParameters()
    val parametersWithTimestamp = TimestampIncrementer().getNext(parameters)
    return jobLauncher.launchJob(parametersWithTimestamp)
  }

  @Test
  fun jobsWriteNonEmptyCsvExportFiles() {
    // jobs write non-empty CSV export files
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

    val parameters = JobParametersBuilder()
      .addString("outputPath", outputDir.pathString)
      .toJobParameters()
    val parametersWithTimestamp = TimestampIncrementer().getNext(parameters)

    val execution1 = executeJob(referralJobLauncher)
    val execution2 = executeJob(appointmentJobLauncher)
    val execution3 = executeJob(complexityJobLauncher)
    val execution4 = executeJob(outcomeJobLauncher)

    assertThat(execution1.exitStatus).isEqualTo(ExitStatus.COMPLETED)
    assertThat(execution2.exitStatus).isEqualTo(ExitStatus.COMPLETED)
    assertThat(execution3.exitStatus).isEqualTo(ExitStatus.COMPLETED)
    assertThat(execution4.exitStatus).isEqualTo(ExitStatus.COMPLETED)

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
