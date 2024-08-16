package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jobs.oneoff.loadstatus

import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory
import org.springframework.batch.core.repository.JobRepository
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.EnableTransactionManagement
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jobs.oneoff.OnStartupJobLauncherFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Referral
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.reporting.TimestampIncrementer

@Configuration
@EnableTransactionManagement
class LoadStatusJobConfiguration(
  private val jobRepository: JobRepository,
  private val listener: LoadStatusJobListener,
  private val onStartupJobLauncherFactory: OnStartupJobLauncherFactory,
  @Qualifier("batchJobBuilderFactory") private val jobBuilderFactory: JobBuilderFactory,
  @Qualifier("batchStepBuilderFactory") private val stepBuilderFactory: StepBuilderFactory,
) {
  @Bean
  fun loadStatusJobLauncher(loadStatusJob: Job): ApplicationRunner {
    return onStartupJobLauncherFactory.makeBatchLauncher(loadStatusJob)
  }

  @Bean
  fun loadStatusJob(loadStatusStep: Step): Job {
    return jobBuilderFactory.get("loadStatusJob")
      .incrementer(TimestampIncrementer())
      .listener(listener)
      .start(loadStatusStep)
      .build()
  }

  @Bean
  fun loadStatusStep(
    reader: LoadStatusReader,
    processor: LoadStatusProcessor,
    writer: LoadStatusWriter,
    transactionManager: PlatformTransactionManager,
  ): Step {
    return stepBuilderFactory.get("loadStatusStep")
      .chunk<Referral, Referral>(5000, transactionManager)
      .reader(reader)
      .processor(processor)
      .writer(writer)
      .build()
  }
}
