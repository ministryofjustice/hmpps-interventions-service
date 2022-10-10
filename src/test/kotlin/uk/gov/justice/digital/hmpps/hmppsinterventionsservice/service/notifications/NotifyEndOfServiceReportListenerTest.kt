package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.notifications

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.component.EmailSender
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.EndOfServiceReportSubmittedEvent
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AuthUser
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.ReferralService
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.ResponsibleProbationPractitioner
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ReferralFactory
import java.time.OffsetDateTime
import java.util.UUID

class NotifyEndOfServiceReportListenerTest {
  private val emailSender = mock<EmailSender>()
  private val referralService = mock<ReferralService>()
  private val referralFactory = ReferralFactory()
  private val now = OffsetDateTime.now()

  private val referral = referralFactory.createSent()

  private val submittedEvent = EndOfServiceReportSubmittedEvent(
    source = "source",
    referralId = referral.id,
    detailUrl = "https://detail-url.example.org/eosr/id",
    endOfServiceReportId = UUID.fromString("f5033a88-a71d-4b8a-b2e1-b80e749f6d2c"),
    submittedAt = now,
    submittedBy = AuthUser("eosrSubmitterId", "eosrSubmitterSource", "eosrSubmitterUserName")
  )

  private fun notifyService(): NotifyEndOfServiceReportListener {
    whenever(referralService.getSentReferral(referral.id)).thenReturn(referral)
    return NotifyEndOfServiceReportListener(
      "template",
      "http://example.com",
      "/end-of-service-report/{id}",
      emailSender,
      referralService
    )
  }

  @Test
  fun `end of service report submitted event does not send email when user details are not available`() {
    whenever(referralService.getResponsibleProbationPractitioner(any())).thenThrow(RuntimeException::class.java)
    assertThrows<RuntimeException> {
      notifyService().onApplicationEvent(submittedEvent)
    }
    verifyNoInteractions(emailSender)
  }

  @Test
  fun `end of service report submitted event generates valid url and sends an email`() {
    whenever(referralService.getResponsibleProbationPractitioner(any()))
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
