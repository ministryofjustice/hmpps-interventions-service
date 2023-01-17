package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jobs.oneoff.concludereferrals

import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory
import org.springframework.batch.core.job.DefaultJobParametersValidator
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jobs.oneoff.OnStartupJobLauncherFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Referral
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.reporting.TimestampIncrementer

@Configuration
@EnableBatchProcessing
class ConcludeReferralsJobConfiguration(
  private val jobBuilderFactory: JobBuilderFactory,
  private val stepBuilderFactory: StepBuilderFactory,
  private val onStartupJobLauncherFactory: OnStartupJobLauncherFactory,
) {

  @Bean
  fun concludeReferralsJobLauncher(concludeReferralsJob: Job): ApplicationRunner {
    return onStartupJobLauncherFactory.makeBatchLauncher(concludeReferralsJob)
  }

  @Bean
  fun concludeReferralsJob(concludeReferralToInterventionStep: Step): Job {
    return jobBuilderFactory["concludeReferralsJob"]
      .incrementer(TimestampIncrementer())
      .start(concludeReferralToInterventionStep)
      .build()
  }

  @Bean
  fun concludeReferralToInterventionStep(
    reader: ConcludeReferralsReader,
    processor: ConcludeReferralsProcessor,
    writer: ConcludeReferralsWriter,
  ): Step {
    return stepBuilderFactory.get("concludeReferralToInterventionStep")
      .chunk<Referral, Referral>(10)
      .reader(reader)
      .processor(processor)
      .writer(writer)
      .build()
  }
}
