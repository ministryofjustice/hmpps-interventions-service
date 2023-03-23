package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jobs.oneoff.markstaleappointments

import org.assertj.core.api.Assertions.assertThat
import org.hibernate.SessionFactory
import org.junit.jupiter.api.Test
import org.springframework.batch.item.ExecutionContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.integration.IntegrationTestBase
import java.time.OffsetDateTime
import java.util.UUID

@ActiveProfiles("test")
class MarkStaleAppointmentsReaderTest @Autowired constructor(
  sessionFactory: SessionFactory,
  @Value("\${spring.batch.jobs.mark-stale-appointments.appointments}")
  private val appointmentsStr: String,
) : IntegrationTestBase() {
  private val reader = MarkStaleAppointmentsReader(sessionFactory, appointmentsStr)

  @Test
  fun `finds target appointments`() {
    val activeReferral = setupAssistant.createAssignedReferral()
    val deliverySession = setupAssistant.createDeliverySession(1, duration = 1, OffsetDateTime.now().plusWeeks(1), referral = activeReferral)
    var targetAppointment = deliverySession.appointments.first()
    setupAssistant.changeAppointmentId(targetAppointment, UUID.fromString("82e2fbbe-1bb4-4967-8ee6-81aa072fd44b"), deliverySession)

    reader.open(ExecutionContext())
    reader.setParameterValues(getJobParameters())
    assertThat(reader.read()?.id).isEqualTo(targetAppointment.id)
    reader.close()
  }

  @Test
  fun `responds with null when no appointments found`() {
    val activeReferral = setupAssistant.createAssignedReferral()
    val deliverySession = setupAssistant.createDeliverySession(1, duration = 1, OffsetDateTime.now().plusWeeks(1), referral = activeReferral)

    reader.open(ExecutionContext())
    reader.setParameterValues(getJobParameters())
    assertThat(reader.read()).isNull()
    reader.close()
  }

  @Test
  fun `skips appointments that are not targetted`() {
    val activeReferral = setupAssistant.createAssignedReferral()
    val deliverySession = setupAssistant.createDeliverySession(1, duration = 1, OffsetDateTime.now().plusWeeks(1), referral = activeReferral)
    var targetAppointment = deliverySession.appointments.first()
    setupAssistant.changeAppointmentId(targetAppointment, UUID.randomUUID(), deliverySession)

    reader.open(ExecutionContext())
    reader.setParameterValues(getJobParameters())
    assertThat(reader.read()).isNull()
    reader.close()
  }

  private fun getJobParameters(): Map<String, Any> {
    val params = HashMap<String, Any>()
    params.put("appointmentStr", appointmentsStr)
    return params
  }
}
