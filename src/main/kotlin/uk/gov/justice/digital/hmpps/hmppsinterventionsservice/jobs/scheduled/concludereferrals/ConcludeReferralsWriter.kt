package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jobs.scheduled.concludereferrals

import org.springframework.batch.item.Chunk
import org.springframework.batch.item.ItemWriter
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Referral

@Component
class ConcludeReferralsWriter : ItemWriter<Referral?> {
  override fun write(chunk: Chunk<out Referral?>) {}
}
