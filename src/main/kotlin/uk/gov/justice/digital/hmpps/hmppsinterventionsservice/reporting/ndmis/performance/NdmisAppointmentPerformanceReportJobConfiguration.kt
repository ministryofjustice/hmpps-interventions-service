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
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Attended
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.reporting.BatchUtils
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.reporting.NPESkipPolicy
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.reporting.OutputPathIncrementer
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.reporting.QueryLoader
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.reporting.ReferralChunkProgressListener
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.reporting.TimestampIncrementer
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.S3Service
import java.nio.file.Path
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.util.UUID
import javax.sql.DataSource

data class ReferralMetadata(
  val referralId: UUID,
  val referralReference: String,
)

@Configuration
@EnableTransactionManagement
class NdmisAppointmentPerformanceReportJobConfiguration(
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
  private val appointmentReportFilename = "crs_performance_report-v2-appointments.csv"
  private val skipPolicy = NPESkipPolicy()

  @Bean
  fun ndmisAppointmentPerformanceReportJobLauncher(ndmisAppointmentPerformanceReportJob: Job): ApplicationRunner = onStartupJobLauncherFactory.makeBatchLauncher(ndmisAppointmentPerformanceReportJob)

  @Bean("ndmisAppointmentReader")
  @JobScope
  fun ndmisReader(): JdbcCursorItemReader<AppointmentData> = JdbcCursorItemReaderBuilder<AppointmentData>()
    .name("ndmisAppointmentReportReader")
    .dataSource(dataSource)
    .sql(this.queryLoader.loadQuery("ndmis-appointment-report.sql"))
    .rowMapper { rs, _ ->
      AppointmentData(
        referralReference = rs.getString("referral_ref"),
        referralId = UUID.fromString(rs.getString("referral_id")),
        appointmentId = UUID.fromString(rs.getString("appointmentId")),
        appointmentTime = NdmisDateTime(rs.getObject("appointment_time", OffsetDateTime::class.java)),
        durationInMinutes = rs.getInt("duration_in_minutes"),
        bookedAt = NdmisDateTime(rs.getObject("booked_at", OffsetDateTime::class.java)),
        attended = rs.getString("attended")?.let { Attended.valueOf(it) },
        attendanceSubmittedAt = rs.getObject("attendance_submitted_at", OffsetDateTime::class.java)?.let { NdmisDateTime(it) },
        notifyPPOfAttendanceBehaviour = rs.getObject("notifyppof_attendance_behaviour") as Boolean?,
        deliusAppointmentId = (rs.getObject("delius_appointment_id") as? Long)?.toString() ?: "",
        reasonForAppointment = AppointmentReason.valueOf(rs.getString("reason_for_appointment")),
      )
    }
    .build()

  @Bean
  @StepScope
  fun ndmisAppointmentWriter(@Value("#{jobParameters['outputPath']}") outputPath: String): FlatFileItemWriter<AppointmentData> = batchUtils.csvFileWriter(
    "ndmisAppointmentPerformanceReportWriter",
    FileSystemResource(Path.of(outputPath).resolve(appointmentReportFilename)),
    AppointmentData.headers,
    AppointmentData.fields,
  )

  @Bean(name = ["ndmisAppointmentPerformanceReportJob"])
  fun ndmisAppointmentPerformanceReportJob(
    ndmisWriteAppointmentToCsvStep: Step,
    pushAppointmentToS3Step: Step,
  ): Job {
    val validator = DefaultJobParametersValidator()
    validator.setRequiredKeys(arrayOf("timestamp", "outputPath"))

    return JobBuilder("ndmisAppointmentPerformanceReportJob", jobRepository)
      .incrementer { parameters -> OutputPathIncrementer().getNext(TimestampIncrementer().getNext(parameters)) }
      .validator(validator)
      .start(ndmisWriteAppointmentToCsvStep)
      .next(pushAppointmentToS3Step)
      .build()
  }

  @Bean
  fun ndmisWriteAppointmentToCsvStep(
    @Qualifier("ndmisAppointmentReader") ndmisReader: JdbcCursorItemReader<AppointmentData>,
    writer: FlatFileItemWriter<AppointmentData>,
  ): Step = StepBuilder("ndmisWriteAppointmentToCsvStep", jobRepository)
    .chunk<AppointmentData, AppointmentData>(chunkSize, transactionManager)
    .reader(ndmisReader)
    .writer(writer)
    .faultTolerant()
    .skipPolicy(skipPolicy)
    .transactionManager(transactionManager)
    .listener(ReferralChunkProgressListener(entityManagerFactory, "appointment"))
    .build()

  @JobScope
  @Bean("pushAppointmentToS3Step")
  fun pushAppointmentToS3Step(@Value("#{jobParameters['outputPath']}") outputPath: String): Step = StepBuilder("pushAppointmentToS3Step", jobRepository).tasklet(pushFileToS3(outputPath), transactionManager).build()

  private fun pushFileToS3(outputPath: String) = { _: StepContribution, _: ChunkContext ->
    val path = Path.of(outputPath).resolve(appointmentReportFilename)
    s3Service.publishFileToS3(ndmisS3Bucket, path, pathPrefix, acl = ObjectCannedACL.BUCKET_OWNER_FULL_CONTROL)
    path.toFile().deleteOnExit()
    RepeatStatus.FINISHED
  }

  private fun instantToOffsetNotNull(instant: Instant?): OffsetDateTime {
    val resolved = instant ?: Instant.now()
    return OffsetDateTime.ofInstant(resolved, ZoneId.systemDefault())
  }

  private fun instantToOffsetNull(instant: Instant?): OffsetDateTime? {
    val resolved = instant ?: Instant.now()
    return instant ?.let { OffsetDateTime.ofInstant(resolved, ZoneId.systemDefault()) }
  }
}
