package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity

import com.fasterxml.jackson.annotation.JsonValue
import com.vladmihalcea.hibernate.type.basic.PostgreSQLEnumType
import org.hibernate.annotations.Fetch
import org.hibernate.annotations.FetchMode
import org.hibernate.annotations.Type
import org.hibernate.annotations.TypeDef
import java.time.OffsetDateTime
import java.util.UUID
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.Id
import javax.persistence.ManyToOne
import javax.persistence.OneToOne
import javax.persistence.Table
import javax.validation.constraints.NotNull

@Entity
@Table(name = "action_plan_appointment")
data class ActionPlanAppointment(

  // Attributes
  @NotNull val sessionNumber: Int,


  // Required
  @NotNull @ManyToOne val actionPlan: ActionPlan,
  @Id val id: UUID,

  @NotNull @OneToOne @Fetch(FetchMode.JOIN) val appointment: Appointment,
)


