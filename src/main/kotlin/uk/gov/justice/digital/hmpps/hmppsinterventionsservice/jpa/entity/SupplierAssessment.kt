package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity

import org.hibernate.annotations.Fetch
import org.hibernate.annotations.FetchMode
import java.util.UUID
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import jakarta.validation.constraints.NotNull

@Entity
@Table(name = "supplier_assessment")
data class SupplierAssessment(
  @Id val id: UUID,
  @NotNull
  @OneToOne(fetch = FetchType.LAZY)
  val referral: Referral,

  @JoinTable(
    name = "supplier_assessment_appointment",
    joinColumns = [JoinColumn(name = "supplier_assessment_id")],
    inverseJoinColumns = [JoinColumn(name = "appointment_id")],
  )
  @NotNull
  @OneToMany
  @Fetch(FetchMode.JOIN)
  val appointments: MutableSet<Appointment> = mutableSetOf(),
) {
  val currentAppointment: Appointment?
    get() = appointments.maxByOrNull { it.createdAt }

  val firstAppointment: Appointment?
    get() = appointments.minByOrNull { it.createdAt }

  val firstAppointmentWithNonAttendance: Appointment?
    get() = appointments.filter { it.attended == Attended.NO }.minByOrNull { it.appointmentTime }

  val firstAttendedAppointment: Appointment?
    get() = appointments.filter { listOf(Attended.YES, Attended.LATE).contains(it.attended) }.minByOrNull { it.appointmentTime }
}
