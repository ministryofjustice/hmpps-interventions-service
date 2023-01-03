package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.listeners

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.component.EmailSender
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.component.SNSPublisher
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.EventDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.EndOfServiceReportSubmittedEvent
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AuthUser
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.ReferralService
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.ResponsibleProbationPractitioner
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ReferralFactory
import java.time.OffsetDateTime
import java.util.UUID

private val now = OffsetDateTime.now()
private val submittedEvent = EndOfServiceReportSubmittedEvent(
  source = "source",
  referralId = UUID.fromString("56b40f96-0657-4e01-925c-da208a6fbcfd"),
  detailUrl = "https://detail-url.example.org/eosr/id",
  endOfServiceReportId = UUID.fromString("f5033a88-a71d-4b8a-b2e1-b80e749f6d2c"),
  submittedAt = now,
  submittedBy = AuthUser("eosrSubmitterId", "eosrSubmitterSource", "eosrSubmitterUserName")
)

internal class EndOfServiceReportDomainEventListenerTest {
  private val publisher = mock<SNSPublisher>()
  private val endOfServiceReportDomainEventListener = EndOfServiceReportDomainEventListener(publisher)

  @Test
  fun `publishes domain event message when an end of service report is submitted`() {
    endOfServiceReportDomainEventListener.onApplicationEvent(submittedEvent)

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

internal class EndOfServiceReportNotificationListenerTest {
  private val emailSender = mock<EmailSender>()
  private val referralService = mock<ReferralService>()
  private val referralFactory = ReferralFactory()

  private val referral = referralFactory.createSent(id = submittedEvent.referralId)

  private fun notifyService(): EndOfServiceReportNotificationListener {
    whenever(referralService.getSentReferral(referral.id)).thenReturn(referral)
    return EndOfServiceReportNotificationListener(
      "template",
      "http://example.com",
      "/end-of-service-report/{id}",
      emailSender,
      referralService
    )
  }

  @Test
  fun `end of service report submitted event does not send email when user details are not available`() {
    whenever(referralService.getResponsibleProbationPractitioner(referral)).thenThrow(RuntimeException::class.java)
    assertThrows<RuntimeException> {
      notifyService().onApplicationEvent(submittedEvent)
    }
    verifyNoInteractions(emailSender)
  }

  @Test
  fun `end of service report submitted event generates valid url and sends an email`() {
    whenever(referralService.getResponsibleProbationPractitioner(referral))
      .thenReturn(ResponsibleProbationPractitioner("abc", "abc@abc.abc", null, null, "def"))

    notifyService().onApplicationEvent(submittedEvent)
    val personalisationCaptor = argumentCaptor<Map<String, String>>()
    verify(emailSender).sendEmail(eq("template"), eq("abc@abc.abc"), personalisationCaptor.capture())
    Assertions.assertThat(personalisationCaptor.firstValue["ppFirstName"]).isEqualTo("abc")
    Assertions.assertThat(personalisationCaptor.firstValue["referralReference"]).isEqualTo("JS18726AC")
    Assertions.assertThat(personalisationCaptor.firstValue["endOfServiceReportLink"])
      .isEqualTo("http://example.com/end-of-service-report/f5033a88-a71d-4b8a-b2e1-b80e749f6d2c")
  }
}
