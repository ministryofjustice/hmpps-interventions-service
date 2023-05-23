package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jobs.oneoff.linksupersededappointments

import org.assertj.core.api.Assertions.assertThat
import org.hibernate.SessionFactory
import org.junit.jupiter.api.Test
import org.springframework.batch.item.ExecutionContext
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.integration.IntegrationTestBase
import java.time.OffsetDateTime

class LinkSupersededAppointmentsReaderTest @Autowired constructor(
  sessionFactory: SessionFactory,
) : IntegrationTestBase() {
  private val reader = LinkSupersededAppointmentsReader(sessionFactory)

  // test disabled as it won't pass once constraint has been applied
  fun `finds superseded appointments`() {
    val activeReferral = setupAssistant.createAssignedReferral()
    val deliverySession = setupAssistant.createDeliverySession(1, duration = 1, OffsetDateTime.now().plusWeeks(1), referral = activeReferral)
    val supersededAppointment = deliverySession.appointments.first()
    setupAssistant.setSuperseded(supersededAppointment, deliverySession)

    reader.open(ExecutionContext())
    assertThat(reader.read()?.id).isEqualTo(supersededAppointment.id)
  }

  @Test
  fun `skips appointments that are not superseded`() {
    val activeReferral = setupAssistant.createAssignedReferral()
    val deliverySession = setupAssistant.createDeliverySession(1, duration = 1, OffsetDateTime.now().plusWeeks(1), referral = activeReferral)
    val targetAppointment = deliverySession.appointments.first()
    setupAssistant.setSuperseded(targetAppointment, deliverySession, false)

    reader.open(ExecutionContext())
    assertThat(reader.read()).isNull()
    reader.close()
  }
}
