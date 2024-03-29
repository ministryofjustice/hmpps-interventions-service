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
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.EndOfServiceReportFactory
import java.net.URI

class EndOfServiceReportEventPublisherTest {

  private val eventPublisher = mock<ApplicationEventPublisher>()
  private val locationMapper = mock<LocationMapper>()
  private val endOfServiceReportFactory = EndOfServiceReportFactory()

  @Test
  fun `builds an end of service report submit event and publishes it`() {
    val endOfServiceReport = endOfServiceReportFactory.create()

    val uri = URI.create("http://localhost/end-of-service-report/${endOfServiceReport.id}")
    whenever(locationMapper.expandPathToCurrentContextPathUrl("/end-of-service-report/{id}", endOfServiceReport.id))
      .thenReturn(uri)
    whenever(locationMapper.getPathFromControllerMethod(EndOfServiceReportController::getEndOfServiceReportById))
      .thenReturn("/end-of-service-report/{id}")
    val publisher = EndOfServiceReportEventPublisher(eventPublisher, locationMapper)

    publisher.endOfServiceReportSubmittedEvent(endOfServiceReport)

    val eventCaptor = argumentCaptor<EndOfServiceReportEvent>()
    verify(eventPublisher).publishEvent(eventCaptor.capture())
    val event = eventCaptor.firstValue

    Assertions.assertThat(event.source).isSameAs(publisher)
    Assertions.assertThat(event.type).isSameAs(EndOfServiceReportEventType.SUBMITTED)
    Assertions.assertThat(event.endOfServiceReport).isSameAs(endOfServiceReport)
    Assertions.assertThat(event.detailUrl).isEqualTo(uri.toString())
  }
}
