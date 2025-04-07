package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity

import jakarta.annotation.Nullable
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToMany
import jakarta.validation.constraints.NotNull
import org.hibernate.annotations.Fetch
import org.hibernate.annotations.FetchMode.JOIN
import java.time.OffsetDateTime
import java.util.UUID

@Entity
data class DesiredOutcome(
  @Id val id: UUID,
  @NotNull val description: String,
  @NotNull
  @Column(name = "service_category_id")
  val serviceCategoryId: UUID,
  @Nullable
  @Column(name = "deprecated_at")
  val deprecatedAt: OffsetDateTime?,
  @OneToMany
  @JoinColumn(name = "desired_outcome_id")
  val desiredOutcomeFilterRules: MutableSet<DesiredOutcomeFilterRule>
){
//  override fun toString(): String = "ActionPlanAppointmentEvent(t)"
}
