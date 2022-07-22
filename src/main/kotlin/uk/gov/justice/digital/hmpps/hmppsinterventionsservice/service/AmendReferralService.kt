package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service

import org.springframework.http.HttpStatus
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.authorization.UserMapper
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.AmendComplexityLevelDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.AmendNeedAndRequirementDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.ReferralAmendmentDetails
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.ReferralEventPublisher
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Changelog
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Referral
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ChangelogRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ReferralRepository
import java.time.OffsetDateTime
import java.util.UUID
import javax.transaction.Transactional

@Service
@Transactional
class AmendReferralService(
  private val referralEventPublisher: ReferralEventPublisher,
  private val changelogRepository: ChangelogRepository,
  private val referralRepository: ReferralRepository,
  private val userMapper: UserMapper,
  private val referralService: ReferralService
) {

  fun updateComplexityLevel(referral: Referral, update: AmendComplexityLevelDTO, authentication: JwtAuthenticationToken): AmendComplexityLevelDTO? {
    val actor = userMapper.fromToken(authentication)
    val complexityLevel = referral.complexityLevelIds?.getValue(update.serviceCategoryId)

    referral.complexityLevelIds?.put(update.serviceCategoryId, update.complexityLevelId)

    val oldValue = ReferralAmendmentDetails(listOf(complexityLevel.toString()))
    val newValue = ReferralAmendmentDetails(listOf(update.complexityLevelId.toString()))

    val changelog = Changelog(
      referral.id,
      UUID.randomUUID(),
      "COMPLEXITY_LEVEL",
      oldValue,
      newValue,
      update.reasonForChange,
      OffsetDateTime.now(),
      actor
    )
    changelogRepository.save(changelog)
    referralRepository.save(referral)
    referralEventPublisher.referralComplexityChangedEvent(referral)

    return update
  }

  fun getSentReferralForAuthenticatedUser(authentication: JwtAuthenticationToken, id: UUID): Referral {
    val user = userMapper.fromToken(authentication)
    return referralService.getSentReferralForUser(id, user)
      ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "sent referral not found [id=$id]")
  }

  fun updateNeedAndRequirement(referral: Referral, update: AmendNeedAndRequirementDTO, authentication: JwtAuthenticationToken): AmendNeedAndRequirementDTO? {
    val actor = userMapper.fromToken(authentication)
    val oldValue = ReferralAmendmentDetails(
      furtherInformation = referral.furtherInformation,
      additionalNeedsInformation = referral.additionalNeedsInformation,
      accessibilityNeeds = referral.accessibilityNeeds,
      needsInterpreter = referral.needsInterpreter,
      interpreterLanguage = referral.interpreterLanguage,
      hasAdditionalResponsibilities = referral.hasAdditionalResponsibilities,
      whenUnavailable = referral.whenUnavailable
    )
    val newValue = ReferralAmendmentDetails(
      additionalNeedsInformation = update.additionalNeedsInformation,
      furtherInformation = update.furtherInformation,
      accessibilityNeeds = update.accessibilityNeeds,
      needsInterpreter = update.needsInterpreter,
      interpreterLanguage = update.interpreterLanguage,
      hasAdditionalResponsibilities = update.hasAdditionalResponsibilities,
      whenUnavailable = update.whenUnavailable

    )
    referral.additionalNeedsInformation = update.additionalNeedsInformation
    referral.accessibilityNeeds = update.accessibilityNeeds
    referral.needsInterpreter = update.needsInterpreter
    referral.interpreterLanguage = update.interpreterLanguage
    referral.hasAdditionalResponsibilities = update.hasAdditionalResponsibilities
    referral.whenUnavailable = update.whenUnavailable
    referral.furtherInformation = update.furtherInformation

    val changelog = Changelog(
      referral.id,
      UUID.randomUUID(),
      "NEED_AND_REQUIREMENT",
      oldValue,
      newValue,
      update.reasonForChange,
      OffsetDateTime.now(),
      actor
    )
    changelogRepository.save(changelog)
    referralRepository.save(referral)
    referralEventPublisher.referralNeedAndRequirementsChangedEvent(referral)

    return update
  }
}
