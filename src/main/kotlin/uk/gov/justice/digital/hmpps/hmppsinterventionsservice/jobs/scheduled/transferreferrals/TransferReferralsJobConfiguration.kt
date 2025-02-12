package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jobs.scheduled.transferreferrals

import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.job.DefaultJobParametersValidator
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
class TransferReferralsJobConfiguration(
  private val jobRepository: JobRepository,
  private val listener: TransferReferralsJobListener,
  private val onStartupJobLauncherFactory: OnStartupJobLauncherFactory,
) {
  @Bean
  fun transferReferralsJobLauncher(transferReferralsJob: Job): ApplicationRunner = onStartupJobLauncherFactory.makeBatchLauncher(transferReferralsJob)

  @Bean
  fun transferReferralsJob(transferReferralToInterventionStep: Step): Job {
    val validator = DefaultJobParametersValidator()
    validator.setRequiredKeys(
      arrayOf(
        "fromContract",
        "toContract",
        "timestamp",
      ),
    )

    return JobBuilder("transferReferralsJob", jobRepository)
      .incrementer(TimestampIncrementer())
      .validator(validator)
      .listener(listener)
      .start(transferReferralToInterventionStep)
      .build()
  }

  @Bean
  fun transferReferralToInterventionStep(
    reader: TransferReferralsReader,
    processor: TransferReferralsProcessor,
    writer: TransferReferralsWriter,
    transactionManager: PlatformTransactionManager,
  ): Step = StepBuilder("transferReferralToInterventionStep", jobRepository)
    .chunk<Referral, Referral>(10, transactionManager)
    .reader(reader)
    .processor(processor)
    .writer(writer)
    .build()
}
