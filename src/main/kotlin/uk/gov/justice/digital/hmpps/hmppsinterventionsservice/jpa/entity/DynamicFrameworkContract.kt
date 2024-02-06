package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany
import jakarta.persistence.ManyToOne
import jakarta.persistence.NamedAttributeNode
import jakarta.persistence.NamedEntityGraph
import jakarta.persistence.NamedSubgraph
import jakarta.validation.constraints.NotNull
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@NamedEntityGraph(
  name = "entity-dynamicFrameWorkContract-graph",
  attributeNodes =
  [
    NamedAttributeNode("primeProvider"),
    NamedAttributeNode("npsRegion"),
    NamedAttributeNode(value = "pccRegion", subgraph = "pcc-region"),
    NamedAttributeNode("subcontractorProviders"),
    NamedAttributeNode(value = "contractType", subgraph = "contractType"),
  ],
  subgraphs = [
    NamedSubgraph(
      name = "pcc-region",
      attributeNodes = [
        NamedAttributeNode("npsRegion"),
      ],
    ),
    NamedSubgraph(
      name = "contractType",
      attributeNodes = [
        NamedAttributeNode("serviceCategories"),
      ],
    ),
  ],
)
@Entity
data class DynamicFrameworkContract(
  @Id val id: UUID,

  @NotNull
  @ManyToOne
  @JoinColumn(name = "prime_provider_id")
  val primeProvider: ServiceProvider,

  @Deprecated("use referralStartDate (hideBefore) instead")
  @NotNull
  val startDate: LocalDate,
  @Deprecated("use referralEndAt (hideAfter) instead")
  @NotNull
  val endDate: LocalDate,

  @NotNull var referralStartDate: LocalDate,
  var referralEndAt: OffsetDateTime? = null,

  @ManyToOne
  @JoinColumn(name = "nps_region_id")
  val npsRegion: NPSRegion? = null,

  @ManyToOne
  @JoinColumn(name = "pcc_region_id")
  val pccRegion: PCCRegion? = null,

  @NotNull val minimumAge: Int,
  val maximumAge: Int?,
  @NotNull val allowsFemale: Boolean,
  @NotNull val allowsMale: Boolean,

  val contractReference: String,

  @ManyToMany
  @JoinTable(
    name = "dynamic_framework_contract_sub_contractor",
    joinColumns = [JoinColumn(name = "dynamic_framework_contract_id")],
    inverseJoinColumns = [JoinColumn(name = "subcontractor_provider_id")],
  )
  val subcontractorProviders: MutableSet<ServiceProvider> = mutableSetOf(),

  @NotNull
  @ManyToOne
  @JoinColumn(name = "contract_type_id")
  val contractType: ContractType,

) {
  // using contract_reference for hashCode and equals because
  // it's guaranteed to have a unique hash (UUID isn't).
  // the field is enforced as unique at the database level.
  override fun hashCode(): Int {
    return contractReference.hashCode()
  }

  override fun equals(other: Any?): Boolean {
    if (other == null || other !is DynamicFrameworkContract) {
      return false
    }

    return contractReference == other.contractReference
  }
}
