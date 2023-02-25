package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id

@Entity
data class CancellationReason(
  @Id val code: String,
  val description: String,
)
