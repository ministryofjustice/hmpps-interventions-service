package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jobs.oneoff.linksupersededappointments

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jobs.oneoff.markstaleappointments.MarkStaleAppointmentsProcessor
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.AppointmentCleaner
import java.time.OffsetDateTime

class MarkStaleAppointmentsProcessorTest : IntegrationTestBase() {
  private lateinit var appointmentCleaner: AppointmentCleaner
  private lateinit var processor: MarkStaleAppointmentsProcessor

  @BeforeEach
  fun beforeEach() {
    appointmentCleaner = AppointmentCleaner(
      appointmentRepository,
      deliverySessionRepository,
      supplierAssessmentRepository,
    )
    processor = MarkStaleAppointmentsProcessor(appointmentCleaner)
  }

  @Test
  fun `sets stale flag on target appointment`() {
    val activeReferral = setupAssistant.createAssignedReferral()
    val deliverySession = setupAssistant.createDeliverySession(1, duration = 1, OffsetDateTime.now().plusWeeks(1), referral = activeReferral)
    val targetAppointment = deliverySession.appointments.first()
    targetAppointment.stale = false

    val result = processor.process(targetAppointment)
    assertThat(result.id).isEqualTo(targetAppointment.id)
    assertThat(result.stale).isTrue()
  }
}
