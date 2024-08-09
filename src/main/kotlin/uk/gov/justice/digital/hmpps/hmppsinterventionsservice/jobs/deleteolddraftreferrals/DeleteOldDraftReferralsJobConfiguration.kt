package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jobs.oneoff.transferreferrals

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
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jobs.deleteolddraftreferrals.DeleteOldDraftReferralsReader
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jobs.oneoff.OnStartupJobLauncherFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.DraftReferral
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.reporting.TimestampIncrementer

@Configuration
@EnableTransactionManagement
class DeleteOldDraftReferralsJobConfiguration(
  private val jobRepository: JobRepository,
  private val listener: DeleteOldDraftReferralsJobListener,
  private val onStartupJobLauncherFactory: OnStartupJobLauncherFactory,
) {
  @Bean
  fun deleteOldDraftReferralsJobLauncher(deleteOldDraftReferralsJob: Job): ApplicationRunner {
    return onStartupJobLauncherFactory.makeBatchLauncher(deleteOldDraftReferralsJob)
  }

  @Bean
  fun deleteOldDraftReferralsJob(deleteOldDraftReferralsStep: Step): Job {
    return JobBuilder("deleteOldDraftReferralsJob", jobRepository)
      .incrementer(TimestampIncrementer())
      .listener(listener)
      .start(deleteOldDraftReferralsStep)
      .build()
  }

  @Bean
  fun deleteOldDraftReferralsStep(
    reader: DeleteOldDraftReferralsReader,
    processor: DeleteOldDraftReferralsProcessor,
    writer: DeleteOldDraftReferralsWriter,
    transactionManager: PlatformTransactionManager,
  ): Step {
    return StepBuilder("deleteOldDraftReferralsStep", jobRepository)
      .chunk<DraftReferral, DraftReferral>(100, transactionManager)
      .reader(reader)
      .processor(processor)
      .writer(writer)
      .build()
  }
}
