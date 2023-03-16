package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jobs.oneoff.linksupersededappointments

import org.assertj.core.api.Assertions.assertThat
import org.hibernate.SessionFactory
import org.junit.jupiter.api.Test
import org.springframework.batch.item.ExecutionContext
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jobs.oneoff.markstaleappointments.LinkSupersededAppointmentsReader
import java.time.OffsetDateTime

internal class LinkSupersededAppointmentsReaderTest @Autowired constructor(
  sessionFactory: SessionFactory,
) : IntegrationTestBase() {
  private val reader = LinkSupersededAppointmentsReader(sessionFactory)

  @Test
  fun `finds superseded appointments`() {
    val activeReferral = setupAssistant.createAssignedReferral()
    val deliverySession = setupAssistant.createDeliverySession(1, duration = 1, OffsetDateTime.now().plusWeeks(1), referral = activeReferral)
    val supersededAppointment = deliverySession.appointments.first()
    supersededAppointment.superseded = true
    appointmentRepository.save(supersededAppointment)
    deliverySessionRepository.save(deliverySession)

    reader.open(ExecutionContext())
    assertThat(reader.read()?.id).isEqualTo(supersededAppointment.id)
  }

  @Test
  fun `skips appointments that are not superseded`() {
    val activeReferral = setupAssistant.createAssignedReferral()
    val deliverySession = setupAssistant.createDeliverySession(1, duration = 1, OffsetDateTime.now().plusWeeks(1), referral = activeReferral)

    reader.open(ExecutionContext())
    assertThat(reader.read()).isNull()
  }
}
