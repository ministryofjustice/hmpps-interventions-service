package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jobs.oneoff.rescheduledreason

import jakarta.persistence.EntityManagerFactory
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.item.database.JpaCursorItemReader
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Appointment

@Component
@JobScope
class RescheduledReasonReader(
  entityManagerFactory: EntityManagerFactory,
) : JpaCursorItemReader<Appointment>() {
  init {
    this.setName("rescheduledReasonReader")
    this.setEntityManagerFactory(entityManagerFactory)
    this.setQueryString(
      "SELECT ap " +
        "FROM Appointment ap " +
        "WHERE ap.superseded is true ",
    )
  }
}
