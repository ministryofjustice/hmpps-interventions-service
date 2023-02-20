package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service

import org.springframework.context.annotation.Lazy
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.server.ServerWebInputException
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.authorization.UserMapper
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.AmendComplexityLevelDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.AmendDesiredOutcomesDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.AmendNeedsAndRequirementsDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.ChangelogUpdateDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.ReferralAmendmentDetails
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.UpdateReferralDetailsDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.ReferralEventPublisher
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AuthUser
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Changelog
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Referral
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ReferralDetails
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.SelectedDesiredOutcomesMapping
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ChangelogRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ComplexityLevelRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.DesiredOutcomeRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ReferralRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ServiceCategoryRepository
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

enum class AmendTopic {
  COMPLEXITY_LEVEL,
  DESIRED_OUTCOMES,
  COMPLETION_DATETIME,
  MAXIMUM_ENFORCEABLE_DAYS,
  NEEDS_AND_REQUIREMENTS_HAS_ADDITIONAL_RESPONSIBILITIES,
  NEEDS_AND_REQUIREMENTS_ACCESSIBILITY_NEEDS,
  NEEDS_AND_REQUIREMENTS_ADDITIONAL_INFORMATION,
  NEEDS_AND_REQUIREMENTS_INTERPRETER_REQUIRED,
}

@Service
@Transactional
class AmendReferralService(
  private val referralEventPublisher: ReferralEventPublisher,
  private val changelogRepository: ChangelogRepository,
  private val referralRepository: ReferralRepository,
  private val serviceCategoryRepository: ServiceCategoryRepository,
  private val complexityLevelRepository: ComplexityLevelRepository,
  private val desiredOutcomeRepository: DesiredOutcomeRepository,
  private val userMapper: UserMapper,
  @Lazy private val referralService: ReferralService
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

  fun amendCaringOrEmploymentResponsibilities(
    referralId: UUID,
    amendNeedsAndRequirementsDTO: AmendNeedsAndRequirementsDTO,
    authentication: JwtAuthenticationToken
  ) {
    val referral = getSentReferralForAuthenticatedUser(referralId, authentication)

    val oldValues = mutableListOf<String>()
    referral.hasAdditionalResponsibilities?.let { oldValues.add(referral.hasAdditionalResponsibilities.toString()) }
    referral.whenUnavailable ?.let { oldValues.add(referral.whenUnavailable!!) }

    val newValues = mutableListOf<String>()
    newValues.add(amendNeedsAndRequirementsDTO.hasAdditionalResponsibilities.toString())
    amendNeedsAndRequirementsDTO.whenUnavailable ?.let { newValues.add(amendNeedsAndRequirementsDTO.whenUnavailable) }

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

  fun getChangeLogById(changeLogId: UUID, authentication: JwtAuthenticationToken): ChangelogUpdateDTO {
    val changelog = changelogRepository.findById(changeLogId)
      .orElseThrow { throw ResponseStatusException(HttpStatus.NOT_FOUND, "Change log not found for [id=$changeLogId]") }

    when (changelog.topic) {
      AmendTopic.COMPLEXITY_LEVEL -> {
        val oldComplexityLevel = complexityLevelRepository.findById(UUID.fromString(changelog.oldVal.values[0]))
        val newComplexityLevel = complexityLevelRepository.findById(UUID.fromString(changelog.newVal.values[0]))
        return ChangelogUpdateDTO(changelog = changelog, oldValue = oldComplexityLevel.get().title, newValue = newComplexityLevel.get().title)
      }
      AmendTopic.DESIRED_OUTCOMES -> {
        val oldDesiredOutcomes = changelog.oldVal.values.map {
          val desiredOutcome = desiredOutcomeRepository.findById(UUID.fromString(it))
          desiredOutcome.get().description
        }.toList()
        val newDesiredOutcomes = changelog.newVal.values.map {
          val desiredOutcome = desiredOutcomeRepository.findById(UUID.fromString(it))
          desiredOutcome.get().description
        }.toList()
        return ChangelogUpdateDTO(changelog = changelog, oldValues = oldDesiredOutcomes, newValues = newDesiredOutcomes)
      }
      AmendTopic.NEEDS_AND_REQUIREMENTS_INTERPRETER_REQUIRED -> {
        return ChangelogUpdateDTO(
          changelog = changelog,
          oldValue = generateDescription(changelog.oldVal.values),
          newValue = generateDescription(changelog.newVal.values)
        )
      }
      AmendTopic.NEEDS_AND_REQUIREMENTS_HAS_ADDITIONAL_RESPONSIBILITIES -> {
        return ChangelogUpdateDTO(
          changelog = changelog,
          oldValue = generateDescription(changelog.oldVal.values),
          newValue = generateDescription(changelog.newVal.values)
        )
      }
      AmendTopic.COMPLETION_DATETIME -> {
        val originalDateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val newDateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")
        val oldDate = originalDateTimeFormatter.parse(changelog.oldVal.values[0])
        val formattedOldCompletionDate = newDateTimeFormatter.format(oldDate)
        val newDate = originalDateTimeFormatter.parse(changelog.newVal.values[0])
        val formattedNewCompletionDate = newDateTimeFormatter.format(newDate)
        return ChangelogUpdateDTO(
          changelog = changelog,
          oldValue = formattedOldCompletionDate,
          newValue = formattedNewCompletionDate
        )
      }
      else -> {}
    }
    return ChangelogUpdateDTO(changelog)
  }

  private fun processChangeLog(
    amendTopic: AmendTopic,
    oldValue: ReferralAmendmentDetails,
    newValue: ReferralAmendmentDetails,
    referralId: UUID,
    actor: AuthUser,
    update: UpdateReferralDetailsDTO
  ) {
    val changelog = Changelog(
      referralId,
      UUID.randomUUID(),
      amendTopic,
      oldValue,
      newValue,
      update.reasonForChange,
      OffsetDateTime.now(),
      actor
    )

    changelogRepository.save(changelog)
  }

  fun logChanges(referralDetails: ReferralDetails, update: UpdateReferralDetailsDTO, actor: AuthUser) {
    if (update.completionDeadline != null) {
      val oldValue = ReferralAmendmentDetails(listOf(referralDetails.completionDeadline.toString()))
      val newValue = ReferralAmendmentDetails(listOf(update.completionDeadline.toString()))
      processChangeLog(AmendTopic.COMPLETION_DATETIME, oldValue, newValue, referralDetails.referralId, actor, update)
    } else {
      val oldValue = ReferralAmendmentDetails(listOf(referralDetails.maximumEnforceableDays.toString()))
      val newValue = ReferralAmendmentDetails(listOf(update.maximumEnforceableDays.toString()))
      processChangeLog(AmendTopic.MAXIMUM_ENFORCEABLE_DAYS, oldValue, newValue, referralDetails.referralId, actor, update)
    }
  }
  private fun generateDescription(values: List<String>): String {
    val booleanValue = values.first { it == "true" || it == "false" }
    return if (booleanValue == "true") "Yes-${values.first { it != "true" }}" else "No"
  }

  fun amendAccessibilityNeeds(
    referralId: UUID,
    amendNeedsAndRequirementsDTO: AmendNeedsAndRequirementsDTO,
    authentication: JwtAuthenticationToken
  ) {

    val referral = getSentReferralForAuthenticatedUser(referralId, authentication)
    val oldValues = mutableListOf<String>()

    referral.accessibilityNeeds?.let { oldValues.add(referral.accessibilityNeeds!!) }

    val newValues = mutableListOf<String>()
    amendNeedsAndRequirementsDTO.accessibilityNeeds?.let { newValues.add(amendNeedsAndRequirementsDTO.accessibilityNeeds!!) }
    referral.accessibilityNeeds = amendNeedsAndRequirementsDTO.accessibilityNeeds

    val changelog = Changelog(
      referral.id,
      UUID.randomUUID(),
      AmendTopic.NEEDS_AND_REQUIREMENTS_ACCESSIBILITY_NEEDS,
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

  fun amendIdentifyNeeds(referralId: UUID, amendNeedsAndRequirementsDTO: AmendNeedsAndRequirementsDTO, authentication: JwtAuthenticationToken) {
    val referral = getSentReferralForAuthenticatedUser(referralId, authentication)
    val oldValues = mutableListOf<String>()
    referral.additionalNeedsInformation?.let { oldValues.add(referral.additionalNeedsInformation!!) }

    val newValues = mutableListOf<String>()
    amendNeedsAndRequirementsDTO.additionalNeedsInformation?.let { newValues.add(amendNeedsAndRequirementsDTO.additionalNeedsInformation!!) }
    referral.additionalNeedsInformation = amendNeedsAndRequirementsDTO.additionalNeedsInformation

    val changelog = Changelog(
      referral.id,
      UUID.randomUUID(),
      AmendTopic.NEEDS_AND_REQUIREMENTS_ADDITIONAL_INFORMATION,
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

  fun amendInterpreterRequired(referralId: UUID, amendNeedsAndRequirementsDTO: AmendNeedsAndRequirementsDTO, authentication: JwtAuthenticationToken) {

    val referral = getSentReferralForAuthenticatedUser(referralId, authentication)
    val oldValues = mutableListOf<String>()
    referral.needsInterpreter?.let { oldValues.add(referral.needsInterpreter.toString()) }
    referral.interpreterLanguage ?.let { oldValues.add(referral.interpreterLanguage!!) }

    val newValues = mutableListOf<String>()
    newValues.add(amendNeedsAndRequirementsDTO.needsInterpreter.toString())
    amendNeedsAndRequirementsDTO.interpreterLanguage ?.let { newValues.add(amendNeedsAndRequirementsDTO.interpreterLanguage!!) }

    referral.needsInterpreter = amendNeedsAndRequirementsDTO.needsInterpreter
    referral.interpreterLanguage = amendNeedsAndRequirementsDTO.interpreterLanguage

    val changelog = Changelog(
      referral.id,
      UUID.randomUUID(),
      AmendTopic.NEEDS_AND_REQUIREMENTS_INTERPRETER_REQUIRED,
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
}
