package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jobs.oneoff.markstaleappointments

import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory
import org.springframework.batch.core.job.DefaultJobParametersValidator
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ResourceLoader
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jobs.oneoff.OnStartupJobLauncherFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Appointment
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.reporting.TimestampIncrementer
import java.util.*

@Configuration
@EnableBatchProcessing
class MarkStaleAppointmentsJobConfiguration(
  private val jobBuilderFactory: JobBuilderFactory,
  private val stepBuilderFactory: StepBuilderFactory,
  private val onStartupJobLauncherFactory: OnStartupJobLauncherFactory,
  @Value("#{environment.SENTRY_ENVIRONMENT}")
  private var env: String?,
) {

  @Autowired
  private lateinit var resourceLoader: ResourceLoader

  private val properties = Properties()

  @Bean
  fun markStaleAppointmentsJobLauncher(markStaleAppointmentsJob: Job): ApplicationRunner {
    return onStartupJobLauncherFactory.makeBatchLauncher(markStaleAppointmentsJob)
  }

  @Bean
  fun markStaleAppointmentsJob(markStaleAppointmentsStep: Step): Job {
    if (env.isNullOrBlank()) this.env = "local"

    val propsPath = "classpath:jobs/oneoff/mark-stale-appointments-$env.txt"
    val resourceUri = resourceLoader.getResource(propsPath)
    val lines = resourceUri.file?.bufferedReader()?.readLines()

    val validator = DefaultJobParametersValidator()
    validator.setRequiredKeys(
      lines!!.toTypedArray(),
    )
    return jobBuilderFactory["markStaleAppointmentsJob"]
      .validator(validator)
      .incrementer(TimestampIncrementer())
      .start(markStaleAppointmentsStep)
      .build()
  }

  @Bean
  fun markStaleAppointmentsStep(
    reader: MarkStaleAppointmentsReader,
    processor: MarkStaleAppointmentsProcessor,
    writer: MarkStaleAppointmentsWriter,
  ): Step {
    return stepBuilderFactory.get("markStaleAppointmentsStep")
      .chunk<Appointment, Appointment>(10)
      .reader(reader)
      .processor(processor)
      .writer(writer)
      .build()
  }

  fun getProperty(key: String): Any? = properties.get(key)
}
