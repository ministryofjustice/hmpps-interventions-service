package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto

import java.time.LocalDate
import java.util.UUID

data class UpdateReferralDetailsDTO(
  val maximumEnforceableDays: Int?,
  val completionDeadline: LocalDate?,
  val furtherInformation: String?,
  val reasonForChange: String,
) {
  val isValidUpdate: Boolean get() = maximumEnforceableDays != null || completionDeadline !== null || furtherInformation !== null
}

data class ReferralDetailsDTO(
  val referralId: UUID,
  val maximumEnforceableDays: Int?,
  val completionDeadline: LocalDate?,
  val furtherInformation: String?,
)
