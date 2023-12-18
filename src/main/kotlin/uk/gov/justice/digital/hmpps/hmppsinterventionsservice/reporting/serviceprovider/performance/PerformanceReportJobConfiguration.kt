package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.reporting.serviceprovider.performance

import org.hibernate.SessionFactory
import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory
import org.springframework.batch.core.job.DefaultJobParametersValidator
import org.springframework.batch.item.ItemProcessor
import org.springframework.batch.item.data.RepositoryItemReader
import org.springframework.batch.item.data.builder.RepositoryItemReaderBuilder
import org.springframework.batch.item.file.FlatFileItemWriter
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.FileSystemResource
import org.springframework.data.domain.Sort
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ReferralRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.reporting.BatchUtils
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.reporting.serviceprovider.performance.model.PerformanceReportReferral
import java.util.Date

@Configuration
@EnableBatchProcessing
class PerformanceReportJobConfiguration(
  @Qualifier("batchJobBuilderFactory") private val jobBuilderFactory: JobBuilderFactory,
  @Qualifier("batchStepBuilderFactory") private val stepBuilderFactory: StepBuilderFactory,
  private val batchUtils: BatchUtils,
  private val listener: PerformanceReportJobListener,
  private val referralRepository: ReferralRepository,
  @Value("\${spring.batch.jobs.service-provider.performance-report.chunk-size}") private val chunkSize: Int,
  @Value("\${spring.batch.jobs.service-provider.performance-report.page-size}") private val pageSize: Int,
) {
  @Bean
  @JobScope
  fun reader(
    @Value("#{jobParameters['contractReferences']}") contractReferences: String,
    @Value("#{jobParameters['from']}") from: Date,
    @Value("#{jobParameters['to']}") to: Date,
    sessionFactory: SessionFactory,
  ): RepositoryItemReader<PerformanceReportReferral> {
    // this reader returns referral entities which need processing for the report.
    return RepositoryItemReaderBuilder<PerformanceReportReferral>()
      .repository(referralRepository)
      .methodName("findPerformanceReportReferral")
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
      .build()
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
        "contractReferences",
        "user.id",
        "user.firstName",
        "user.email",
        "from",
        "to",
        "timestamp",
      ),
    )

    return jobBuilderFactory["performanceReportJob"]
      .validator(validator)
      .listener(listener)
      .start(writeToCsvStep)
      .build()
  }

  @Bean
  fun writeToCsvStep(
    reader: RepositoryItemReader<PerformanceReportReferral>,
    processor: ItemProcessor<PerformanceReportReferral, PerformanceReportData>,
    writer: FlatFileItemWriter<PerformanceReportData>,
  ): Step {
    return stepBuilderFactory.get("writeToCsvStep")
      .chunk<PerformanceReportReferral, PerformanceReportData>(chunkSize)
      .reader(reader)
      .processor(processor)
      .writer(writer)
      .build()
  }
}
