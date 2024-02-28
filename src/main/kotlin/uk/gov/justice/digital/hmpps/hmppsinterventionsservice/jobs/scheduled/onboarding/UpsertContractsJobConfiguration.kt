package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jobs.scheduled.onboarding

import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jobs.oneoff.OnStartupJobLauncherFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Intervention
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.reporting.TimestampIncrementer

@Configuration
class UpsertContractsJobConfiguration(
  private val onStartupJobLauncherFactory: OnStartupJobLauncherFactory,
  private val batchJobRepository: JobRepository,
  private val transactionManager: PlatformTransactionManager,
) {

  @Bean
  fun upsertContractsJobLauncher(upsertContractsJob: Job): ApplicationRunner {
    return onStartupJobLauncherFactory.makeBatchLauncher(upsertContractsJob)
  }

  @Bean
  fun upsertContractsJob(upsertProvidersStep: Step, upsertContractStep: Step, upsertContractDetailsStep: Step): Job {
    return JobBuilder("upsertContractsJob", batchJobRepository)
      .incrementer(TimestampIncrementer())
      .start(upsertProvidersStep)
      .next(upsertContractDetailsStep)
      .next(upsertContractStep)
      .build()
  }

  @Bean
  fun upsertProvidersStep(
    providerSetup: ProviderSetupTasklet,
  ): Step {
    return StepBuilder("upsertProvidersStep", batchJobRepository)
      .tasklet(providerSetup, transactionManager)
      .build()
  }

  @Bean
  fun upsertContractDetailsStep(
    contractDetailsSetupTasklet: ContractDetailsSetupTasklet,
  ): Step {
    return StepBuilder("upsertContractDetailsStep", batchJobRepository)
      .tasklet(contractDetailsSetupTasklet, transactionManager)
      .build()
  }

  @Bean
  fun upsertContractStep(
    reader: ContractDefinitionReader,
    processor: UpsertContractProcessor,
  ): Step {
    return StepBuilder("upsertContractStep", batchJobRepository)
      .chunk<ContractDefinition, Intervention>(10)
      .reader(reader)
      .processor(processor)
      .writer {}
      .transactionManager(transactionManager)
      .build()
  }
}
