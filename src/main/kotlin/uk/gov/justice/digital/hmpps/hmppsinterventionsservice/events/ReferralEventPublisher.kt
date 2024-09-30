package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events

import mu.KLogging
import org.springframework.context.ApplicationEvent
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.component.LocationMapper
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.controller.ReferralController
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.AuthUserDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.ReferralDetailsDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AuthUser
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Referral
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ReferralDetails
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.ReferralConcludedState
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.ReferralWithdrawalState

enum class ReferralEventType {
  SENT,
  ASSIGNED,
  DETAILS_AMENDED,
  COMPLEXITY_LEVEL_AMENDED,
  DESIRED_OUTCOMES_AMENDED,
  NEEDS_AND_REQUIREMENTS_AMENDED,
  PRISON_ESTABLISHMENT_AMENDED,
  EXPECTED_RELEASE_DATE,
  PROBATION_OFFICE_AMENDED,
  PROBATION_PRACTITIONER_NAME_AMENDED,
}

class ReferralEvent(
  source: Any,
  val type: ReferralEventType,
  val referral: Referral,
  val detailUrl: String,
  val data: Map<String, Any?> = emptyMap(),
) : ApplicationEvent(source) {
  override fun toString(): String = "ReferralEvent(type=$type, referralId=${referral.id}, detailUrl='$detailUrl', source=$source)"
}

class ReferralEndingEvent(
  source: Any,
  val state: ReferralConcludedState,
  val referral: Referral,
  val detailUrl: String,
) : ApplicationEvent(source) {
  override fun toString(): String = "ReferralEndingEvent(state=$state, referralId=${referral.id}, detailUrl='$detailUrl', source=$source)"
}

class ReferralConcludedEvent(
  source: Any,
  val type: ReferralConcludedState,
  val referral: Referral,
  val detailUrl: String,
  val referralWithdrawalState: ReferralWithdrawalState? = null,
) : ApplicationEvent(source) {
  override fun toString(): String = "ReferralConcludedEvent(type=$type, referralId=${referral.id}, detailUrl='$detailUrl', source=$source)"
}

@Component
class ReferralEventPublisher(
  private val applicationEventPublisher: ApplicationEventPublisher,
  private val locationMapper: LocationMapper,
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

  fun referralPrisonEstablishmentChangedEvent(
    referral: Referral,
    oldPrisonEstablishment: String,
    newPrisonEstablishment: String,
    user: AuthUser,
  ) {
    applicationEventPublisher.publishEvent(
      ReferralEvent(
        this,
        ReferralEventType.PRISON_ESTABLISHMENT_AMENDED,
        referral,
        getSentReferralURL(referral),
        mapOf(
          "oldPrisonEstablishment" to oldPrisonEstablishment,
          "newPrisonEstablishment" to newPrisonEstablishment,
          "currentAssignee" to referral.currentAssignee?.let { AuthUserDTO.from(it) },
          "crn" to referral.serviceUserCRN,
          "sentBy" to referral.sentBy,
          "createdBy" to referral.createdBy,
          "updater" to user,
        ),
      ),
    )
  }

  fun referralExpectedReleaseDateChangedEvent(
    referral: Referral,
    oldExpectedReleaseDateDetails: String,
    newExpectedReleaseDateDetails: String,
    user: AuthUser,
  ) {
    applicationEventPublisher.publishEvent(
      ReferralEvent(
        this,
        ReferralEventType.EXPECTED_RELEASE_DATE,
        referral,
        getSentReferralURL(referral),
        mapOf(
          "oldExpectedReleaseDateDetails" to oldExpectedReleaseDateDetails,
          "newExpectedReleaseDateDetails" to newExpectedReleaseDateDetails,
          "currentAssignee" to referral.currentAssignee?.let { AuthUserDTO.from(it) },
          "crn" to referral.serviceUserCRN,
          "sentBy" to referral.sentBy,
          "createdBy" to referral.createdBy,
          "updater" to user,
        ),
      ),
    )
  }

  fun referralProbationOfficeChangedEvent(
    referral: Referral,
    oldProbationOffice: String,
    newProbationOffice: String,
    user: AuthUser,
    preventEmailNotification: Boolean?,
  ) {
    applicationEventPublisher.publishEvent(
      ReferralEvent(
        this,
        ReferralEventType.PROBATION_OFFICE_AMENDED,
        referral,
        getSentReferralURL(referral),
        mapOf(
          "oldProbationOffice" to oldProbationOffice,
          "newProbationOffice" to newProbationOffice,
          "currentAssignee" to referral.currentAssignee?.let { AuthUserDTO.from(it) },
          "crn" to referral.serviceUserCRN,
          "sentBy" to referral.sentBy,
          "createdBy" to referral.createdBy,
          "updater" to user,
          "preventEmailNotification" to preventEmailNotification,
        ),
      ),
    )
  }

  fun referralEndingEvent(referral: Referral, eventType: ReferralConcludedState) {
    applicationEventPublisher.publishEvent(ReferralEndingEvent(this, eventType, referral, getSentReferralURL(referral)))
  }

  fun referralConcludedEvent(
    referral: Referral,
    eventType: ReferralConcludedState,
    referralWithdrawalState: ReferralWithdrawalState? = null,
  ) {
    applicationEventPublisher.publishEvent(ReferralConcludedEvent(this, eventType, referral, getSentReferralURL(referral), referralWithdrawalState))
  }

  fun referralDetailsChangedEvent(referral: Referral, newDetails: ReferralDetails, previousDetails: ReferralDetails) {
    applicationEventPublisher.publishEvent(
      ReferralEvent(
        this,
        ReferralEventType.DETAILS_AMENDED,
        referral,
        getSentReferralURL(referral),
        mapOf(
          "newDetails" to ReferralDetailsDTO.from(newDetails),
          "previousDetails" to ReferralDetailsDTO.from(previousDetails),
          "currentAssignee" to referral.currentAssignee?.let { AuthUserDTO.from(it) },
          "crn" to referral.serviceUserCRN,
          "sentBy" to referral.sentBy,
          "createdBy" to referral.createdBy,
        ),
      ),
    )
  }

  fun referralProbationPractitionerNameChangedEvent(referral: Referral, newPpName: String, previousPpName: String) {
    applicationEventPublisher.publishEvent(
      ReferralEvent(
        this,
        ReferralEventType.PROBATION_PRACTITIONER_NAME_AMENDED,
        referral,
        getSentReferralURL(referral),
        mapOf(
          "newProbationPractitionerName" to newPpName,
          "oldProbationPractitionerName" to previousPpName,
          "currentAssignee" to referral.currentAssignee?.let { AuthUserDTO.from(it) },
          "crn" to referral.serviceUserCRN,
          "sentBy" to referral.sentBy,
          "createdBy" to referral.createdBy,
        ),
      ),
    )
  }

  private fun getSentReferralURL(referral: Referral): String {
    val path = locationMapper.getPathFromControllerMethod(ReferralController::getSentReferral)
    return locationMapper.expandPathToCurrentContextPathUrl(path, referral.id).toString()
  }
}
