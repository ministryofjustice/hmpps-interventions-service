package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.component.LocationMapper
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.controller.CaseNoteController
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AuthUser
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.CaseNote
import java.util.UUID

class CreateCaseNoteEvent(
  source: Any,
  val caseNoteId: UUID,
  val sentBy: AuthUser,
  val detailUrl: String,
  val referralId: UUID,
) : TraceableEvent(source) {
  override fun toString(): String = "CreateCaseNoteEvent(caseNoteId=$caseNoteId, referralId=$referralId)"
}

@Component
class CaseNoteEventPublisher(
  private val eventPublisher: TracePropagatingEventPublisher,
  private val locationMapper: LocationMapper,
) {
  fun caseNoteSentEvent(caseNote: CaseNote) {
    eventPublisher.publishEvent(
      CreateCaseNoteEvent(
        this,
        caseNote.id,
        caseNote.sentBy,
        caseNoteUrl(caseNote),
        caseNote.referral.id,
      )
    )
  }

  private fun caseNoteUrl(caseNote: CaseNote): String {
    val path = locationMapper.getPathFromControllerMethod(CaseNoteController::getCaseNote)
    return locationMapper.expandPathToCurrentContextPathUrl(path, caseNote.id).toString()
  }
}
