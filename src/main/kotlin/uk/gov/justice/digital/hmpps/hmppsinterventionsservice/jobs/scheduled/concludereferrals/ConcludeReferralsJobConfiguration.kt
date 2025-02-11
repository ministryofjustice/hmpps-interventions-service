package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jobs.scheduled.concludereferrals

import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.EnableTransactionManagement
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jobs.scheduled.OnStartupJobLauncherFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Referral
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.reporting.TimestampIncrementer

@Configuration
@EnableTransactionManagement
class ConcludeReferralsJobConfiguration(
  private val jobRepository: JobRepository,
  private val onStartupJobLauncherFactory: OnStartupJobLauncherFactory,
) {
  @Bean
  fun concludeReferralsJobLauncher(concludeReferralsJob: Job): ApplicationRunner = onStartupJobLauncherFactory.makeBatchLauncher(concludeReferralsJob)

  @Bean
  fun concludeReferralsJob(concludeReferralToInterventionStep: Step): Job = JobBuilder("concludeReferralsJob", jobRepository)
    .incrementer(TimestampIncrementer())
    .start(concludeReferralToInterventionStep)
    .build()

  @Bean
  fun concludeReferralToInterventionStep(
    reader: ConcludeReferralsReader,
    processor: ConcludeReferralsProcessor,
    writer: ConcludeReferralsWriter,
    transactionManager: PlatformTransactionManager,
  ): Step = StepBuilder("concludeReferralToInterventionStep", jobRepository)
    .chunk<Referral, Referral>(10, transactionManager)
    .reader(reader)
    .processor(processor)
    .writer(writer)
    .build()
}
