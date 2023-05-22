package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jobs.oneoff.linksupersededappointments

import org.springframework.batch.item.ItemWriter
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Appointment

@Component
class LinkSupersededAppointmentsWriter : ItemWriter<Appointment?> {
  override fun write(items: MutableList<out Appointment?>) {
  }
}
