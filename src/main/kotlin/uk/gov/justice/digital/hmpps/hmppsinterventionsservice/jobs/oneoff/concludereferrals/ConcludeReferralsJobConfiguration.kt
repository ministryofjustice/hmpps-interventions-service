package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jobs.oneoff.concludereferrals

import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.orm.jpa.JpaTransactionManager
import org.springframework.transaction.annotation.EnableTransactionManagement
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jobs.oneoff.OnStartupJobLauncherFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Referral
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.reporting.TimestampIncrementer

@Configuration
@EnableBatchProcessing
@EnableTransactionManagement
class ConcludeReferralsJobConfiguration(
  private val jobRepository: JobRepository,
  private val transactionManager: JpaTransactionManager,
  private val onStartupJobLauncherFactory: OnStartupJobLauncherFactory,
) {
  @Bean
  fun concludeReferralsJobLauncher(concludeReferralsJob: Job): ApplicationRunner {
    return onStartupJobLauncherFactory.makeBatchLauncher(concludeReferralsJob)
  }

  @Bean
  fun concludeReferralsJob(concludeReferralToInterventionStep: Step): Job {
    return JobBuilder("concludeReferralsJob", jobRepository)
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
    return StepBuilder("concludeReferralToInterventionStep", jobRepository)
      .chunk<Referral, Referral>(10, transactionManager)
      .reader(reader)
      .processor(processor)
      .writer(writer)
      .build()
  }
}
