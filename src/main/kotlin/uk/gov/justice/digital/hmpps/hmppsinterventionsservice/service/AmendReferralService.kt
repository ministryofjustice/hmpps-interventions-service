package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service

import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.server.ServerWebInputException
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.authorization.UserMapper
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.AmendComplexityLevelDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.AmendDesiredOutcomesDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.AmendNeedsAndRequirementsDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.ReferralAmendmentDetails
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.ReferralEventPublisher
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Changelog
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Referral
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.SelectedDesiredOutcomesMapping
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ChangelogRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ReferralRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ServiceCategoryRepository
import java.time.OffsetDateTime
import java.util.UUID
import javax.transaction.Transactional

enum class AmendTopic {
  COMPLEXITY_LEVEL,
  DESIRED_OUTCOMES,
  NEEDS_AND_REQUIREMENTS_HAS_ADDITIONAL_RESPONSIBILITIES
}

@Service
@Transactional
class AmendReferralService(
  private val referralEventPublisher: ReferralEventPublisher,
  private val changelogRepository: ChangelogRepository,
  private val referralRepository: ReferralRepository,
  private val serviceCategoryRepository: ServiceCategoryRepository,
  private val userMapper: UserMapper,
  private val referralService: ReferralService
) {

  fun updateComplexityLevel(referralId: UUID, update: AmendComplexityLevelDTO, serviceCategoryId: UUID, authentication: JwtAuthenticationToken) {
    val referral = getSentReferralForAuthenticatedUser(referralId, authentication)

    if (referral.approvedActionPlan != null) {
      throw ServerWebInputException("complexity level cannot be updated: the action plan is already approved")
    }
    val actor = userMapper.fromToken(authentication)
    val complexityLevel = referral.complexityLevelIds?.get(serviceCategoryId)

    referral.complexityLevelIds?.put(serviceCategoryId, update.complexityLevelId)

    val oldValue = ReferralAmendmentDetails(listOf(complexityLevel!!.toString()))
    val newValue = ReferralAmendmentDetails(listOf(update.complexityLevelId.toString()))

    val changelog = Changelog(
      referral.id,
      UUID.randomUUID(),
      AmendTopic.COMPLEXITY_LEVEL,
      oldValue,
      newValue,
      update.reasonForChange,
      OffsetDateTime.now(),
      actor
    )
    changelogRepository.save(changelog)
    referralRepository.save(referral)
    referralEventPublisher.referralComplexityChangedEvent(referral)
  }

  fun updateReferralDesiredOutcomes(
    referralId: UUID,
    amendDesiredOutcomesDTO: AmendDesiredOutcomesDTO,
    authentication: JwtAuthenticationToken,
    serviceCategoryId: UUID
  ) {
    val referral = getSentReferralForAuthenticatedUser(referralId, authentication)

    val desiredOutcomeIds = amendDesiredOutcomesDTO.desiredOutcomesIds

    if (desiredOutcomeIds.isEmpty()) {
      throw ServerWebInputException("desired outcomes cannot be empty")
    }

    if (referral.approvedActionPlan != null) {
      throw ServerWebInputException("desired outcomes cannot be updated: the action plan is already approved")
    }

    if (referral.selectedServiceCategories.isNullOrEmpty()) {
      throw ServerWebInputException("desired outcomes cannot be updated: no service categories selected for this referral")
    }

    if (!referral.selectedServiceCategories!!.map { it.id }.contains(serviceCategoryId)) {
      throw ServerWebInputException("desired outcomes cannot be updated: specified service category not selected for this referral")
    }

    val validDesiredOutcomeIds = serviceCategoryRepository.findByIdOrNull(serviceCategoryId)?.desiredOutcomes?.map { it.id }
      ?: throw ServerWebInputException("desired outcomes cannot be updated: specified service category not found")

    if (!validDesiredOutcomeIds.containsAll(desiredOutcomeIds)) {
      throw ServerWebInputException("desired outcomes cannot be updated: at least one desired outcome is not valid for this service category")
    }

    if (referral.selectedDesiredOutcomes == null) {
      referral.selectedDesiredOutcomes = mutableListOf()
    }

    val actor = userMapper.fromToken(authentication)
    val oldValue = ReferralAmendmentDetails(values = referral.selectedDesiredOutcomes!!.map { it.desiredOutcomeId.toString() }.toList())
    val newValue = ReferralAmendmentDetails(values = desiredOutcomeIds.map { it.toString() }.toList())

    referral.selectedDesiredOutcomes!!.removeIf { desiredOutcome -> desiredOutcome.serviceCategoryId == serviceCategoryId }
    desiredOutcomeIds.forEach { desiredOutcomeId ->
      referral.selectedDesiredOutcomes!!.add(
        SelectedDesiredOutcomesMapping(serviceCategoryId, desiredOutcomeId)
      )
    }

    val changelog = Changelog(
      referral.id,
      UUID.randomUUID(),
      AmendTopic.DESIRED_OUTCOMES,
      oldValue,
      newValue,
      amendDesiredOutcomesDTO.reasonForChange,
      OffsetDateTime.now(),
      actor
    )
    changelogRepository.save(changelog)
    val savedReferral = referralRepository.save(referral)
    referralEventPublisher.referralDesiredOutcomesChangedEvent(savedReferral)
  }

  fun updateAmendCaringOrEmploymentResponsibilitiesDTO(
    referralId: UUID,
    amendNeedsAndRequirementsDTO: AmendNeedsAndRequirementsDTO,
    authentication: JwtAuthenticationToken
  ) {
    val referral = getSentReferralForAuthenticatedUser(referralId, authentication)

    val oldValues = mutableListOf<String>()
    if (referral.hasAdditionalResponsibilities != null) oldValues.add(referral.hasAdditionalResponsibilities.toString())
    if (referral.whenUnavailable != null) oldValues.add(referral.whenUnavailable!!)

    val newValues = mutableListOf<String>()
    newValues.add(amendNeedsAndRequirementsDTO.hasAdditionalResponsibilities.toString())
    if (amendNeedsAndRequirementsDTO.whenUnavailable != null) newValues.add(amendNeedsAndRequirementsDTO.whenUnavailable)

    referral.hasAdditionalResponsibilities = amendNeedsAndRequirementsDTO.hasAdditionalResponsibilities
    referral.whenUnavailable = amendNeedsAndRequirementsDTO.whenUnavailable

    val changelog = Changelog(
      referral.id,
      UUID.randomUUID(),
      AmendTopic.NEEDS_AND_REQUIREMENTS_HAS_ADDITIONAL_RESPONSIBILITIES,
      ReferralAmendmentDetails(values = oldValues),
      ReferralAmendmentDetails(values = newValues),
      amendNeedsAndRequirementsDTO.reasonForChange,
      OffsetDateTime.now(),
      userMapper.fromToken(authentication)
    )
    changelogRepository.save(changelog)
    val savedReferral = referralRepository.save(referral)
    referralEventPublisher.referralNeedsAndRequirementsChangedEvent(savedReferral)
  }

  fun getSentReferralForAuthenticatedUser(referralId: UUID, authentication: JwtAuthenticationToken): Referral {
    val user = userMapper.fromToken(authentication)
    return referralService.getSentReferralForUser(referralId, user)
      ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "sent referral not found [id=$referralId]")
  }

  fun getListOfChangeLogEntries(referral: Referral): List<Changelog> {
    return changelogRepository.findByReferralIdOrderByChangedAtDesc(referral.id)
  }
}
