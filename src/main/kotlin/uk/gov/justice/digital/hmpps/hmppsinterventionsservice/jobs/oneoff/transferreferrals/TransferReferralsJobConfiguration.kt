package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jobs.oneoff.transferreferrals

import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory
import org.springframework.batch.core.job.DefaultJobParametersValidator
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
@EnableBatchProcessing(dataSourceRef = "dataSource", transactionManagerRef = "transactionManager")
class TransferReferralsJobConfiguration(
  private val jobRepository: JobRepository,
  private val transactionManager: JpaTransactionManager,
  private val listener: TransferReferralsJobListener,
  private val onStartupJobLauncherFactory: OnStartupJobLauncherFactory,
  @Qualifier("batchJobBuilderFactory") private val jobBuilderFactory: JobBuilderFactory,
  @Qualifier("batchStepBuilderFactory") private val stepBuilderFactory: StepBuilderFactory,
) {
  @Bean
  fun transferReferralsJobLauncher(transferReferralsJob: Job): ApplicationRunner {
    return onStartupJobLauncherFactory.makeBatchLauncher(transferReferralsJob)
  }

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

    return jobBuilderFactory.get("transferReferralsJob")
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
  ): Step {
    return stepBuilderFactory.get("transferReferralToInterventionStep")
      .chunk<Referral, Referral>(10, transactionManager)
      .reader(reader)
      .processor(processor)
      .writer(writer)
      .build()
  }
}
