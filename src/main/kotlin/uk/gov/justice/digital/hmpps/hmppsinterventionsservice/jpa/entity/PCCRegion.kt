package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity

import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.validation.constraints.NotNull

@Entity
@Table(name = "pcc_region")
data class PCCRegion(
  @NotNull @Id
  val id: PCCRegionID,
  @NotNull val name: String,
  @NotNull
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "nps_region_id")
  val npsRegion: NPSRegion,
)

typealias PCCRegionID = String
