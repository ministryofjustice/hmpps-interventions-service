<<<<<<< HEAD
package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jobs.oneoff.linksupersededappointments
=======
package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jobs.oneoff.markstaleappointments
>>>>>>> 0faded70 (Rebase and apply merge conflict fixes for latest from main)

import org.hibernate.SessionFactory
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.item.database.HibernateCursorItemReader
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Appointment

@Component
@JobScope
class LinkSupersededAppointmentsReader(
  sessionFactory: SessionFactory,
) : HibernateCursorItemReader<Appointment>() {

  init {
    this.setName("linkSupersededAppointmentsReader")
    this.setSessionFactory(sessionFactory)
    this.setQueryString(
      "SELECT a FROM Appointment a where a.superseded = true",
    )
  }
}
