package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id

@Entity
data class WithdrawalReason(
  @Id val code: String,
  val description: String,
)
