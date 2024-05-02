package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto

data class WithdrawReferralRequestDTO(
  val code: String,
  val comments: String,
  val withdrawalState: String,
)
