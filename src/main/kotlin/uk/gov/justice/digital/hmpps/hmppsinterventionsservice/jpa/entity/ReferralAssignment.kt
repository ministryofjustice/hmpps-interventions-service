package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity

import jakarta.persistence.Embeddable
import jakarta.persistence.ManyToOne
import org.hibernate.annotations.Fetch
import org.hibernate.annotations.FetchMode
import java.time.OffsetDateTime

@Embeddable
class ReferralAssignment(
  val assignedAt: OffsetDateTime,
  @ManyToOne
  @Fetch(FetchMode.JOIN)
  val assignedBy: AuthUser,
  @ManyToOne
  @Fetch(FetchMode.JOIN)
  val assignedTo: AuthUser,
  var superseded: Boolean = false,
)
