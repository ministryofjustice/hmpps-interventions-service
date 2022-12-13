package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jobs.oneoff.transferreferrals

import org.hibernate.SessionFactory
import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory
import org.springframework.batch.core.job.DefaultJobParametersValidator
import org.springframework.batch.item.database.HibernateCursorItemReader
import org.springframework.batch.item.database.builder.HibernateCursorItemReaderBuilder
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jobs.oneoff.OnStartupJobLauncherFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Referral
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.reporting.TimestampIncrementer

@Configuration
@EnableBatchProcessing
class TransferReferralsJobConfiguration(
  private val jobBuilderFactory: JobBuilderFactory,
  private val stepBuilderFactory: StepBuilderFactory,
  private val listener: TransferReferralsJobListener,
  private val onStartupJobLauncherFactory: OnStartupJobLauncherFactory,
) {
  private val transferSignalSubject = "Authority to transfer case"
  private val transferSignalText = "On behalf of Advance Charity Ltd, I request and give permission for this referral to be transferred to Women in Prison, as of December 2022."

  @Bean
  fun transferReferralsJobLauncher(transferReferralsJob: Job): ApplicationRunner {
    return onStartupJobLauncherFactory.makeBatchLauncher(transferReferralsJob)
  }

  @Bean
  @JobScope
  fun transferReferralsReader(
    @Value("#{jobParameters['fromContract']}") fromContract: String,
    @Value("#{jobParameters['toContract']}") toContract: String,
    sessionFactory: SessionFactory,
  ): HibernateCursorItemReader<Referral> {
    return HibernateCursorItemReaderBuilder<Referral>()
      .name("transferReferralsReader")
      .sessionFactory(sessionFactory)
      .queryString(
        "SELECT r FROM Referral r JOIN CaseNote n ON n.referralId = r.id " +
          "WHERE r.intervention.dynamicFrameworkContract.contractReference = :fromContract " +
          "  AND n.subject = :transferSignalSubject AND n.body = :transferSignalText"
      )
      .parameterValues(
        mapOf(
          "fromContract" to fromContract,
          "transferSignalSubject" to transferSignalSubject,
          "transferSignalText" to transferSignalText
        )
      )
      .build()
  }

  @Bean
  fun transferReferralsJob(transferReferralToInterventionStep: Step): Job {
    val validator = DefaultJobParametersValidator()
    validator.setRequiredKeys(
      arrayOf(
        "fromContract",
        "toContract",
        "timestamp",
      )
    )

    return jobBuilderFactory["transferReferralsJob"]
      .incrementer(TimestampIncrementer())
      .validator(validator)
      .listener(listener)
      .start(transferReferralToInterventionStep)
      .build()
  }

  @Bean
  fun transferReferralToInterventionStep(
    transferReferralsReader: HibernateCursorItemReader<Referral>,
    processor: TransferReferralsProcessor,
    writer: TransferReferralsWriter,
  ): Step {
    return stepBuilderFactory.get("transferReferralToInterventionStep")
      .chunk<Referral, Referral>(10)
      .reader(transferReferralsReader)
      .processor(processor)
      .writer(writer)
      .build()
  }
}
