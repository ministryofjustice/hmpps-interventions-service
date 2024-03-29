package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.context.ApplicationEventPublisher
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.component.LocationMapper
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.controller.CaseNoteController
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.CaseNoteFactory
import java.net.URI

internal class CaseNoteEventPublisherTest {
  private val eventPublisher = mock<ApplicationEventPublisher>()
  private val locationMapper = mock<LocationMapper>()
  private val caseNoteFactory = CaseNoteFactory()

  @Test
  fun `builds a case note sent event and publishes it`() {
    val caseNote = caseNoteFactory.create(subject = "subject", body = "body")
    val uri = URI.create("http://localhost/case-note/${caseNote.id}")
    whenever(locationMapper.expandPathToCurrentContextPathUrl("/case-note/{id}", caseNote.id)).thenReturn(uri)
    whenever(locationMapper.getPathFromControllerMethod(CaseNoteController::getCaseNote)).thenReturn("/case-note/{id}")
    val publisher = CaseNoteEventPublisher(eventPublisher, locationMapper)

    publisher.caseNoteSentEvent(caseNote, true)

    val eventCaptor = argumentCaptor<CreateCaseNoteEvent>()
    verify(eventPublisher).publishEvent(eventCaptor.capture())
    val event = eventCaptor.firstValue

    assertThat(event.source).isSameAs(publisher)
    assertThat(event.caseNoteId).isSameAs(caseNote.id)
    assertThat(event.sentBy).isSameAs(caseNote.sentBy)
    assertThat(event.detailUrl).isEqualTo(uri.toString())
    assertThat(event.referralId).isSameAs(caseNote.referral.id)
    assertThat(event.sendEmail).isEqualTo(true)
  }
}
