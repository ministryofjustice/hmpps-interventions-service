package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jobs.oneoff.markstaleappointments

import org.hibernate.SessionFactory
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.item.database.HibernateCursorItemReader
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Appointment

@Component
@JobScope
@Configuration
class MarkStaleAppointmentsReader(
  sessionFactory: SessionFactory,
  @Value("#{jobParameters['appointmentsStr']}") appointmentsStr: String,
) : HibernateCursorItemReader<Appointment>() {
  init {
    val targetIds = parseIds(appointmentsStr)
    this.setName("markStaleAppointmentsReader")
    this.setSessionFactory(sessionFactory)
    this.setQueryString(
      "SELECT a FROM Appointment a where CAST(a.id as text) IN :targetIds",
    )
    setParameterValues(
      mapOf(
        "targetIds" to targetIds,
      ),
    )
  }

  fun parseIds(appointmentStr: String): Array<String> {
    return appointmentStr.split(",").toTypedArray()
  }
}
