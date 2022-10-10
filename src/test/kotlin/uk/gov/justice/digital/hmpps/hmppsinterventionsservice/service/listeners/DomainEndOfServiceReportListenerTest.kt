package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.listeners

import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.component.SNSPublisher
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.EventDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.EndOfServiceReportSubmittedEvent
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AuthUser
import java.time.OffsetDateTime
import java.util.UUID

class DomainEndOfServiceReportListenerTest {
  private val publisher = mock<SNSPublisher>()
  private val domainEndOfServiceReportListener = DomainEndOfServiceReportListener(publisher)
  private val now = OffsetDateTime.now()

  private val submittedEvent = EndOfServiceReportSubmittedEvent(
    source = "source",
    referralId = UUID.fromString("56b40f96-0657-4e01-925c-da208a6fbcfd"),
    detailUrl = "https://detail-url.example.org/eosr/id",
    endOfServiceReportId = UUID.fromString("f5033a88-a71d-4b8a-b2e1-b80e749f6d2c"),
    submittedAt = now,
    submittedBy = AuthUser("eosrSubmitterId", "eosrSubmitterSource", "eosrSubmitterUserName")
  )

  @Test
  fun `publishes domain event message when an end of service report is submitted`() {
    domainEndOfServiceReportListener.onApplicationEvent(submittedEvent)

    val eventDTO = EventDTO(
      eventType = "intervention.end-of-service-report.submitted",
      description = "An end of service report has been submitted",
      detailUrl = "https://detail-url.example.org/eosr/id",
      occurredAt = now,
      additionalInformation = mapOf(
        "endOfServiceReportId" to "f5033a88-a71d-4b8a-b2e1-b80e749f6d2c",
        "submittedBy" to "eosrSubmitterUserName"
      )
    )

    verify(publisher).publish(submittedEvent.referralId, submittedEvent.submittedBy, eventDTO)
  }
}
