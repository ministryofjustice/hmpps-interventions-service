package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events

import mu.KLogging
import org.springframework.context.ApplicationEvent
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.component.LocationMapper
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.controller.ReferralController
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.AuthUserDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.ReferralDetailsDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Referral
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ReferralDetails
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.ReferralConcludedState

enum class ReferralEventType {
  SENT, ASSIGNED, DETAILS_AMENDED, COMPLEXITY_LEVEL_AMENDED, DESIRED_OUTCOMES_AMENDED, NEEDS_AND_REQUIREMENTS_AMENDED
}

class ReferralEvent(
  source: Any,
  val type: ReferralEventType,
  val referral: Referral,
  val detailUrl: String,
  val data: Map<String, Any?> = emptyMap()
) : ApplicationEvent(source) {
  override fun toString(): String {
    return "ReferralEvent(type=$type, referralId=${referral.id}, detailUrl='$detailUrl', source=$source)"
  }
}

class ReferralEndingEvent(
  source: Any,
  val state: ReferralConcludedState,
  val referral: Referral,
  val detailUrl: String,
) : ApplicationEvent(source) {
  override fun toString(): String {
    return "ReferralEndingEvent(state=$state, referralId=${referral.id}, detailUrl='$detailUrl', source=$source)"
  }
}

class ReferralConcludedEvent(
  source: Any,
  val type: ReferralConcludedState,
  val referral: Referral,
  val detailUrl: String,
) : ApplicationEvent(source) {
  override fun toString(): String {
    return "ReferralConcludedEvent(type=$type, referralId=${referral.id}, detailUrl='$detailUrl', source=$source)"
  }
}

@Component
class ReferralEventPublisher(
  private val applicationEventPublisher: ApplicationEventPublisher,
  private val locationMapper: LocationMapper
) {
  companion object : KLogging()

  fun referralSentEvent(referral: Referral) {
    applicationEventPublisher.publishEvent(ReferralEvent(this, ReferralEventType.SENT, referral, getSentReferralURL(referral)))
  }

  fun referralAssignedEvent(referral: Referral) {
    applicationEventPublisher.publishEvent(ReferralEvent(this, ReferralEventType.ASSIGNED, referral, getSentReferralURL(referral)))
  }

  fun referralComplexityChangedEvent(referral: Referral) {
    applicationEventPublisher.publishEvent(ReferralEvent(this, ReferralEventType.COMPLEXITY_LEVEL_AMENDED, referral, getSentReferralURL(referral)))
  }

  fun referralDesiredOutcomesChangedEvent(referral: Referral) {
    applicationEventPublisher.publishEvent(ReferralEvent(this, ReferralEventType.DESIRED_OUTCOMES_AMENDED, referral, getSentReferralURL(referral)))
  }

  fun referralNeedsAndRequirementsChangedEvent(referral: Referral) {
    applicationEventPublisher.publishEvent(ReferralEvent(this, ReferralEventType.NEEDS_AND_REQUIREMENTS_AMENDED, referral, getSentReferralURL(referral)))
  }

  fun referralEndingEvent(referral: Referral, eventType: ReferralConcludedState) {
    applicationEventPublisher.publishEvent(ReferralEndingEvent(this, eventType, referral, getSentReferralURL(referral)))
  }

  fun referralConcludedEvent(referral: Referral, eventType: ReferralConcludedState) {
    applicationEventPublisher.publishEvent(ReferralConcludedEvent(this, eventType, referral, getSentReferralURL(referral)))
  }

  fun referralDetailsChangedEvent(referral: Referral, newDetails: ReferralDetails, previousDetails: ReferralDetails) {
    applicationEventPublisher.publishEvent(
      ReferralEvent(
        this, ReferralEventType.DETAILS_AMENDED, referral, getSentReferralURL(referral),
        mapOf(
          "newDetails" to ReferralDetailsDTO.from(newDetails),
          "previousDetails" to ReferralDetailsDTO.from(previousDetails),
          "currentAssignee" to referral.currentAssignee?.let { AuthUserDTO.from(it) },
          "crn" to referral.serviceUserCRN,
          "sentBy" to referral.sentBy,
          "createdBy" to referral.createdBy
        )
      )
    )
  }

  private fun getSentReferralURL(referral: Referral): String {
    val path = locationMapper.getPathFromControllerMethod(ReferralController::getSentReferral)
    return locationMapper.expandPathToCurrentContextPathUrl(path, referral.id).toString()
  }
}
