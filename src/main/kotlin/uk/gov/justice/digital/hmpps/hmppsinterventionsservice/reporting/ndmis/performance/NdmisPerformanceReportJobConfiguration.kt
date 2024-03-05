package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.reporting.ndmis.performance

import mu.KLogging
import org.hibernate.SessionFactory
import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.core.job.DefaultJobParametersValidator
import org.springframework.batch.core.job.builder.FlowBuilder
import org.springframework.batch.core.job.flow.support.SimpleFlow
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.item.database.HibernateCursorItemReader
import org.springframework.batch.item.database.builder.HibernateCursorItemReaderBuilder
import org.springframework.batch.item.file.FlatFileItemWriter
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.FileSystemResource
import org.springframework.core.task.SimpleAsyncTaskExecutor
import org.springframework.core.task.TaskExecutor
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
class NdmisPerformanceReportJobConfiguration(
  @Qualifier("batchJobBuilderFactory") private val jobBuilderFactory: JobBuilderFactory,
  @Qualifier("batchStepBuilderFactory") private val stepBuilderFactory: StepBuilderFactory,
  private val batchUtils: BatchUtils,
  private val s3Service: S3Service,
  private val ndmisS3Bucket: S3Bucket,
  private val onStartupJobLauncherFactory: OnStartupJobLauncherFactory,
  @Qualifier("transactionManager") private val transactionManager: PlatformTransactionManager,
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
  fun taskExecutor(): TaskExecutor? {
    return SimpleAsyncTaskExecutor("spring_batch_ndmis_report")
  }

  @Bean
  @StepScope
  fun ndmisReader(
    sessionFactory: SessionFactory,
  ): HibernateCursorItemReader<Referral> {
    // this reader returns referral entities which need processing for the report.
    return HibernateCursorItemReaderBuilder<Referral>()
      .name("ndmisPerformanceReportReader")
      .sessionFactory(sessionFactory)
      .queryString("select r from Referral r where sentAt is not null")
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

  @Bean(name = ["ndmisPerformanceReportJob"])
  fun ndmisPerformanceReportJob(
    writeReferralFlow: SimpleFlow,
    writeComplexityFlow: SimpleFlow,
    writeAppointmentFlow: SimpleFlow,
    writeOutcomeFlow: SimpleFlow,
    pushToS3Step: Step,
  ): Job {
    val validator = DefaultJobParametersValidator()
    validator.setRequiredKeys(arrayOf("timestamp", "outputPath"))

    return jobBuilderFactory["ndmisPerformanceReportJob"]
      .incrementer { parameters -> OutputPathIncrementer().getNext(TimestampIncrementer().getNext(parameters)) }
      .validator(validator)
      .start(
        mainFlow(
          writeReferralFlow,
          writeComplexityFlow,
          writeAppointmentFlow,
          writeOutcomeFlow,
        ),
      )
      .next(pushToS3Step)
      .build()
      .build()
  }

  @Bean
  fun writeReferralFlow(
    ndmisReader: HibernateCursorItemReader<Referral>,
    referralsProcessor: ReferralsProcessor,
    referralWriter: FlatFileItemWriter<ReferralsData>,
  ): SimpleFlow? {
    return FlowBuilder<SimpleFlow>("writeReferralFlow")
      .start(ndmisWriteReferralToCsvStep(ndmisReader, referralsProcessor, referralWriter))
      .build()
  }

  @Bean
  fun writeComplexityFlow(
    ndmisReader: HibernateCursorItemReader<Referral>,
    complexityProcessor: ComplexityProcessor,
    ndmisComplexityWriter: FlatFileItemWriter<Collection<ComplexityData>>,
  ): SimpleFlow? {
    return FlowBuilder<SimpleFlow>("writeComplexityFlow")
      .start(ndmisWriteComplexityToCsvStep(ndmisReader, complexityProcessor, ndmisComplexityWriter))
      .build()
  }

  @Bean
  fun writeAppointmentFlow(
    ndmisReader: HibernateCursorItemReader<Referral>,
    appointmentProcessor: AppointmentProcessor,
    ndmisAppointmentWriter: FlatFileItemWriter<Collection<AppointmentData>>,
  ): SimpleFlow? {
    return FlowBuilder<SimpleFlow>("writeAppointmentFlow")
      .start(ndmisWriteAppointmentToCsvStep(ndmisReader, appointmentProcessor, ndmisAppointmentWriter))
      .build()
  }

  @Bean
  fun writeOutcomeFlow(
    ndmisReader: HibernateCursorItemReader<Referral>,
    outcomeProcessor: OutcomeProcessor,
    ndmisOutcomeWriter: FlatFileItemWriter<Collection<OutcomeData>>,
  ): SimpleFlow? {
    return FlowBuilder<SimpleFlow>("writeOutcomeFlow")
      .start(ndmisWriteOutcomeToCsvStep(ndmisReader, outcomeProcessor, ndmisOutcomeWriter))
      .build()
  }
  private fun mainFlow(
    writeReferralFlow: SimpleFlow,
    writeComplexityFlow: SimpleFlow,
    writeAppointmentFlow: SimpleFlow,
    writeOutcomeFlow: SimpleFlow,
  ): SimpleFlow? {
    return FlowBuilder<SimpleFlow>("mainFlow")
      .split(taskExecutor())
      .add(writeReferralFlow, writeComplexityFlow, writeAppointmentFlow, writeOutcomeFlow)
      .build()
  }

  @Bean
  fun ndmisWriteReferralToCsvStep(
    ndmisReader: HibernateCursorItemReader<Referral>,
    processor: ReferralsProcessor,
    writer: FlatFileItemWriter<ReferralsData>,
  ): Step {
    return stepBuilderFactory.get("ndmisWriteReferralToCsvStep")
      .chunk<Referral, ReferralsData>(chunkSize, transactionManager)
      .reader(ndmisReader)
      .processor(processor)
      .writer(writer)
      .faultTolerant()
      .skipPolicy(skipPolicy)
      .transactionManager(transactionManager)
      .build()
  }

  @Bean
  fun ndmisWriteComplexityToCsvStep(
    ndmisReader: HibernateCursorItemReader<Referral>,
    processor: ComplexityProcessor,
    writer: FlatFileItemWriter<Collection<ComplexityData>>,
  ): Step {
    return stepBuilderFactory.get("ndmisWriteComplexityToCsvStep")
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
    ndmisReader: HibernateCursorItemReader<Referral>,
    processor: AppointmentProcessor,
    writer: FlatFileItemWriter<Collection<AppointmentData>>,
  ): Step {
    return stepBuilderFactory.get("ndmisWriteAppointmentToCsvStep")
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
    ndmisReader: HibernateCursorItemReader<Referral>,
    processor: OutcomeProcessor,
    writer: FlatFileItemWriter<Collection<OutcomeData>>,
  ): Step {
    return stepBuilderFactory.get("ndmisWriteOutcomeToCsvStep")
      .chunk<Referral, List<OutcomeData>>(chunkSize, transactionManager)
      .reader(ndmisReader)
      .processor(processor)
      .writer(writer)
      .faultTolerant()
      .skipPolicy(skipPolicy)
      .transactionManager(transactionManager)
      .build()
  }

  @JobScope
  @Bean
  fun pushToS3Step(@Value("#{jobParameters['outputPath']}") outputPath: String): Step =
    stepBuilderFactory["pushToS3Step"].tasklet(pushFilesToS3(outputPath), transactionManager).build()

  private fun pushFilesToS3(outputPath: String) = { _: StepContribution, _: ChunkContext ->
    listOf(referralReportFilename, complexityReportFilename, appointmentReportFilename, outcomeReportFilename).forEach { file ->
      val path = Path.of(outputPath).resolve(file)
      s3Service.publishFileToS3(ndmisS3Bucket, path, pathPrefix, acl = ObjectCannedACL.BUCKET_OWNER_FULL_CONTROL)
      path.toFile().deleteOnExit()
    }
    RepeatStatus.FINISHED
  }
}
