package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.validation.constraints.NotNull
import java.util.UUID

@Entity
data class DesiredOutcome(
  @Id val id: UUID,
  @NotNull val description: String,
  @NotNull
  @Column(name = "service_category_id")
  val serviceCategoryId: UUID,
)
