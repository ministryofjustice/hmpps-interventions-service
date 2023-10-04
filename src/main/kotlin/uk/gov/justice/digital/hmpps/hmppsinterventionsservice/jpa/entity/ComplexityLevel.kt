package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.validation.constraints.NotNull
import java.util.UUID

@Entity
data class ComplexityLevel(
  @Id val id: UUID,
  val title: String,
  val description: String,
  @Enumerated(EnumType.STRING)
  @Column(name = "complexity")
  val complexity: Complexity,
  @NotNull
  @Column(name = "service_category_id")
  val serviceCategoryId: UUID? = null,
)

enum class Complexity {
  LOW,
  MEDIUM,
  HIGH,
}
