package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jobs.oneoff.approveactionplans

import org.springframework.batch.item.Chunk
import org.springframework.batch.item.ItemWriter
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ActionPlan

@Component
class ApproveActionPlansWriter : ItemWriter<ActionPlan?> {
  override fun write(chunk: Chunk<out ActionPlan?>) {}
}
