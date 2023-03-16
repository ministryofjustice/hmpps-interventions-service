package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jobs.oneoff.markstaleappointments

import org.assertj.core.api.Assertions.assertThat
import org.hibernate.SessionFactory
import org.junit.jupiter.api.Test
import org.springframework.batch.item.ExecutionContext
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.integration.IntegrationTestBase
import java.time.OffsetDateTime
import java.util.UUID

class MarkStaleAppointmentsReaderTest @Autowired constructor(
  sessionFactory: SessionFactory,
) : IntegrationTestBase() {
  private val reader = MarkStaleAppointmentsReader(sessionFactory)

  @Test
  fun `finds target appointments`() {
    val activeReferral = setupAssistant.createAssignedReferral()
    val deliverySession = setupAssistant.createDeliverySession(1, duration = 1, OffsetDateTime.now().plusWeeks(1), referral = activeReferral)
    var targetAppointment = deliverySession.appointments.first()
    targetAppointment.id = UUID.fromString("82e2fbbe-1bb4-4967-8ee6-81aa072fd44b")
    appointmentRepository.save(targetAppointment)
    deliverySessionRepository.save(deliverySession)

    reader.open(ExecutionContext())
    assertThat(reader.read()?.id).isEqualTo(targetAppointment.id)
  }

  @Test
  fun `skips appointments that are not targetted`() {
    val activeReferral = setupAssistant.createAssignedReferral()
    val deliverySession = setupAssistant.createDeliverySession(1, duration = 1, OffsetDateTime.now().plusWeeks(1), referral = activeReferral)

    reader.open(ExecutionContext())
    assertThat(reader.read()).isNull()
  }
}
