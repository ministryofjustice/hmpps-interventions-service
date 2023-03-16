package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jobs.oneoff.linksupersededappointments

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.AppointmentCleaner
import java.time.OffsetDateTime
import java.util.UUID

class LinkSupersededAppointmentsProcessorTest : IntegrationTestBase() {
  private lateinit var appointmentCleaner: AppointmentCleaner
  private lateinit var processor: LinkSupersededAppointmentsProcessor

  @BeforeEach
  fun beforeEach() {
    appointmentCleaner = AppointmentCleaner(
      appointmentRepository,
      deliverySessionRepository,
    )
    processor = LinkSupersededAppointmentsProcessor(appointmentCleaner)
  }

  @Test
  fun `adds superseded id to a superseded appointment`() {
    val activeReferral = setupAssistant.createAssignedReferral()
    val deliverySession = setupAssistant.createDeliverySession(1, duration = 1, OffsetDateTime.now().plusWeeks(1), referral = activeReferral)
    val supersededAppointment = deliverySession.appointments.first()

    setupAssistant.setSuperseded(supersededAppointment, deliverySession)

    val newId = UUID.randomUUID()
    val newAppointment = setupAssistant.createAppointment(activeReferral, id = newId, superseded = false)

    val result = processor.process(supersededAppointment)
    assertThat(result.id).isEqualTo(supersededAppointment.id)
    assertThat(result.superseded).isTrue()
    assertThat(result.supersededByAppointmentId).isNotNull()
    assertThat(result.supersededByAppointmentId).isEqualTo(newId)
  }

  @Test
  fun `makes no changes to active appointment`() {
    val activeReferral = setupAssistant.createAssignedReferral()
    val deliverySession = setupAssistant.createDeliverySession(1, duration = 1, OffsetDateTime.now().plusWeeks(1), referral = activeReferral)
    val newAppointment = deliverySession.appointments.first()

    val result = processor.process(newAppointment)
    assertThat(result.id).isEqualTo(newAppointment.id)
    assertThat(result.superseded).isFalse()
    assertThat(result).isEqualTo(newAppointment)
  }
}
