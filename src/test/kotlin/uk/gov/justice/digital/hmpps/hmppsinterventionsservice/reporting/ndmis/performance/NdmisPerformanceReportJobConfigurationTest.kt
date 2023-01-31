package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.reporting.ndmis.performance

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.batch.core.ExitStatus
import org.springframework.batch.core.Job
import org.springframework.batch.core.JobExecution
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.batch.test.JobLauncherTestUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.reporting.TimestampIncrementer
import kotlin.io.path.createTempDirectory
import kotlin.io.path.fileSize
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.pathString

@Component
class NdmisPerformanceJobLauncherTestUtils : JobLauncherTestUtils() {
  @Autowired
  override fun setJob(@Qualifier("ndmisPerformanceReportJob") job: Job) = super.setJob(job)
}

class NdmisPerformanceReportJobConfigurationTest : IntegrationTestBase() {
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
  fun `job writes non-empty CSV export files`() {
    val execution = executeJob()
    assertThat(execution.exitStatus).isEqualTo(ExitStatus.COMPLETED)
    assertThat(outputDir.listDirectoryEntries().filter { it.fileSize() > 0 }.map { it.name })
      .containsExactlyInAnyOrder(
        "crs_performance_report-v2-referrals.csv",
        "crs_performance_report-v2-complexity.csv",
        "crs_performance_report-v2-appointments.csv",
      )
  }
}
