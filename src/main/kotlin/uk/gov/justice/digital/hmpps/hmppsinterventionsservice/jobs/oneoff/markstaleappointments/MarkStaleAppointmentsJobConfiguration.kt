package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jobs.oneoff.markstaleappointments

import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory
import org.springframework.batch.core.job.DefaultJobParametersValidator
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jobs.oneoff.OnStartupJobLauncherFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Appointment
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.reporting.TimestampIncrementer

@Configuration
@EnableBatchProcessing
class MarkStaleAppointmentsJobConfiguration(
  private val jobBuilderFactory: JobBuilderFactory,
  private val stepBuilderFactory: StepBuilderFactory,
  private val onStartupJobLauncherFactory: OnStartupJobLauncherFactory,
  @Value("\${spring.batch.jobs.mark-stale-appointments.appointments}")
  private val appointmentsStr: String,
) {
  @Bean
  fun markStaleAppointmentsJobLauncher(markStaleAppointmentsJob: Job): ApplicationRunner {
    return onStartupJobLauncherFactory.makeBatchLauncher(markStaleAppointmentsJob)
  }

  @Bean
  fun markStaleAppointmentsJob(markStaleAppointmentsStep: Step): Job {
    val validator = DefaultJobParametersValidator()
    validator.setRequiredKeys(
      arrayOf(
        "appointmentStr",
      ),
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
}
