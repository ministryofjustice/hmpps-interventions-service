package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events

import org.springframework.context.ApplicationEvent
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AuthUser
import java.time.OffsetDateTime
import java.util.UUID

class EndOfServiceReportSubmittedEvent(
  source: Any,
  val referralId: UUID,
  val detailUrl: String,
  val endOfServiceReportId: UUID,
  val submittedAt: OffsetDateTime,
  val submittedBy: AuthUser
) : ApplicationEvent(source) {
  override fun toString(): String {
    return "EndOfServiceReportSubmittedEvent(referralId=$referralId, endOfServiceReportId=$endOfServiceReportId, submittedAt=$submittedAt)"
  }
}
