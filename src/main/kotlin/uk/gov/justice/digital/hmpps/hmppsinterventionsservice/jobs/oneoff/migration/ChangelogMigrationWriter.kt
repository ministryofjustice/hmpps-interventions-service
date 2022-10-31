package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jobs.oneoff.migration

import mu.KLogging
import org.springframework.batch.item.ItemWriter
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Changelog
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ChangelogRepository

@Component
class ChangelogMigrationWriter(val changelogRepository: ChangelogRepository) : ItemWriter<Changelog?> {
  companion object : KLogging()
  override fun write(items: MutableList<out Changelog?>) {
    logger().info("writing into the change log table. The total amount of data loaded into changelog is {}", items.size)
    changelogRepository.saveAll(items.filterNotNull())
  }
}
