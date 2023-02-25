package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity

import com.vladmihalcea.hibernate.type.basic.PostgreSQLEnumType
import org.hibernate.annotations.Type
import org.hibernate.annotations.TypeDef
import org.hibernate.annotations.TypeDefs
import java.util.UUID
import jakarta.persistence.Column
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Entity
import jakarta.persistence.Id
import javax.validation.constraints.NotNull

@Entity
@TypeDefs(
  value = [
    TypeDef(name = "complexities", typeClass = PostgreSQLEnumType::class),
  ],
)
data class ComplexityLevel(
  @Id val id: UUID,
  val title: String,
  val description: String,
  @Type(type = "complexities")
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
  ;
}
