package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jobs.oneoff.migration

import org.hibernate.SessionFactory
import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory
import org.springframework.batch.item.ItemProcessor
import org.springframework.batch.item.ItemWriter
import org.springframework.batch.item.database.HibernateCursorItemReader
import org.springframework.batch.item.database.builder.HibernateCursorItemReaderBuilder
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Changelog
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ReferralDetails

@Configuration
@EnableBatchProcessing
class ChangelogMigrationJobConfiguration(
  private val jobBuilderFactory: JobBuilderFactory,
  private val stepBuilderFactory: StepBuilderFactory,
  private val jobListener: ChangelogMigrationJobListener,
  @Value("\${spring.batch.jobs.migration.changelog.chunk-size}") private val chunkSize: Int
) {

  @Bean
  @JobScope
  fun referralDetailsReader(
    sessionFactory: SessionFactory,
  ): HibernateCursorItemReader<ReferralDetails> {
    // this reader returns referral entities which need processing for the report.
    return HibernateCursorItemReaderBuilder<ReferralDetails>()
      .name("changelogMigrationReader")
      .sessionFactory(sessionFactory)
      .queryString("SELECT r FROM ReferralDetails r")
      .build()
  }

  @Bean
  fun changelogMigrationJob(migrateChangelogStep: Step): Job {
    return jobBuilderFactory["changelogMigrationJob"]
      .listener(jobListener)
      .start(migrateChangelogStep)
      .build()
  }

  @Bean
  fun migrateChangelogStep(
    referralDetailsReader: HibernateCursorItemReader<ReferralDetails>,
    processor: ItemProcessor<ReferralDetails, Changelog?>,
    writer: ItemWriter<Changelog?>,
  ): Step {
    return stepBuilderFactory.get("migrateChangelog")
      .chunk<ReferralDetails, Changelog?>(chunkSize)
      .reader(referralDetailsReader)
      .processor(processor)
      .writer(writer)
      .build()
  }
}
