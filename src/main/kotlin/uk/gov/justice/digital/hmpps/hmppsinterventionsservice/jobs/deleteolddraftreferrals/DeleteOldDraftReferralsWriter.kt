package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jobs.oneoff.transferreferrals

import org.springframework.batch.item.Chunk
import org.springframework.batch.item.ItemWriter
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.DraftReferral

@Component
class DeleteOldDraftReferralsWriter : ItemWriter<DraftReferral?> {
  override fun write(chunk: Chunk<out DraftReferral?>) {}
}
