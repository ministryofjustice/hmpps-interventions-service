package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "withdrawal_reason")
data class WithdrawalReason(
  @Id val code: String,
  val description: String,
  val grouping: String,
)
