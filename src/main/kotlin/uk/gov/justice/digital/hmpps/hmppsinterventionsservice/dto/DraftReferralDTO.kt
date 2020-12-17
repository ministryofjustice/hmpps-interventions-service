package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto

import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Referral
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID
import javax.validation.constraints.Future

data class DraftReferralDTO(
  val id: UUID? = null,
  val created: OffsetDateTime? = null,
  val completionDeadline: LocalDate? = null,
) {
  companion object {
    fun from(referral: Referral): DraftReferralDTO {
      return DraftReferralDTO(
        referral.id!!,
        referral.created!!,
        referral.completionDeadline,
      )
    }
  }
}
