package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity

import org.hibernate.annotations.Fetch
import org.hibernate.annotations.FetchMode
import java.util.UUID
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.ManyToOne
import javax.persistence.OneToOne
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

  @NotNull @OneToOne @Fetch(FetchMode.JOIN) val appointment: Appointment,
)
