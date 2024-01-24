package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jobs.scheduled.onboarding

import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jobs.oneoff.OnStartupJobLauncherFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Intervention
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.reporting.TimestampIncrementer

@Configuration
@EnableBatchProcessing
class UpsertContractsJobConfiguration(
  private val jobBuilderFactory: JobBuilderFactory,
  private val stepBuilderFactory: StepBuilderFactory,
  private val onStartupJobLauncherFactory: OnStartupJobLauncherFactory,
) {

  @Bean
  fun upsertContractsJobLauncher(upsertContractsJob: Job): ApplicationRunner {
    return onStartupJobLauncherFactory.makeBatchLauncher(upsertContractsJob)
  }

  @Bean
  fun upsertContractsJob(upsertProvidersStep: Step, upsertContractStep: Step, upsertContractDetailsStep: Step): Job {
    return jobBuilderFactory["upsertContractsJob"]
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
    return stepBuilderFactory.get("upsertProvidersStep")
      .tasklet(providerSetup)
      .build()
  }

  @Bean
  fun upsertContractDetailsStep(
    contractDetailsSetupTasklet: ContractDetailsSetupTasklet,
  ): Step {
    return stepBuilderFactory.get("upsertContractDetailsStep")
      .tasklet(contractDetailsSetupTasklet)
      .build()
  }

  @Bean
  fun upsertContractStep(
    reader: ContractDefinitionReader,
    processor: UpsertContractProcessor,
  ): Step {
    return stepBuilderFactory.get("upsertContractStep")
      .chunk<ContractDefinition, Intervention>(10)
      .reader(reader)
      .processor(processor)
      .writer {}
      .build()
  }
}
