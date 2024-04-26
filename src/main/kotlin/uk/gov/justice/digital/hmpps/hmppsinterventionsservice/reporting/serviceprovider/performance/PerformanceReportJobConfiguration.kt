package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.reporting.serviceprovider.performance

import org.hibernate.SessionFactory
import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.core.job.DefaultJobParametersValidator
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.batch.item.ItemProcessor
import org.springframework.batch.item.file.FlatFileItemWriter
import org.springframework.batch.item.support.ListItemReader
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.FileSystemResource
import org.springframework.transaction.PlatformTransactionManager
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jobs.oneoff.OnStartupJobLauncherFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ReferralPerformanceReportRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.reporting.BatchUtils
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.reporting.serviceprovider.performance.model.ReferralPerformanceReport
import java.util.Date

@Configuration
class PerformanceReportJobConfiguration(
  private val jobRepository: JobRepository,
  private val transactionManager: PlatformTransactionManager,
  private val batchUtils: BatchUtils,
  private val listener: PerformanceReportJobListener,
  private val onStartupJobLauncherFactory: OnStartupJobLauncherFactory,
  private val referralPerformanceReportRepository: ReferralPerformanceReportRepository,
  @Value("\${spring.batch.jobs.service-provider.performance-report.chunk-size}") private val chunkSize: Int,
  @Value("\${spring.batch.jobs.service-provider.performance-report.page-size}") private val pageSize: Int,
) {

  @Bean
  fun performanceReportJobLauncher(performanceReportJob: Job): ApplicationRunner {
    return onStartupJobLauncherFactory.makeBatchLauncher(performanceReportJob)
  }

  @Bean
  @JobScope
  fun reader(
    @Value("#{jobParameters['contractReferences']}") contractReferences: String,
    @Value("#{jobParameters['from']}") from: Date,
    @Value("#{jobParameters['to']}") to: Date,
    sessionFactory: SessionFactory,
  ): ListItemReader<ReferralPerformanceReport> {
    // var referralPerformanceReportRepositoryImpl: ReferralPerformanceReportRepository? = null

    // this reader returns referral entities which need processing for the report.
    return ListItemReader<ReferralPerformanceReport>(
      referralPerformanceReportRepository.serviceProviderReportReferrals(
        batchUtils.parseDateToOffsetDateTime(from),
        batchUtils.parseDateToOffsetDateTime(to),
        contractReferences.split(","),
      ),
    )
      /* .repository(referralPerformanceReportRepository)
      .methodName("serviceProviderReportReferrals")
      .arguments(
        listOf(
          batchUtils.parseDateToOffsetDateTime(from),
          batchUtils.parseDateToOffsetDateTime(to),
          contractReferences.split(","),
        ),
      )
      .pageSize(pageSize)
      .sorts(mapOf("dateReferralReceived" to Sort.Direction.ASC))
      .saveState(false)
      .build()*/
  }

  @Bean
  @JobScope
  fun writer(@Value("#{jobExecutionContext['output.file.path']}") path: String): FlatFileItemWriter<PerformanceReportData> {
    return batchUtils.csvFileWriter(
      "performanceReportWriter",
      FileSystemResource(path),
      PerformanceReportData.headers,
      PerformanceReportData.fields,
    )
  }

  @Bean
  fun performanceReportJob(writeToCsvStep: Step): Job {
    val validator = DefaultJobParametersValidator()
    validator.setRequiredKeys(
      arrayOf(
    /*
        "contractReferences",
        "user.id",
        "user.firstName",
        "user.email",
        "from",
        "to",
        "timestamp",
     */
      ),
    )

    return JobBuilder("performanceReportJob", jobRepository)
      .validator(validator)
      .listener(listener)
      .start(writeToCsvStep)
      .build()
  }

  @Bean
  fun writeToCsvStep(
    reader: ListItemReader<ReferralPerformanceReport>,
    processor: ItemProcessor<ReferralPerformanceReport, PerformanceReportData>,
    writer: FlatFileItemWriter<PerformanceReportData>,
  ): Step {
    return StepBuilder("writeToCsvStep", jobRepository)
      .chunk<ReferralPerformanceReport, PerformanceReportData>(chunkSize, transactionManager)
      .reader(reader)
      .processor(processor)
      .writer(writer)
      .build()
  }
}
