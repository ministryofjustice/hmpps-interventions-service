package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.reporting.ndmis.performance

import mu.KLogging
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.batch.core.BatchStatus
import org.springframework.batch.core.job.Job
import org.springframework.batch.core.job.JobExecution
import org.springframework.batch.core.job.parameters.JobParametersBuilder
import org.springframework.batch.core.launch.JobOperator
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.test.JobOperatorTestUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Attended
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.reporting.TimestampIncrementer
import java.time.OffsetDateTime
import kotlin.io.path.createTempDirectory
import kotlin.io.path.pathString

class NdmisPerformanceReportJobConfigurationTest : IntegrationTestBase() {

  companion object : KLogging()

  // the default job operator runs jobs synchronously, so each job has finished when startJob returns
  @Autowired
  @Qualifier("jobOperator")
  lateinit var jobOperator: JobOperator

  @Autowired
  lateinit var jobRepository: JobRepository

  @Autowired
  @Qualifier("ndmisReferralPerformanceReportJob")
  lateinit var referralJob: Job

  @Autowired
  @Qualifier("ndmisAppointmentPerformanceReportJob")
  lateinit var appointmentJob: Job

  @Autowired
  @Qualifier("ndmisComplexityPerformanceReportJob")
  lateinit var complexityJob: Job

  @Autowired
  @Qualifier("ndmisOutcomePerformanceReportJob")
  lateinit var outcomeJob: Job

  private val outputDir = createTempDirectory("test")

  fun executeJob(job: Job): JobExecution {
    val parameters = JobParametersBuilder()
      .addString("outputPath", outputDir.pathString)
      .toJobParameters()
    val parametersWithTimestamp = TimestampIncrementer().getNext(parameters)
    return JobOperatorTestUtils(jobOperator, jobRepository)
      .apply { setJob(job) }
      .startJob(parametersWithTimestamp)
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

    val execution1 = executeJob(referralJob)
    val execution2 = executeJob(appointmentJob)
    val execution3 = executeJob(complexityJob)
    val execution4 = executeJob(outcomeJob)

    assertThat(execution1.status).isEqualTo(BatchStatus.COMPLETED)
    assertThat(execution2.status).isEqualTo(BatchStatus.COMPLETED)
    assertThat(execution3.status).isEqualTo(BatchStatus.COMPLETED)
    assertThat(execution4.status).isEqualTo(BatchStatus.COMPLETED)

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
