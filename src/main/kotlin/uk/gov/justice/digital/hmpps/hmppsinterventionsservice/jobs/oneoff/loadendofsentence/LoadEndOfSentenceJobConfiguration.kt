package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jobs.oneoff.transferreferrals

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
class LoadEndOfSentenceJobConfiguration(
  private val jobRepository: JobRepository,
  private val listener: LoadEndOfSentenceJobListener,
  private val onStartupJobLauncherFactory: OnStartupJobLauncherFactory,
  @Qualifier("batchJobBuilderFactory") private val jobBuilderFactory: JobBuilderFactory,
  @Qualifier("batchStepBuilderFactory") private val stepBuilderFactory: StepBuilderFactory,
) {
  @Bean
  fun loadEndOfSentenceJobLauncher(loadEndOfSentenceJob: Job): ApplicationRunner {
    return onStartupJobLauncherFactory.makeBatchLauncher(loadEndOfSentenceJob)
  }

  @Bean
  fun loadEndOfSentenceJob(loadEndOfSentenceStep: Step): Job {
    return jobBuilderFactory.get("loadEndOfSentenceJob")
      .incrementer(TimestampIncrementer())
      .listener(listener)
      .start(loadEndOfSentenceStep)
      .build()
  }

  @Bean
  fun loadEndOfSentenceStep(
    reader: LoadEndOfSentenceReader,
    processor: LoadEndOfSentenceProcessor,
    writer: LoadEndOfSentenceWriter,
    transactionManager: PlatformTransactionManager,
  ): Step {
    return stepBuilderFactory.get("loadEndOfSentenceStep")
      .chunk<Referral, Referral>(20000, transactionManager)
      .reader(reader)
      .processor(processor)
      .writer(writer)
      .build()
  }
}
