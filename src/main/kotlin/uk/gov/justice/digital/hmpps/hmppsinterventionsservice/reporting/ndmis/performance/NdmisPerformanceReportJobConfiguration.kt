package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.reporting.ndmis.performance

import org.hibernate.SessionFactory
import org.springframework.batch.core.ExitStatus
import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.StepExecution
import org.springframework.batch.core.StepExecutionListener
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.core.job.DefaultJobParametersValidator
import org.springframework.batch.core.step.job.DefaultJobParametersExtractor
import org.springframework.batch.item.database.HibernateCursorItemReader
import org.springframework.batch.item.database.builder.HibernateCursorItemReaderBuilder
import org.springframework.batch.item.file.FlatFileItemWriter
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.FileSystemResource
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Referral
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.reporting.BatchUtils


@Configuration
@EnableBatchProcessing
class NdmisPerformanceReportJobConfiguration(
  private val jobBuilderFactory: JobBuilderFactory,
  private val stepBuilderFactory: StepBuilderFactory,
  private val batchUtils: BatchUtils,
  private val listener: NdmisPerformanceReportJobListener,
  @Value("\${spring.batch.jobs.ndmis.performance-report.page-size}") private val pageSize: Int,
  @Value("\${spring.batch.jobs.ndmis.performance-report.chunk-size}") private val chunkSize: Int,
) {
  @Bean
  @JobScope
  fun ndmisReader(
    sessionFactory: SessionFactory,
  ): HibernateCursorItemReader<Referral> {
    // this reader returns referral entities which need processing for the report.
    return HibernateCursorItemReaderBuilder<Referral>()
      .name("ndmisPerformanceReportReader")
      .sessionFactory(sessionFactory)
      .queryString("select r from Referral r where sent_at is not null")
      .build()
  }

  @Bean
  @StepScope
  fun ndmisReferralsWriter(@Value("#{jobExecutionContext['output.file.path']}") path: String): FlatFileItemWriter<ReferralsData> {
    return batchUtils.csvFileWriter(
      "ndmisReferralsPerformanceReportWriter",
      FileSystemResource(path),
      ReferralsData.headers,
      ReferralsData.fields
    )
  }

  @Bean
  @StepScope
  fun ndmisComplexityWriter(@Value("#{jobExecutionContext['output.file.path']}") path: String): FlatFileItemWriter<Collection<ComplexityData>> {
    return batchUtils.recursiveCollectionCsvFileWriter(
      "ndmisComplexityPerformanceReportWriter",
      FileSystemResource(path),
      ComplexityData.headers,
      ComplexityData.fields
    )
  }

  @Bean
  @StepScope
  fun ndmisAppointmentWriter(@Value("#{jobExecutionContext['output.file.path']}") path: String): FlatFileItemWriter<Collection<AppointmentData>> {
    return batchUtils.recursiveCollectionCsvFileWriter(
      "ndmisAppointmentPerformanceReportWriter",
      FileSystemResource(path),
      AppointmentData.headers,
      AppointmentData.fields
    )
  }

  @Bean
  fun ndmisPerformanceReportJob(
    ndmisWriteReferralToCsvStep: Step,
    ndmisWriteComplexityToCsvStep: Step,
    ndmisWriteAppointmentToCsvStep: Step
  ): Job {
    val validator = DefaultJobParametersValidator()
    validator.setRequiredKeys(
      arrayOf(
        "timestamp",
      )
    )

    return jobBuilderFactory["ndmisPerformanceReportJob"]
      .validator(validator)
      .listener(listener)
      .start(ndmisWriteReferralToCsvStep)
      .next(ndmisWriteComplexityToCsvStep)
      .next(ndmisWriteAppointmentToCsvStep)
      .build()
  }

  @Bean
  fun ndmisWriteReferralToCsvStep(
    ndmisReader: HibernateCursorItemReader<Referral>,
    processor: ReferralsProcessor,
    writer: FlatFileItemWriter<ReferralsData>,
  ): Step {
    val builder = stepBuilderFactory.get("ndmisWriteReferralToCsvStep")
      .chunk<Referral, ReferralsData>(chunkSize)
      .reader(ndmisReader)
      .processor(processor)
      .writer(writer)
//    builder.listener(object : StepExecutionListener {
//      override fun beforeStep(stepExecution: StepExecution) {
//          stepExecution.jobExecution.executionContext.put("fileName", "crs_performance_report-v2-referral")
//      }
//      override fun afterStep(stepExecution: StepExecution): ExitStatus? {
//        return stepExecution.exitStatus
//      }
//    }
//    )
    builder.listener(listener)
    return builder.build()
  }

  @Bean
  fun ndmisWriteComplexityToCsvStep(
    ndmisReader: HibernateCursorItemReader<Referral>,
    processor: ComplexityProcessor,
    writer: FlatFileItemWriter<Collection<ComplexityData>>,
  ): Step {
    val builder = stepBuilderFactory.get("ndmisWriteComplexityToCsvStep")
      .chunk<Referral, List<ComplexityData>>(chunkSize)
      .reader(ndmisReader)
      .processor(processor)
      .writer(writer)
    builder.listener(listener)
    return builder.build()
  }

  @Bean
  fun ndmisWriteAppointmentToCsvStep(
    ndmisReader: HibernateCursorItemReader<Referral>,
    processor: AppointmentProcessor,
    writer: FlatFileItemWriter<Collection<AppointmentData>>,
  ): Step {
    val builder = stepBuilderFactory.get("ndmisWriteAppointmentToCsvStep")
      .chunk<Referral, List<AppointmentData>>(chunkSize)
      .reader(ndmisReader)
      .processor(processor)
      .writer(writer)
    builder.listener(listener)
    return builder.build()
  }
}
