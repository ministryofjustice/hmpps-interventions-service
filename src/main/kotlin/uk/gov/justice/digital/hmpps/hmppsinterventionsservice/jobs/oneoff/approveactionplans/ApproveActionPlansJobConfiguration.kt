package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jobs.oneoff.approveactionplans

import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory
import org.springframework.batch.core.job.DefaultJobParametersValidator
import org.springframework.batch.core.repository.JobRepository
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.EnableTransactionManagement
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jobs.oneoff.OnStartupJobLauncherFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jobs.oneoff.transferreferrals.ApproveActionPlansJobListener
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jobs.oneoff.transferreferrals.ApproveActionPlansReader
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ActionPlan
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Referral
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.reporting.TimestampIncrementer

@Configuration
@EnableTransactionManagement
@Transactional
class ApproveActionPlansJobConfiguration(
  private val jobRepository: JobRepository,
  private val listener: ApproveActionPlansJobListener,
  private val onStartupJobLauncherFactory: OnStartupJobLauncherFactory,
  @Qualifier("batchJobBuilderFactory") private val jobBuilderFactory: JobBuilderFactory,
  @Qualifier("batchStepBuilderFactory") private val stepBuilderFactory: StepBuilderFactory,
) {
  @Bean
  fun approveActionPlansJobLauncher(approveActionPlansJob: Job): ApplicationRunner {
    return onStartupJobLauncherFactory.makeBatchLauncher(approveActionPlansJob)
  }

  @Bean
  fun approveActionPlansJob(approveActionPlanStep: Step): Job {
    val validator = DefaultJobParametersValidator()
    validator.setRequiredKeys(
      arrayOf(
        "timestamp",
      ),
    )

    return jobBuilderFactory.get("approveActionPlansJob")
      .incrementer(TimestampIncrementer())
      .validator(validator)
      .listener(listener)
      .start(approveActionPlanStep)
      .build()
  }

  @Bean
  fun approveActionPlanStep(
    reader: ApproveActionPlansReader,
    processor: ApproveActionPlansProcessor,
    writer: ApproveActionPlansWriter,
    transactionManager: PlatformTransactionManager,
  ): Step {
    return stepBuilderFactory.get("approveActionPlanStep")
      .chunk<ActionPlan, ActionPlan>(10, transactionManager)
      .reader(reader)
      .processor(processor)
      .writer(writer)
      .build()
  }
}
