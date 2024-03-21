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
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.EnableTransactionManagement
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
@EnableTransactionManagement
class NdmisOutcomePerformanceReportJobConfiguration(
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
  private val outcomeReportFilename = "crs_performance_report-v2-outcomes.csv"
  private val skipPolicy = NPESkipPolicy()

  @Bean
  fun ndmisPerformanceReportJobLauncher(ndmisOutcomePerformanceReportJob: Job): ApplicationRunner {
    return onStartupJobLauncherFactory.makeBatchLauncher(ndmisOutcomePerformanceReportJob)
  }

  @Bean("ndmisOutcomeReader")
  @JobScope
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
  fun ndmisOutcomeWriter(@Value("#{jobParameters['outputPath']}") outputPath: String): FlatFileItemWriter<Collection<OutcomeData>> {
    return batchUtils.recursiveCollectionCsvFileWriter(
      "ndmisOutcomePerformanceReportWriter",
      FileSystemResource(Path.of(outputPath).resolve(outcomeReportFilename)),
      OutcomeData.headers,
      OutcomeData.fields,
    )
  }

  @Bean(name = ["ndmisOutcomePerformanceReportJob"])
  fun ndmisOutcomePerformanceReportJob(
    ndmisWriteOutcomeToCsvStep: Step,
    pushOutcomeToS3Step: Step,
  ): Job {
    val validator = DefaultJobParametersValidator()
    validator.setRequiredKeys(arrayOf("timestamp", "outputPath"))

    return jobBuilderFactory["ndmisOutcomePerformanceReportJob"]
      .incrementer { parameters -> OutputPathIncrementer().getNext(TimestampIncrementer().getNext(parameters)) }
      .validator(validator)
      .start(ndmisWriteOutcomeToCsvStep)
      .next(pushOutcomeToS3Step)
      .build()
  }

  @Bean
  fun ndmisWriteOutcomeToCsvStep(
    @Qualifier("ndmisOutcomeReader") ndmisReader: HibernateCursorItemReader<Referral>,
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
  fun pushOutcomeToS3Step(@Value("#{jobParameters['outputPath']}") outputPath: String): Step =
    stepBuilderFactory["pushOutcomeToS3Step"].tasklet(pushFilesToS3(outputPath), transactionManager).build()

  private fun pushFilesToS3(outputPath: String) = { _: StepContribution, _: ChunkContext ->
    val path = Path.of(outputPath).resolve(outcomeReportFilename)
    s3Service.publishFileToS3(ndmisS3Bucket, path, pathPrefix, acl = ObjectCannedACL.BUCKET_OWNER_FULL_CONTROL)
    path.toFile().deleteOnExit()
    RepeatStatus.FINISHED
  }
}
