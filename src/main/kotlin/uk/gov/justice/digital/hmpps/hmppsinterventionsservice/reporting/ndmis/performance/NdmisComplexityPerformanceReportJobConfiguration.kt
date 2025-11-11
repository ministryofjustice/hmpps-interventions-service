
package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.reporting.ndmis.performance

import jakarta.persistence.EntityManagerFactory
import mu.KLogging
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
import org.springframework.batch.item.database.JdbcCursorItemReader
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder
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
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.reporting.BatchUtils
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.reporting.NPESkipPolicy
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.reporting.OutputPathIncrementer
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.reporting.QueryLoader
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.reporting.ReferralChunkProgressListener
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.reporting.TimestampIncrementer
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.S3Service
import java.nio.file.Path
import java.util.UUID
import javax.sql.DataSource

@Configuration
@EnableTransactionManagement
class NdmisComplexityPerformanceReportJobConfiguration(
  private val jobRepository: JobRepository,
  private val batchUtils: BatchUtils,
  private val s3Service: S3Service,
  private val ndmisS3Bucket: S3Bucket,
  private val onStartupJobLauncherFactory: OnStartupJobLauncherFactory,
  private val entityManagerFactory: EntityManagerFactory,
  private val dataSource: DataSource,
  private val queryLoader: QueryLoader,
  @Qualifier("transactionManager") private val transactionManager: PlatformTransactionManager,
  @Value("\${spring.batch.jobs.ndmis.performance-report.chunk-size}") private val chunkSize: Int,
) {
  companion object : KLogging()

  private val pathPrefix = "dfinterventions/dfi/csv/reports/"
  private val complexityReportFilename = "crs_performance_report-v2-complexity.csv"
  private val skipPolicy = NPESkipPolicy()

  @Bean
  fun ndmisComplexityPerformanceReportJobLauncher(ndmisComplexityPerformanceReportJob: Job): ApplicationRunner = onStartupJobLauncherFactory.makeBatchLauncher(ndmisComplexityPerformanceReportJob)

  @Bean("ndmisComplexityReader")
  @JobScope
  fun ndmisReader(): JdbcCursorItemReader<ComplexityData> = JdbcCursorItemReaderBuilder<ComplexityData>()
    .name("ndmisComplexityReportReader")
    .dataSource(dataSource)
    .sql(this.queryLoader.loadQuery("ndmis-complexity-report.sql"))
    .rowMapper { rs, _ ->
      ComplexityData(
        referralReference = rs.getString("referral_ref"),
        referralId = UUID.fromString(rs.getString("referral_id")),
        interventionTitle = rs.getString("intervention_title"),
        serviceCategoryId = UUID.fromString(rs.getString("service_category_id")),
        serviceCategoryName = rs.getString("service_category_name"),
        complexityLevelTitle = rs.getString("complexity_level_title") ?: "",
      )
    }
    .build()

  @Bean
  @StepScope
  fun ndmisComplexityWriter(@Value("#{jobParameters['outputPath']}") outputPath: String): FlatFileItemWriter<ComplexityData> = batchUtils.csvFileWriter(
    "ndmisComplexityPerformanceReportWriter",
    FileSystemResource(Path.of(outputPath).resolve(complexityReportFilename)),
    ComplexityData.headers,
    ComplexityData.fields,
  )

  @Bean(name = ["ndmisComplexityPerformanceReportJob"])
  fun ndmisComplexityPerformanceReportJob(
    ndmisWriteComplexityToCsvStep: Step,
    pushComplexityToS3Step: Step,
  ): Job {
    val validator = DefaultJobParametersValidator()
    validator.setRequiredKeys(arrayOf("timestamp", "outputPath"))

    return JobBuilder("ndmisComplexityPerformanceReportJob", jobRepository)
      .incrementer { parameters -> OutputPathIncrementer().getNext(TimestampIncrementer().getNext(parameters)) }
      .validator(validator)
      .start(ndmisWriteComplexityToCsvStep)
      .next(pushComplexityToS3Step)
      .build()
  }

  @Bean
  fun ndmisWriteComplexityToCsvStep(
    @Qualifier("ndmisComplexityReader") ndmisReader: JdbcCursorItemReader<ComplexityData>,
    writer: FlatFileItemWriter<ComplexityData>,
  ): Step = StepBuilder("ndmisWriteComplexityToCsvStep", jobRepository)
    .chunk<ComplexityData, ComplexityData>(chunkSize, transactionManager)
    .reader(ndmisReader)
    .writer(writer)
    .faultTolerant()
    .skipPolicy(skipPolicy)
    .transactionManager(transactionManager)
    .listener(ReferralChunkProgressListener(entityManagerFactory, "complexity"))
    .build()

  @JobScope
  @Bean
  fun pushComplexityToS3Step(@Value("#{jobParameters['outputPath']}") outputPath: String): Step = StepBuilder("pushComplexityToS3Step", jobRepository).tasklet(pushFilesToS3(outputPath), transactionManager).build()

  private fun pushFilesToS3(outputPath: String) = { _: StepContribution, _: ChunkContext ->
    val path = Path.of(outputPath).resolve(complexityReportFilename)
    s3Service.publishFileToS3(ndmisS3Bucket, path, pathPrefix, acl = ObjectCannedACL.BUCKET_OWNER_FULL_CONTROL)
    path.toFile().deleteOnExit()
    RepeatStatus.FINISHED
  }
}
