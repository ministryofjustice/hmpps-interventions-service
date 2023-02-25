package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity

import java.util.UUID
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany

@Entity
data class ContractType(
  @Id val id: UUID,
  val name: String,
  val code: String,

  @ManyToMany
  @JoinTable(
    name = "contract_type_service_category",
    joinColumns = [JoinColumn(name = "contract_type_id")],
    inverseJoinColumns = [JoinColumn(name = "service_category_id")],
  )
  val serviceCategories: Set<ServiceCategory> = setOf(),
)
