package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jobs.oneoff.linksupersededappointments

import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jobs.oneoff.OnStartupJobLauncherFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Appointment
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.reporting.TimestampIncrementer

@Configuration
@EnableBatchProcessing
class LinkSupersededAppointmentsJobConfiguration(
  private val jobBuilderFactory: JobBuilderFactory,
  private val stepBuilderFactory: StepBuilderFactory,
  private val onStartupJobLauncherFactory: OnStartupJobLauncherFactory,
) {

  @Bean
  fun linkSupersededAppointmentsJobLauncher(linkSupersededAppointmentsJob: Job): ApplicationRunner {
    return onStartupJobLauncherFactory.makeBatchLauncher(linkSupersededAppointmentsJob)
  }

  @Bean
  fun linkSupersededAppointmentsJob(linkSupersededAppointmentsStep: Step): Job {
    return jobBuilderFactory["linkSupersededAppointmentsJob"]
      .incrementer(TimestampIncrementer())
      .start(linkSupersededAppointmentsStep)
      .build()
  }

  @Bean
  fun linkSupersededAppointmentsStep(
    reader: LinkSupersededAppointmentsReader,
    processor: LinkSupersededAppointmentsProcessor,
    writer: LinkSupersededAppointmentsWriter,
  ): Step {
    return stepBuilderFactory.get("linkSupersededAppointmentsStep")
      .chunk<Appointment, Appointment>(10)
      .reader(reader)
      .processor(processor)
      .writer(writer)
      .build()
  }
}
