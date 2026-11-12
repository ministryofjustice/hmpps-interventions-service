package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
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
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.EndOfServiceReportEvent
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.EndOfServiceReportEventType
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ReferralServiceUserData
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.EndOfServiceReportFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ReferralFactory
import java.util.UUID

class NotifyEndOfServiceReportTest {
  private val emailSender = mock<EmailSender>()
  private val referralService = mock<ReferralService>()
  private val endOfServiceReportService = mock<EndOfServiceReportService>()
  private val endOfServiceReportFactory = EndOfServiceReportFactory()
  private val referralFactory = ReferralFactory()

  private val endOfServiceReport = endOfServiceReportFactory.create(
    id = UUID.fromString("42c7d267-0776-4272-a8e8-a673bfe30d0d"),
    referral = referralFactory.createSent(
      serviceUserData = ReferralServiceUserData(firstName = "Alex", lastName = "River"),
    ),
  )
  private val endOfServiceReportSubmittedEvent = EndOfServiceReportEvent(
    "source",
    EndOfServiceReportEventType.SUBMITTED,
    endOfServiceReport.id,
    "http://localhost:8080/end-of-service-report/42c7d267-0776-4272-a8e8-a673bfe30d0d",
  )

  @BeforeEach
  fun setup() {
    whenever(endOfServiceReportService.getEndOfServiceReport(endOfServiceReport.id)).thenReturn(endOfServiceReport)
  }

  private fun notifyService(): NotifyEndOfServiceReportService = NotifyEndOfServiceReportService(
    "template",
    "http://example.com",
    "/end-of-service-report/{id}",
    emailSender,
    referralService,
    endOfServiceReportService,
  )

  @Test
  fun `end of service report submitted event does not send email when user details are not available`() {
    whenever(referralService.getResponsibleProbationPractitioner(any())).thenThrow(RuntimeException::class.java)
    assertThrows<RuntimeException> {
      notifyService().onApplicationEvent(endOfServiceReportSubmittedEvent)
    }
    verifyNoInteractions(emailSender)
  }

  @Test
  fun `end of service report submitted event generates valid url and sends an email`() {
    whenever(referralService.getResponsibleProbationPractitioner(any()))
      .thenReturn(ResponsibleProbationPractitioner("abc", "abc@abc.abc", null, null, "def"))

    notifyService().onApplicationEvent(endOfServiceReportSubmittedEvent)
    verify(endOfServiceReportService).getEndOfServiceReport(endOfServiceReport.id)
    val personalisationCaptor = argumentCaptor<Map<String, String>>()
    verify(emailSender).sendEmail(eq("template"), eq("abc@abc.abc"), personalisationCaptor.capture())
    Assertions.assertThat(personalisationCaptor.firstValue["ppFirstName"]).isEqualTo("abc")
    Assertions.assertThat(personalisationCaptor.firstValue["referralReference"]).isEqualTo("JS18726AC")
    Assertions.assertThat(personalisationCaptor.firstValue["endOfServiceReportLink"]).isEqualTo("http://example.com/end-of-service-report/42c7d267-0776-4272-a8e8-a673bfe30d0d")
  }
}
