package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jobs.oneoff.markstaleappointments
import mu.KLogging
import net.logstash.logback.argument.StructuredArguments.kv
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.item.ItemProcessor
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Appointment
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.AppointmentCleaner

@Component
@JobScope
class MarkStaleAppointmentsProcessor(
  private val appointentCleaner: AppointmentCleaner,
) : ItemProcessor<Appointment, Appointment> {
  companion object : KLogging()

  override fun process(appointment: Appointment): Appointment {
    logger.info("processing appointment {} for stale tagging", kv("appointmentId", appointment.id))

    return appointentCleaner.markAppoinmentAsStale(appointment)
  }
}
