package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jobs.routine.transferreferrals

import org.springframework.batch.infrastructure.item.Chunk
import org.springframework.batch.infrastructure.item.ItemWriter
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.DraftReferral

@Component
class DeleteOldDraftReferralsWriter : ItemWriter<DraftReferral> {
  override fun write(chunk: Chunk<out DraftReferral>) {}
}
