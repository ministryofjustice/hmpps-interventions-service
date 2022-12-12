package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jobs.movereferrals

import org.hibernate.SessionFactory
import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory
import org.springframework.batch.core.job.DefaultJobParametersValidator
import org.springframework.batch.item.ItemProcessor
import org.springframework.batch.item.ItemWriter
import org.springframework.batch.item.database.BeanPropertyItemSqlParameterSourceProvider
import org.springframework.batch.item.database.HibernateCursorItemReader
import org.springframework.batch.item.database.JdbcBatchItemWriter
import org.springframework.batch.item.database.builder.HibernateCursorItemReaderBuilder
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder
import org.springframework.batch.item.file.FlatFileItemWriter
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.FileSystemResource
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Intervention
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Referral
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.reporting.BatchUtils
import java.util.*


@Configuration
@EnableBatchProcessing
class MoveReferralsJobConfiguration(
  private val jobBuilderFactory: JobBuilderFactory,
  private val stepBuilderFactory: StepBuilderFactory,
  private val listener: MoveReferralsJobListener,
) {
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
      .queryString("select r from Referral r where r.intervention.dynamicFrameworkContract.contractReference = :fromContract")
      .parameterValues(
        mapOf(
          "fromContract" to fromContract,
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
