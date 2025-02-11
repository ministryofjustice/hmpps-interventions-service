package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.reporting.ndmis.performance

import jakarta.persistence.EntityManagerFactory
import mu.KLogging
import org.hibernate.SessionFactory
import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.StepContribution
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
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jobs.scheduled.OnStartupJobLauncherFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Referral
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.reporting.BatchUtils
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.reporting.NPESkipPolicy
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.reporting.OutputPathIncrementer
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.reporting.TimestampIncrementer
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.S3Service
import java.nio.file.Path

@Configuration
@EnableTransactionManagement
class NdmisReferralPerformanceReportJobConfiguration(
  private val jobRepository: JobRepository,
  private val batchUtils: BatchUtils,
  private val s3Service: S3Service,
  private val ndmisS3Bucket: S3Bucket,
  private val onStartupJobLauncherFactory: OnStartupJobLauncherFactory,
  private val entityManagerFactory: EntityManagerFactory,
  @Qualifier("transactionManager") private val transactionManager: PlatformTransactionManager,
  @Value("\${spring.batch.jobs.ndmis.performance-report.chunk-size}") private val chunkSize: Int,
) {
  companion object : KLogging()

  private val pathPrefix = "dfinterventions/dfi/csv/reports/"
  private val referralReportFilename = "crs_performance_report-v2-referrals.csv"
  private val skipPolicy = NPESkipPolicy()

  @Bean
  fun ndmisReferralPerformanceReportJobLauncher(ndmisReferralPerformanceReportJob: Job): ApplicationRunner = onStartupJobLauncherFactory.makeBatchLauncher(ndmisReferralPerformanceReportJob)

  @Bean("ndmisReferralReader")
  @JobScope
  fun ndmisReader(
    sessionFactory: SessionFactory,
  ): JpaCursorItemReader<Referral> {
    // this reader returns referral entities which need processing for the report.
    return JpaCursorItemReaderBuilder<Referral>()
      .name("ndmisReferralReportReader")
      .entityManagerFactory(entityManagerFactory)
      .queryString("select r from Referral r where sentAt is not null")
      .build()
  }

  @Bean
  @StepScope
  fun ndmisReferralsWriter(@Value("#{jobParameters['outputPath']}") outputPath: String): FlatFileItemWriter<ReferralsData> = batchUtils.csvFileWriter(
    "ndmisReferralsPerformanceReportWriter",
    FileSystemResource(Path.of(outputPath).resolve(referralReportFilename)),
    ReferralsData.headers,
    ReferralsData.fields,
  )

  @Bean(name = ["ndmisReferralPerformanceReportJob"])
  fun ndmisReferralPerformanceReportJob(
    ndmisWriteReferralToCsvStep: Step,
    pushReferralToS3Step: Step,
  ): Job {
    val validator = DefaultJobParametersValidator()
    validator.setRequiredKeys(arrayOf("timestamp", "outputPath"))

    return JobBuilder("ndmisReferralPerformanceReportJob", jobRepository)
      .incrementer { parameters -> OutputPathIncrementer().getNext(TimestampIncrementer().getNext(parameters)) }
      .validator(validator)
      .start(ndmisWriteReferralToCsvStep)
      .next(pushReferralToS3Step)
      .build()
  }

  @Bean
  fun ndmisWriteReferralToCsvStep(
    @Qualifier("ndmisReferralReader") ndmisReader: JpaCursorItemReader<Referral>,
    processor: ReferralsProcessor,
    writer: FlatFileItemWriter<ReferralsData>,
  ): Step = StepBuilder("ndmisWriteReferralToCsvStep", jobRepository)
    .chunk<Referral, ReferralsData>(chunkSize, transactionManager)
    .reader(ndmisReader)
    .processor(processor)
    .writer(writer)
    .faultTolerant()
    .skipPolicy(skipPolicy)
    .transactionManager(transactionManager)
    .build()

  @JobScope
  @Bean("pushReferralToS3Step")
  fun pushReferralToS3Step(@Value("#{jobParameters['outputPath']}") outputPath: String): Step = StepBuilder("pushReferralToS3Step", jobRepository).tasklet(pushFileToS3(outputPath), transactionManager).build()

  private fun pushFileToS3(outputPath: String) = { _: StepContribution, _: ChunkContext ->
    val path = Path.of(outputPath).resolve(referralReportFilename)
    s3Service.publishFileToS3(ndmisS3Bucket, path, pathPrefix, acl = ObjectCannedACL.BUCKET_OWNER_FULL_CONTROL)
    path.toFile().deleteOnExit()
    RepeatStatus.FINISHED
  }
}
