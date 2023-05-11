package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jobs.oneoff.markstaleappointments

import org.hibernate.SessionFactory
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.item.database.HibernateCursorItemReader
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Appointment

@Component
@JobScope
class MarkStaleAppointmentsReader(
  sessionFactory: SessionFactory,
) : HibernateCursorItemReader<Appointment>() {

  init {
    this.setName("markStaleAppointmentsReader")
    this.setSessionFactory(sessionFactory)
    this.setQueryString(
      "SELECT a FROM Appointment a where a.id in ('82e2fbbe-1bb4-4967-8ee6-81aa072fd44b')",
    )
    // 75834134-3fdf-4b29-ae6f-af8eff441cd0 PREPROD
    // 82e2fbbe-1bb4-4967-8ee6-81aa072fd44b DEV
  }
}
