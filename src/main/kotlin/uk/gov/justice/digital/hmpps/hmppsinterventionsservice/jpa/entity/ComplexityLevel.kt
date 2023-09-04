package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity

import com.vladmihalcea.hibernate.type.basic.PostgreSQLEnumType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import javax.validation.constraints.NotNull
import org.hibernate.annotations.Type
import java.util.UUID

@Entity
data class ComplexityLevel(
  @Id val id: UUID,
  val title: String,
  val description: String,
  @Type(PostgreSQLEnumType::class)
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
