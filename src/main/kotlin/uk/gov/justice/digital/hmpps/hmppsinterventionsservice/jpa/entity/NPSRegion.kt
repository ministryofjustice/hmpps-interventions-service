package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.validation.constraints.NotNull

@Entity
@Table(name = "nps_region")
data class NPSRegion(
  @NotNull @Id
  val id: Char,
  @NotNull val name: String,
)
