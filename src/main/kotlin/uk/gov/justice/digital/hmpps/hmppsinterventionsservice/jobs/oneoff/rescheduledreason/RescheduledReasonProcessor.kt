package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jobs.oneoff.rescheduledreason

import mu.KLogging
import net.logstash.logback.argument.StructuredArguments.kv
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.item.ItemProcessor
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Appointment
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.AppointmentRepository

@Component
@JobScope
class RescheduledReasonProcessor(
  private val appointmentRepository: AppointmentRepository,
) : ItemProcessor<Appointment, Appointment> {
  companion object : KLogging()

  override fun process(appointment: Appointment): Appointment {
    logger.info("processing appointment {} for rescheduled reason", kv("appointmentId", appointment.id))
    appointment.rescheduledReason = "---"

    return appointmentRepository.save(appointment)
  }
}
