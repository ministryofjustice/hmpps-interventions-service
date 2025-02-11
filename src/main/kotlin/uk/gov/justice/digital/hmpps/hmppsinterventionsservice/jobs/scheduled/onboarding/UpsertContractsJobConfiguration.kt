package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jobs.scheduled.onboarding

import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.EnableTransactionManagement
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jobs.scheduled.OnStartupJobLauncherFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Intervention
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.reporting.TimestampIncrementer

@Configuration
@EnableTransactionManagement
class UpsertContractsJobConfiguration(
  private val onStartupJobLauncherFactory: OnStartupJobLauncherFactory,
  @Qualifier("jobRepository") private val jobRepository: JobRepository,
) {

  @Bean
  fun upsertContractsJobLauncher(upsertContractsJob: Job): ApplicationRunner = onStartupJobLauncherFactory.makeBatchLauncher(upsertContractsJob)

  @Bean
  fun upsertContractsJob(upsertProvidersStep: Step, upsertContractStep: Step, upsertContractDetailsStep: Step): Job = JobBuilder("upsertContractsJob", jobRepository)
    .incrementer(TimestampIncrementer())
    .start(upsertProvidersStep)
    .next(upsertContractDetailsStep)
    .next(upsertContractStep)
    .build()

  @Bean
  fun upsertProvidersStep(
    providerSetup: ProviderSetupTasklet,
    platformTransactionManager: PlatformTransactionManager,
  ): Step = StepBuilder("upsertProvidersStep", jobRepository)
    .tasklet(providerSetup, platformTransactionManager)
    .build()

  @Bean
  fun upsertContractDetailsStep(
    contractDetailsSetupTasklet: ContractDetailsSetupTasklet,
    platformTransactionManager: PlatformTransactionManager,
  ): Step = StepBuilder("upsertContractDetailsStep", jobRepository)
    .tasklet(contractDetailsSetupTasklet, platformTransactionManager)
    .build()

  @Bean
  fun upsertContractStep(
    reader: ContractDefinitionReader,
    processor: UpsertContractProcessor,
    platformTransactionManager: PlatformTransactionManager,
  ): Step = StepBuilder("upsertContractStep", jobRepository)
    .chunk<ContractDefinition, Intervention>(10)
    .reader(reader)
    .processor(processor)
    .writer {}
    .transactionManager(platformTransactionManager)
    .build()
}
