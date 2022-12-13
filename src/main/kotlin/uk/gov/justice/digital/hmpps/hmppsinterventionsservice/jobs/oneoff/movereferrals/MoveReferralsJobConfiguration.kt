package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jobs.oneoff.movereferrals

import org.hibernate.SessionFactory
import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory
import org.springframework.batch.core.job.DefaultJobParametersValidator
import org.springframework.batch.item.ItemWriter
import org.springframework.batch.item.database.HibernateCursorItemReader
import org.springframework.batch.item.database.builder.HibernateCursorItemReaderBuilder
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Referral

@Configuration
@EnableBatchProcessing
class MoveReferralsJobConfiguration(
  private val jobBuilderFactory: JobBuilderFactory,
  private val stepBuilderFactory: StepBuilderFactory,
  private val listener: MoveReferralsJobListener,
) {
  private val transferSignalSubject = "Authority to transfer case"
  private val transferSignalText = "On behalf of Advance Charity Ltd, I request and give permission for this referral to be transferred to Women in Prison, as of December 2022."

  @Bean
  @JobScope
  fun moveReferralsReader(
    @Value("#{jobParameters['fromContract']}") fromContract: String,
    @Value("#{jobParameters['toContract']}") toContract: String,
    sessionFactory: SessionFactory,
  ): HibernateCursorItemReader<Referral> {
    return HibernateCursorItemReaderBuilder<Referral>()
      .name("moveReferralsReader")
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
  fun moveReferralsJob(updateInterventionIdStep: Step): Job {
    val validator = DefaultJobParametersValidator()
    validator.setRequiredKeys(
      arrayOf(
        "fromContract",
        "toContract",
        "timestamp",
      )
    )

    return jobBuilderFactory["moveReferralsJob"]
      .validator(validator)
      .listener(listener)
      .start(updateInterventionIdStep)
      .build()
  }

  @Bean
  fun updateInterventionIdStep(
    moveReferralsReader: HibernateCursorItemReader<Referral>,
    processor: MoveReferralsProcessor,
    writer: ItemWriter<Referral?>,
  ): Step {
    return stepBuilderFactory.get("updateInterventionIdStep")
      .chunk<Referral, Referral>(10)
      .reader(moveReferralsReader)
      .processor(processor)
      .writer(writer)
      .build()
  }
}

@Component
class noOpWriter() : ItemWriter<Referral> {
  override fun write(items: MutableList<out Referral>) {
    /* no-op */
  }
}
