package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToMany
import jakarta.persistence.OrderBy
import org.hibernate.annotations.CreationTimestamp
import java.time.OffsetDateTime
import java.util.UUID

@Entity
data class ServiceCategory(
  @Id val id: UUID,
  @CreationTimestamp val created: OffsetDateTime,
  val name: String,
  @OneToMany
  @JoinColumn(name = "service_category_id")
  @OrderBy("complexity")
  val complexityLevels: List<ComplexityLevel>,
  @OneToMany
  @JoinColumn(name = "service_category_id")
  val desiredOutcomes: List<DesiredOutcome>,
)
