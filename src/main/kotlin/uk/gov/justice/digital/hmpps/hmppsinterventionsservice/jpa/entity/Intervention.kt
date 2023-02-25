package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.validation.constraints.NotNull
import java.time.OffsetDateTime
import java.util.UUID

@Entity
data class Intervention(
  @NotNull @Id
  val id: UUID,
  @NotNull val createdAt: OffsetDateTime,

  @NotNull var title: String,
  @NotNull var description: String,
  @NotNull val incomingReferralDistributionEmail: String,

  @NotNull
  @OneToOne
  @JoinColumn(name = "dynamic_framework_contract_id")
  val dynamicFrameworkContract: DynamicFrameworkContract,
)
