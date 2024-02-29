package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jobs.oneoff.concludereferrals

import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory
import org.springframework.batch.core.repository.JobRepository
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.orm.jpa.JpaTransactionManager
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jobs.oneoff.OnStartupJobLauncherFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Referral
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.reporting.TimestampIncrementer

@Configuration
class ConcludeReferralsJobConfiguration(
  private val jobRepository: JobRepository,
  private val transactionManager: JpaTransactionManager,
  private val onStartupJobLauncherFactory: OnStartupJobLauncherFactory,
  @Qualifier("batchJobBuilderFactory") private val jobBuilderFactory: JobBuilderFactory,
  @Qualifier("batchStepBuilderFactory") private val stepBuilderFactory: StepBuilderFactory,
) {
  @Bean
  fun concludeReferralsJobLauncher(concludeReferralsJob: Job): ApplicationRunner {
    return onStartupJobLauncherFactory.makeBatchLauncher(concludeReferralsJob)
  }

  @Bean
  fun concludeReferralsJob(concludeReferralToInterventionStep: Step): Job {
    return jobBuilderFactory.get("concludeReferralsJob")
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
      .chunk<Referral, Referral>(10, transactionManager)
      .reader(reader)
      .processor(processor)
      .writer(writer)
      .build()
  }
}
