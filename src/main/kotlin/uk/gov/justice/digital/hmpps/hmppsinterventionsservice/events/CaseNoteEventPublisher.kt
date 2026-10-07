package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events

import jakarta.persistence.EntityManager
import org.springframework.context.ApplicationEvent
import org.springframework.context.ApplicationEventPublisher
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
  val sendEmail: Boolean?,
) : ApplicationEvent(source),
  EntityEvent {
  override fun reloadIn(entityManager: EntityManager) = CreateCaseNoteEvent(source, caseNoteId, entityManager.reload(sentBy), detailUrl, referralId, sendEmail)

  override fun toString(): String = "CreateCaseNoteEvent(caseNoteId=$caseNoteId, referralId=$referralId)"
}

@Component
class CaseNoteEventPublisher(
  private val applicationEventPublisher: ApplicationEventPublisher,
  private val locationMapper: LocationMapper,
) {
  fun caseNoteSentEvent(caseNote: CaseNote, sendEmail: Boolean?) {
    applicationEventPublisher.publishEvent(
      CreateCaseNoteEvent(
        this,
        caseNote.id,
        caseNote.sentBy,
        caseNoteUrl(caseNote),
        caseNote.referral.id,
        sendEmail,
      ),
    )
  }

  private fun caseNoteUrl(caseNote: CaseNote): String {
    val path = locationMapper.getPathFromControllerMethod(CaseNoteController::getCaseNote)
    return locationMapper.expandPathToCurrentContextPathUrl(path, caseNote.id).toString()
  }
}
