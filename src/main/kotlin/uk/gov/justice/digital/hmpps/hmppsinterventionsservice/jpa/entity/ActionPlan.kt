package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity

import jakarta.persistence.CollectionTable
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import jakarta.validation.constraints.NotNull
import org.hibernate.annotations.Fetch
import org.hibernate.annotations.FetchMode
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "action_plan")
data class ActionPlan(

  // Attributes
  var numberOfSessions: Int? = null,

  // Activities
  @ElementCollection
  @CollectionTable(name = "action_plan_activity", joinColumns = [JoinColumn(name = "action_plan_id")])
  @NotNull
  val activities: MutableList<ActionPlanActivity> = mutableListOf(),

  // Status
  @NotNull
  @ManyToOne
  @Fetch(FetchMode.JOIN)
  val createdBy: AuthUser,
  @NotNull val createdAt: OffsetDateTime,
  @ManyToOne
  @Fetch(FetchMode.JOIN)
  var submittedBy: AuthUser? = null,
  var submittedAt: OffsetDateTime? = null,
  @ManyToOne
  @Fetch(FetchMode.JOIN)
  var approvedBy: AuthUser? = null,
  var approvedAt: OffsetDateTime? = null,

  // Required
  @NotNull
  @OneToOne
  @Fetch(FetchMode.JOIN)
  val referral: Referral,
  @Id val id: UUID,
) {
  // This is to avoid stack overflow issues with the bi-directional association with referral
  override fun toString(): String {
    return "ActionPlan(referralId=${referral.id}, numberOfSessions=$numberOfSessions, activities=$activities, createdBy=$createdBy, createdAt=$createdAt, submittedBy=$submittedBy, submittedAt=$submittedAt, id=$id)"
  }

  override fun hashCode(): Int {
    return id.hashCode()
  }

  override fun equals(other: Any?): Boolean {
    if (other == null || other !is ActionPlan) {
      return false
    }

    return id == other.id
  }
}
