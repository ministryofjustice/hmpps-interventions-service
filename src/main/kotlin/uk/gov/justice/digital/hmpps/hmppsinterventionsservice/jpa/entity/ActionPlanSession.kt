package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity

import org.hibernate.annotations.Fetch
import org.hibernate.annotations.FetchMode
import java.util.UUID
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.ManyToOne
import javax.persistence.OneToMany
import javax.persistence.Table
import javax.validation.constraints.NotNull

@Entity
@Table(name = "action_plan_session")
data class ActionPlanSession(

  // Attributes
  @NotNull val sessionNumber: Int,

  // Required
  @NotNull @ManyToOne val actionPlan: ActionPlan,
  @Id val id: UUID,

  @NotNull @OneToMany @Fetch(FetchMode.JOIN) val appointments: Set<Appointment>,
) {
  val appointment: Appointment
    get() = appointments.maxByOrNull { it.createdAt }!!
}
