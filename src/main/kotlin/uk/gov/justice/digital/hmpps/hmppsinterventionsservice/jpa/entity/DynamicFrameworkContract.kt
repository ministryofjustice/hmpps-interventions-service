package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity

import java.time.LocalDate
import java.util.UUID
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToMany
import javax.persistence.ManyToOne
import javax.validation.constraints.NotNull

@Entity
data class DynamicFrameworkContract(
  @Id val id: UUID,

  @NotNull @ManyToOne @JoinColumn(name = "service_category_id")
  val serviceCategory: ServiceCategory,

  @NotNull @ManyToOne @JoinColumn(name = "service_provider_id")
  val serviceProvider: ServiceProvider,

  @NotNull val startDate: LocalDate,
  @NotNull val endDate: LocalDate,

  // a contract is offered for a single NPS region,
  // or one-or-more PCC regions within an NPS region.
  // if `npsRegion` is set, `pccRegions` should contain
  // all PCC regions within the NPS region.
  @ManyToOne @JoinColumn(name = "nps_region_id") val npsRegion: NPSRegion? = null,
  @ManyToMany val pccRegions: Set<PCCRegion>,

  @NotNull val minimumAge: Int,
  val maximumAge: Int?,
  @NotNull val allowsFemale: Boolean,
  @NotNull val allowsMale: Boolean,
)
