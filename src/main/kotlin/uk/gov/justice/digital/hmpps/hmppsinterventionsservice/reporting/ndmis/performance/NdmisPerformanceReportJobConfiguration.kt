package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.reporting.ndmis.performance

import mu.KLogging
import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.core.job.DefaultJobParametersValidator
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.batch.item.database.JpaCursorItemReader
import org.springframework.batch.item.database.builder.JpaCursorItemReaderBuilder
import org.springframework.batch.item.file.FlatFileItemWriter
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.FileSystemResource
import org.springframework.transaction.PlatformTransactionManager
import software.amazon.awssdk.services.s3.model.ObjectCannedACL
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.config.S3Bucket
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jobs.oneoff.OnStartupJobLauncherFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Referral
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.reporting.BatchUtils
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.reporting.NPESkipPolicy
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.reporting.OutputPathIncrementer
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.reporting.TimestampIncrementer
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.S3Service
import java.nio.file.Path

@Configuration
@EnableBatchProcessing
class NdmisPerformanceReportJobConfiguration(
  private val transactionManager: PlatformTransactionManager,
  private val jobRepository: JobRepository,
  private val batchUtils: BatchUtils,
  private val s3Service: S3Service,
  private val ndmisS3Bucket: S3Bucket,
  private val onStartupJobLauncherFactory: OnStartupJobLauncherFactory,
  @Value("\${spring.batch.jobs.ndmis.performance-report.page-size}") private val pageSize: Int,
  @Value("\${spring.batch.jobs.ndmis.performance-report.chunk-size}") private val chunkSize: Int,
) {
  companion object : KLogging()

  private val pathPrefix = "dfinterventions/dfi/csv/reports/"
  private val referralReportFilename = "crs_performance_report-v2-referrals.csv"
  private val complexityReportFilename = "crs_performance_report-v2-complexity.csv"
  private val appointmentReportFilename = "crs_performance_report-v2-appointments.csv"
  private val outcomeReportFilename = "crs_performance_report-v2-outcomes.csv"
  private val skipPolicy = NPESkipPolicy()

  @Bean
  fun ndmisPerformanceReportJobLauncher(ndmisPerformanceReportJob: Job): ApplicationRunner {
    return onStartupJobLauncherFactory.makeBatchLauncher(ndmisPerformanceReportJob)
  }

  @Bean
  @JobScope
  fun ndmisReader(): JpaCursorItemReader<Referral> {
    return JpaCursorItemReaderBuilder<Referral>()
      .name("ndmisPerformanceReportReader")
      .queryString("select r from Referral r where sent_at is not null")
      // do we need .maxItemCount(pageSize)?
      .build()
  }

  @Bean
  @StepScope
  fun ndmisReferralsWriter(@Value("#{jobParameters['outputPath']}") outputPath: String): FlatFileItemWriter<ReferralsData> {
    return batchUtils.csvFileWriter(
      "ndmisReferralsPerformanceReportWriter",
      FileSystemResource(Path.of(outputPath).resolve(referralReportFilename)),
      ReferralsData.headers,
      ReferralsData.fields,
    )
  }

  @Bean
  @StepScope
  fun ndmisComplexityWriter(@Value("#{jobParameters['outputPath']}") outputPath: String): FlatFileItemWriter<Collection<ComplexityData>> {
    return batchUtils.recursiveCollectionCsvFileWriter(
      "ndmisComplexityPerformanceReportWriter",
      FileSystemResource(Path.of(outputPath).resolve(complexityReportFilename)),
      ComplexityData.headers,
      ComplexityData.fields,
    )
  }

  @Bean
  @StepScope
  fun ndmisAppointmentWriter(@Value("#{jobParameters['outputPath']}") outputPath: String): FlatFileItemWriter<Collection<AppointmentData>> {
    return batchUtils.recursiveCollectionCsvFileWriter(
      "ndmisAppointmentPerformanceReportWriter",
      FileSystemResource(Path.of(outputPath).resolve(appointmentReportFilename)),
      AppointmentData.headers,
      AppointmentData.fields,
    )
  }

  @Bean
  @StepScope
  fun ndmisOutcomeWriter(@Value("#{jobParameters['outputPath']}") outputPath: String): FlatFileItemWriter<Collection<OutcomeData>> {
    return batchUtils.recursiveCollectionCsvFileWriter(
      "ndmisOutcomePerformanceReportWriter",
      FileSystemResource(Path.of(outputPath).resolve(outcomeReportFilename)),
      OutcomeData.headers,
      OutcomeData.fields,
    )
  }

  @Bean
  fun ndmisPerformanceReportJob(
    ndmisWriteReferralToCsvStep: Step,
    ndmisWriteComplexityToCsvStep: Step,
    ndmisWriteAppointmentToCsvStep: Step,
    ndmisWriteOutcomeToCsvStep: Step,
    pushToS3Step: Step,
  ): Job {
    val validator = DefaultJobParametersValidator()
    validator.setRequiredKeys(arrayOf("timestamp", "outputPath"))

    return JobBuilder("ndmisPerformanceReportJob", jobRepository)
      .incrementer { parameters -> OutputPathIncrementer().getNext(TimestampIncrementer().getNext(parameters)) }
      .validator(validator)
      .start(ndmisWriteReferralToCsvStep)
      .next(ndmisWriteComplexityToCsvStep)
      .next(ndmisWriteAppointmentToCsvStep)
      .next(ndmisWriteOutcomeToCsvStep)
      .next(pushToS3Step)
      .build()
  }

  @Bean
  fun ndmisWriteReferralToCsvStep(
    ndmisReader: JpaCursorItemReader<Referral>,
    processor: ReferralsProcessor,
    writer: FlatFileItemWriter<ReferralsData>,
    transactionManager: PlatformTransactionManager,
  ): Step {
    return StepBuilder("ndmisWriteReferralToCsvStep", jobRepository)
      .chunk<Referral, ReferralsData>(chunkSize, transactionManager)
      .reader(ndmisReader)
      .processor(processor)
      .writer(writer)
      .faultTolerant()
      .skipPolicy(skipPolicy)
      .build()
  }

  @Bean
  fun ndmisWriteComplexityToCsvStep(
    ndmisReader: JpaCursorItemReader<Referral>,
    processor: ComplexityProcessor,
    writer: FlatFileItemWriter<Collection<ComplexityData>>,
  ): Step {
    return StepBuilder("ndmisWriteComplexityToCsvStep", jobRepository)
      .chunk<Referral, List<ComplexityData>>(chunkSize, transactionManager)
      .reader(ndmisReader)
      .processor(processor)
      .writer(writer)
      .faultTolerant()
      .skipPolicy(skipPolicy)
      .transactionManager(transactionManager)
      .build()
  }

  @Bean
  fun ndmisWriteAppointmentToCsvStep(
    ndmisReader: JpaCursorItemReader<Referral>,
    processor: AppointmentProcessor,
    writer: FlatFileItemWriter<Collection<AppointmentData>>,
  ): Step {
    return StepBuilder("ndmisWriteAppointmentToCsvStep", jobRepository)
      .chunk<Referral, List<AppointmentData>>(chunkSize, transactionManager)
      .reader(ndmisReader)
      .processor(processor)
      .writer(writer)
      .faultTolerant()
      .skipPolicy(skipPolicy)
      .transactionManager(transactionManager)
      .build()
  }

  @Bean
  fun ndmisWriteOutcomeToCsvStep(
    ndmisReader: JpaCursorItemReader<Referral>,
    processor: OutcomeProcessor,
    writer: FlatFileItemWriter<Collection<OutcomeData>>,
  ): Step {
    return StepBuilder("ndmisWriteOutcomeToCsvStep", jobRepository)
      .chunk<Referral, List<OutcomeData>>(chunkSize)
      .reader(ndmisReader)
      .processor(processor)
      .writer(writer)
      .faultTolerant()
      .skipPolicy(skipPolicy)
      .transactionManager(transactionManager)
      .build()
  }

  @Bean
  @JobScope
  fun pushToS3Step(@Value("#{jobParameters['outputPath']}") outputPath: String): Step =
    StepBuilder("pushToS3Step", jobRepository).tasklet(pushFilesToS3(outputPath), transactionManager).build()

  private fun pushFilesToS3(outputPath: String) = { _: StepContribution, _: ChunkContext ->
    listOf(referralReportFilename, complexityReportFilename, appointmentReportFilename, outcomeReportFilename).forEach { file ->
      val path = Path.of(outputPath).resolve(file)
      s3Service.publishFileToS3(ndmisS3Bucket, path, pathPrefix, acl = ObjectCannedACL.BUCKET_OWNER_FULL_CONTROL)
      path.toFile().deleteOnExit()
    }
    RepeatStatus.FINISHED
  }
}
