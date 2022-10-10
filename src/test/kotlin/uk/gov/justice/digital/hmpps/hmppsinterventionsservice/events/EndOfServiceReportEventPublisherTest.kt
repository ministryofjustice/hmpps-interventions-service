package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.context.ApplicationEventPublisher
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.component.LocationMapper
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.controller.EndOfServiceReportController
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AuthUser
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.EndOfServiceReportFactory
import java.net.URI
import java.time.OffsetDateTime

class EndOfServiceReportEventPublisherTest {
  private val eventPublisher = mock<ApplicationEventPublisher>()
  private val locationMapper = mock<LocationMapper>()
  private val endOfServiceReportFactory = EndOfServiceReportFactory()
  private val publisher = EndOfServiceReportEventPublisher(eventPublisher, locationMapper)

  @Test
  fun `builds an end of service report submit event and publishes it`() {
    val submittedAt = OffsetDateTime.now()
    val submittedBy = AuthUser("submitterId", "submitterSource", "submitterUsername")
    val endOfServiceReport = endOfServiceReportFactory.create(
      submittedAt = submittedAt,
      submittedBy = submittedBy
    )

    val uri = "http://localhost/end-of-service-report/${endOfServiceReport.id}"
    whenever(locationMapper.expandPathToCurrentContextPathUrl("/end-of-service-report/{id}", endOfServiceReport.id))
      .thenReturn(URI.create(uri))
    whenever(locationMapper.getPathFromControllerMethod(EndOfServiceReportController::getEndOfServiceReportById))
      .thenReturn("/end-of-service-report/{id}")

    publisher.publishSubmittedEvent(endOfServiceReport)

    val eventCaptor = argumentCaptor<EndOfServiceReportSubmittedEvent>()
    verify(eventPublisher).publishEvent(eventCaptor.capture())
    val event = eventCaptor.firstValue

    Assertions.assertThat(event.source).isSameAs(publisher)
    Assertions.assertThat(event.referralId).isNotNull.isEqualTo(endOfServiceReport.referral.id)
    Assertions.assertThat(event.detailUrl).isEqualTo(uri)
    Assertions.assertThat(event.endOfServiceReportId).isNotNull.isEqualTo(endOfServiceReport.id)
    Assertions.assertThat(event.submittedAt).isNotNull.isEqualTo(submittedAt)
    Assertions.assertThat(event.submittedBy).isNotNull.isEqualTo(submittedBy)
  }
}
