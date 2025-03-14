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
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.AmendExpectedReleaseDateDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.AmendNeedsAndRequirementsDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.AmendPrisonEstablishmentDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.AmendProbationOfficeDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.AmendProbationPractitionerEmailDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.AmendProbationPractitionerNameDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.AmendProbationPractitionerPhoneNumberDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.AmendProbationPractitionerTeamPhoneNumberDTO
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
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ProbationPractitionerDetailsRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ReferralLocationRepository
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
  REASON_FOR_REFERRAL,
  REASON_FOR_REFERRAL_FURTHER_INFORMATION,
  PRISON_ESTABLISHMENT,
  EXPECTED_RELEASE_DATE,
  EXPECTED_PROBATION_OFFICE,
  PROBATION_PRACTITIONER_PROBATION_OFFICE,
  PROBATION_PRACTITIONER_NAME,
  PROBATION_PRACTITIONER_EMAIL,
  PROBATION_PRACTITIONER_PHONE_NUMBER,
  PROBATION_PRACTITIONER_TEAM_PHONE_NUMBER,
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
  private val referralLocationRepository: ReferralLocationRepository,
  private val probationPractitionerDetailsRepository: ProbationPractitionerDetailsRepository,
  private val userMapper: UserMapper,
  @Lazy private val referralService: ReferralService,
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
      actor,
    )
    changelogRepository.save(changelog)
    referralRepository.save(referral)
    referralEventPublisher.referralComplexityChangedEvent(referral)
  }

  fun updateReferralDesiredOutcomes(
    referralId: UUID,
    amendDesiredOutcomesDTO: AmendDesiredOutcomesDTO,
    authentication: JwtAuthenticationToken,
    serviceCategoryId: UUID,
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
        SelectedDesiredOutcomesMapping(serviceCategoryId, desiredOutcomeId),
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
      actor,
    )
    changelogRepository.save(changelog)
    val savedReferral = referralRepository.save(referral)
    referralEventPublisher.referralDesiredOutcomesChangedEvent(savedReferral)
  }

  fun amendCaringOrEmploymentResponsibilities(
    referralId: UUID,
    amendNeedsAndRequirementsDTO: AmendNeedsAndRequirementsDTO,
    authentication: JwtAuthenticationToken,
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
      userMapper.fromToken(authentication),
    )
    changelogRepository.save(changelog)
    val savedReferral = referralRepository.save(referral)
    referralEventPublisher.referralNeedsAndRequirementsChangedEvent(savedReferral)
  }

  fun amendPrisonEstablishment(
    referralId: UUID,
    amendPrisonEstablishmentDTO: AmendPrisonEstablishmentDTO,
    authentication: JwtAuthenticationToken,
    user: AuthUser,
  ) {
    val referral = getSentReferralForAuthenticatedUser(referralId, authentication)

    val oldValues = mutableListOf<String>()
    referral.referralLocation?.prisonId?.let { oldValues.add(referral.referralLocation?.prisonId!!) }

    val newValues = mutableListOf<String>()
    newValues.add(amendPrisonEstablishmentDTO.personCustodyPrisonId)

    val referralLocation = referral.referralLocation
    referralLocation?.prisonId = amendPrisonEstablishmentDTO.personCustodyPrisonId

    val changelog = Changelog(
      referral.id,
      UUID.randomUUID(),
      AmendTopic.PRISON_ESTABLISHMENT,
      ReferralAmendmentDetails(values = oldValues),
      ReferralAmendmentDetails(values = newValues),
      amendPrisonEstablishmentDTO.reasonForChange,
      OffsetDateTime.now(),
      userMapper.fromToken(authentication),
    )
    changelogRepository.save(changelog)
    referralLocation?.let { referralLocationRepository.save(it) }
    referralEventPublisher.referralPrisonEstablishmentChangedEvent(referral, amendPrisonEstablishmentDTO.oldPrisonEstablishment, amendPrisonEstablishmentDTO.newPrisonEstablishment, user)
  }

  fun amendExpectedReleaseDate(
    referralId: UUID,
    amendExpectedReleaseDateDTO: AmendExpectedReleaseDateDTO,
    authentication: JwtAuthenticationToken,
    user: AuthUser,
  ) {
    val referral = getSentReferralForAuthenticatedUser(referralId, authentication)
    val expectedReleaseDate = referral.referralLocation?.expectedReleaseDate
    val expectedReleaseDateMissingReason = referral.referralLocation?.expectedReleaseDateMissingReason

    if ((amendExpectedReleaseDateDTO.expectedReleaseDate != null && expectedReleaseDate == amendExpectedReleaseDateDTO.expectedReleaseDate) ||
      (amendExpectedReleaseDateDTO.expectedReleaseDateMissingReason != null && expectedReleaseDateMissingReason == amendExpectedReleaseDateDTO.expectedReleaseDateMissingReason)
    ) {
      // do not amend or save in the change log if the values are same
      return
    }

    val oldValues = mutableListOf<String>()
    val newValues = mutableListOf<String>()
    var oldValueForNotify = ""
    var newValueForNotify = ""
    val formatter = DateTimeFormatter.ofPattern("dd-MMM-yyyy")
    val referralLocation = referral.referralLocation
    amendExpectedReleaseDateDTO.expectedReleaseDate?.let {
      if (referral.referralLocation?.expectedReleaseDate !== null) {
        oldValues.add(formatter.format(referral.referralLocation?.expectedReleaseDate!!))
        oldValueForNotify = formatter.format(referral.referralLocation?.expectedReleaseDate!!)
      } else if (referral.referralLocation?.expectedReleaseDateMissingReason != null) {
        oldValues.add(referral.referralLocation?.expectedReleaseDateMissingReason!!)
        oldValueForNotify = "Expected release date is not known"
      }
      newValues.add(formatter.format(amendExpectedReleaseDateDTO.expectedReleaseDate))
      newValueForNotify = formatter.format(amendExpectedReleaseDateDTO.expectedReleaseDate)
      referralLocation?.expectedReleaseDate = amendExpectedReleaseDateDTO.expectedReleaseDate
      referralLocation?.expectedReleaseDateMissingReason = null
    }

    amendExpectedReleaseDateDTO.expectedReleaseDateMissingReason?.let {
      if (referral.referralLocation?.expectedReleaseDateMissingReason != null) {
        oldValues.add(referral.referralLocation?.expectedReleaseDateMissingReason!!)
        oldValueForNotify = "Expected release date is not known"
      } else if (referral.referralLocation?.expectedReleaseDate !== null) {
        oldValues.add(formatter.format(referral.referralLocation?.expectedReleaseDate!!))
        oldValueForNotify = formatter.format(referral.referralLocation?.expectedReleaseDate!!)
      }
      newValues.add(amendExpectedReleaseDateDTO.expectedReleaseDateMissingReason)
      newValueForNotify = "Expected release date is not known"
      referralLocation?.expectedReleaseDateMissingReason = amendExpectedReleaseDateDTO.expectedReleaseDateMissingReason
      referralLocation?.expectedReleaseDate = null
    }

    val changelog = Changelog(
      referral.id,
      UUID.randomUUID(),
      AmendTopic.EXPECTED_RELEASE_DATE,
      ReferralAmendmentDetails(values = oldValues),
      ReferralAmendmentDetails(values = newValues),
      "",
      OffsetDateTime.now(),
      userMapper.fromToken(authentication),
    )
    changelogRepository.save(changelog)
    referralLocation?.let { referralLocationRepository.save(it) }
    referralEventPublisher.referralExpectedReleaseDateChangedEvent(referral, oldValueForNotify, newValueForNotify, user)
  }

  fun getSentReferralForAuthenticatedUser(referralId: UUID, authentication: JwtAuthenticationToken): Referral {
    val user = userMapper.fromToken(authentication)
    return referralService.getSentReferralForUser(referralId, user)
      ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "sent referral not found [id=$referralId]")
  }

  fun amendExpectedProbationOffice(
    referralId: UUID,
    amendProbationOfficeDTO: AmendProbationOfficeDTO,
    authentication: JwtAuthenticationToken,
    user: AuthUser,
  ) {
    val referral = getSentReferralForAuthenticatedUser(referralId, authentication)

    val oldValues = mutableListOf<String>()
    referral.referralLocation?.expectedProbationOffice?.let { oldValues.add(referral.referralLocation?.expectedProbationOffice!!) }

    val newValues = mutableListOf<String>()
    newValues.add(amendProbationOfficeDTO.probationOffice)

    val referralLocation = referral.referralLocation
    referralLocation?.expectedProbationOffice = amendProbationOfficeDTO.probationOffice

    val changelog = Changelog(
      referral.id,
      UUID.randomUUID(),
      AmendTopic.EXPECTED_PROBATION_OFFICE,
      ReferralAmendmentDetails(values = oldValues),
      ReferralAmendmentDetails(values = newValues),
      "",
      OffsetDateTime.now(),
      userMapper.fromToken(authentication),
    )
    changelogRepository.save(changelog)
    referralLocation?.let { referralLocationRepository.save(it) }
    referralEventPublisher.referralProbationOfficeChangedEvent(
      referral,
      oldValues[0],
      newValues[0],
      user,
      amendProbationOfficeDTO.preventEmailNotification,
    )
  }

  fun amendProbationPractitionerProbationOffice(
    referralId: UUID,
    amendProbationOfficeDTO: AmendProbationOfficeDTO,
    authentication: JwtAuthenticationToken,
    user: AuthUser,
  ) {
    val referral = getSentReferralForAuthenticatedUser(referralId, authentication)

    val oldValues = mutableListOf<String>()
    referral.probationPractitionerDetails?.probationOffice?.let { oldValues.add(referral.probationPractitionerDetails?.probationOffice!!) }

    val newValues = mutableListOf<String>()
    newValues.add(amendProbationOfficeDTO.probationOffice)

    val probationPractitionerDetails = referral.probationPractitionerDetails
    probationPractitionerDetails?.probationOffice = amendProbationOfficeDTO.probationOffice

    val changelog = Changelog(
      referral.id,
      UUID.randomUUID(),
      AmendTopic.PROBATION_PRACTITIONER_PROBATION_OFFICE,
      ReferralAmendmentDetails(values = oldValues),
      ReferralAmendmentDetails(values = newValues),
      "",
      OffsetDateTime.now(),
      userMapper.fromToken(authentication),
    )
    changelogRepository.save(changelog)
    probationPractitionerDetails?.let { probationPractitionerDetailsRepository.save(it) }
    referralEventPublisher.referralProbationOfficeChangedEvent(
      referral,
      oldValues[0],
      newValues[0],
      user,
      amendProbationOfficeDTO.preventEmailNotification,
    )
  }

  fun amendProbationPractitionerName(
    referralId: UUID,
    amendProbationPractitionerNameDTO: AmendProbationPractitionerNameDTO,
    authentication: JwtAuthenticationToken,
    user: AuthUser,
  ) {
    val referral = getSentReferralForAuthenticatedUser(referralId, authentication)

    val oldValues = mutableListOf<String>()
    referral.probationPractitionerDetails?.name?.let { oldValues.add(referral.probationPractitionerDetails?.name!!) }

    if (oldValues.isEmpty()) {
      referral.probationPractitionerDetails?.nDeliusName?.let { oldValues.add(referral.probationPractitionerDetails?.nDeliusName!!) }
    }

    val newValues = mutableListOf<String>()
    newValues.add(amendProbationPractitionerNameDTO.ppName)

    val probationPractitionerDetails = referral.probationPractitionerDetails
    probationPractitionerDetails?.name = amendProbationPractitionerNameDTO.ppName

    val changelog = Changelog(
      referral.id,
      UUID.randomUUID(),
      AmendTopic.PROBATION_PRACTITIONER_NAME,
      ReferralAmendmentDetails(values = oldValues),
      ReferralAmendmentDetails(values = newValues),
      "",
      OffsetDateTime.now(),
      userMapper.fromToken(authentication),
    )
    changelogRepository.save(changelog)
    probationPractitionerDetails?.let { probationPractitionerDetailsRepository.save(it) }
    referralEventPublisher.referralProbationPractitionerNameChangedEvent(
      referral,
      newValues.get(0),
      oldValues.get(0),
      user,
    )
  }

  fun amendProbationPractitionerEmail(
    referralId: UUID,
    amendProbationPractitionerEmailDTO: AmendProbationPractitionerEmailDTO,
    authentication: JwtAuthenticationToken,
    user: AuthUser,
  ) {
    val referral = getSentReferralForAuthenticatedUser(referralId, authentication)

    val oldValues = mutableListOf<String>()
    referral.probationPractitionerDetails?.emailAddress?.let { oldValues.add(referral.probationPractitionerDetails?.emailAddress!!) }

    if (oldValues.isEmpty()) {
      referral.probationPractitionerDetails?.nDeliusEmailAddress?.let { oldValues.add(referral.probationPractitionerDetails?.nDeliusEmailAddress!!) }
    }

    val newValues = mutableListOf<String>()
    newValues.add(amendProbationPractitionerEmailDTO.ppEmail)

    val probationPractitionerDetails = referral.probationPractitionerDetails
    probationPractitionerDetails?.emailAddress = amendProbationPractitionerEmailDTO.ppEmail

    val changelog = Changelog(
      referral.id,
      UUID.randomUUID(),
      AmendTopic.PROBATION_PRACTITIONER_EMAIL,
      ReferralAmendmentDetails(values = oldValues),
      ReferralAmendmentDetails(values = newValues),
      "",
      OffsetDateTime.now(),
      userMapper.fromToken(authentication),
    )
    changelogRepository.save(changelog)
    probationPractitionerDetails?.let { probationPractitionerDetailsRepository.save(it) }
    referralEventPublisher.referralProbationPractitionerEmailChangedEvent(
      referral,
      newValues.get(0),
      oldValues.get(0),
      user,
    )
  }

  fun amendProbationPractitionerPhoneNumber(
    referralId: UUID,
    amendProbationPractitionerPhoneNumberDTO: AmendProbationPractitionerPhoneNumberDTO,
    authentication: JwtAuthenticationToken,
    user: AuthUser,
  ) {
    val referral = getSentReferralForAuthenticatedUser(referralId, authentication)

    val oldValues = mutableListOf<String>()
    referral.probationPractitionerDetails?.ppPhoneNumber?.let { oldValues.add(referral.probationPractitionerDetails?.ppPhoneNumber!!) }

    if (oldValues.isEmpty()) {
      referral.probationPractitionerDetails?.nDeliusPPTelephoneNumber?.let { oldValues.add(referral.probationPractitionerDetails?.nDeliusPPTelephoneNumber!!) }
    }

    val newValues = mutableListOf<String>()
    newValues.add(amendProbationPractitionerPhoneNumberDTO.ppPhoneNumber)

    val probationPractitionerDetails = referral.probationPractitionerDetails
    probationPractitionerDetails?.ppPhoneNumber = amendProbationPractitionerPhoneNumberDTO.ppPhoneNumber

    val changelog = Changelog(
      referral.id,
      UUID.randomUUID(),
      AmendTopic.PROBATION_PRACTITIONER_PHONE_NUMBER,
      ReferralAmendmentDetails(values = oldValues),
      ReferralAmendmentDetails(values = newValues),
      "",
      OffsetDateTime.now(),
      userMapper.fromToken(authentication),
    )
    changelogRepository.save(changelog)
    probationPractitionerDetails?.let { probationPractitionerDetailsRepository.save(it) }
    val oldValue = if (oldValues.size == 0) {
      " "
    } else {
      oldValues[0]
    }
    referralEventPublisher.referralProbationPractitionerPhoneNumberChangedEvent(
      referral,
      newValues.get(0),
      oldValue,
      user,
    )
  }

  fun amendProbationPractitionerTeamPhoneNumber(
    referralId: UUID,
    amendProbationPractitionerTeamPhoneNumberDTO: AmendProbationPractitionerTeamPhoneNumberDTO,
    authentication: JwtAuthenticationToken,
    user: AuthUser,
  ) {
    val referral = getSentReferralForAuthenticatedUser(referralId, authentication)

    val oldValues = mutableListOf<String>()
    referral.probationPractitionerDetails?.ppTeamTelephoneNumber?.let { oldValues.add(referral.probationPractitionerDetails?.ppTeamTelephoneNumber!!) }

    if (oldValues.isEmpty()) {
      referral.probationPractitionerDetails?.nDeliusPPTeamTelephoneNumber?.let { oldValues.add(referral.probationPractitionerDetails?.nDeliusPPTeamTelephoneNumber!!) }
    }

    val newValues = mutableListOf<String>()
    newValues.add(amendProbationPractitionerTeamPhoneNumberDTO.ppTeamPhoneNumber)

    val probationPractitionerDetails = referral.probationPractitionerDetails
    probationPractitionerDetails?.ppTeamTelephoneNumber = amendProbationPractitionerTeamPhoneNumberDTO.ppTeamPhoneNumber

    val changelog = Changelog(
      referral.id,
      UUID.randomUUID(),
      AmendTopic.PROBATION_PRACTITIONER_TEAM_PHONE_NUMBER,
      ReferralAmendmentDetails(values = oldValues),
      ReferralAmendmentDetails(values = newValues),
      "",
      OffsetDateTime.now(),
      userMapper.fromToken(authentication),
    )
    changelogRepository.save(changelog)
    probationPractitionerDetails?.let { probationPractitionerDetailsRepository.save(it) }
    val oldValue = if (oldValues.size == 0) {
      " "
    } else {
      oldValues[0]
    }
    referralEventPublisher.referralProbationPractitionerTeamPhoneNumberChangedEvent(
      referral,
      newValues[0],
      oldValue,
      user,
    )
  }

  fun getListOfChangeLogEntries(referral: Referral): List<Changelog> = changelogRepository.findByReferralIdOrderByChangedAtDesc(referral.id)

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
          newValue = generateDescription(changelog.newVal.values),
        )
      }
      AmendTopic.NEEDS_AND_REQUIREMENTS_HAS_ADDITIONAL_RESPONSIBILITIES -> {
        return ChangelogUpdateDTO(
          changelog = changelog,
          oldValue = generateDescription(changelog.oldVal.values),
          newValue = generateDescription(changelog.newVal.values),
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
          newValue = formattedNewCompletionDate,
        )
      }
      AmendTopic.REASON_FOR_REFERRAL -> {
        return ChangelogUpdateDTO(
          changelog = changelog,
          oldValue = changelog.oldVal.values[0],
          newValue = changelog.newVal.values[0],
        )
      }
      AmendTopic.REASON_FOR_REFERRAL_FURTHER_INFORMATION -> {
        return ChangelogUpdateDTO(
          changelog = changelog,
          oldValue = changelog.oldVal.values[0],
          newValue = changelog.newVal.values[0],
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
    update: UpdateReferralDetailsDTO,
  ) {
    val changelog = Changelog(
      referralId,
      UUID.randomUUID(),
      amendTopic,
      oldValue,
      newValue,
      update.reasonForChange,
      OffsetDateTime.now(),
      actor,
    )

    changelogRepository.save(changelog)
  }

  fun logChanges(referralDetails: ReferralDetails, update: UpdateReferralDetailsDTO, actor: AuthUser) {
    if (update.completionDeadline != null) {
      val oldValue = ReferralAmendmentDetails(listOf(referralDetails.completionDeadline.toString()))
      val newValue = ReferralAmendmentDetails(listOf(update.completionDeadline.toString()))
      processChangeLog(AmendTopic.COMPLETION_DATETIME, oldValue, newValue, referralDetails.referralId, actor, update)
    } else if (update.maximumEnforceableDays != null) {
      val oldValue = ReferralAmendmentDetails(listOf(referralDetails.maximumEnforceableDays.toString()))
      val newValue = ReferralAmendmentDetails(listOf(update.maximumEnforceableDays.toString()))
      processChangeLog(AmendTopic.MAXIMUM_ENFORCEABLE_DAYS, oldValue, newValue, referralDetails.referralId, actor, update)
    }
    if (update.reasonForReferral != null) {
      val oldValue = ReferralAmendmentDetails(listOf(referralDetails.reasonForReferral!!))
      val newValue = ReferralAmendmentDetails(listOf(update.reasonForReferral))
      processChangeLog(AmendTopic.REASON_FOR_REFERRAL, oldValue, newValue, referralDetails.referralId, actor, update)
    }
    if (update.reasonForReferralFurtherInformation != null) {
      val currentValue = referralDetails.reasonForReferralFurtherInformation?.let { it }.orEmpty()
      val oldValue = ReferralAmendmentDetails(listOf(currentValue))
      val newValue = ReferralAmendmentDetails(listOf(update.reasonForReferralFurtherInformation))
      processChangeLog(AmendTopic.REASON_FOR_REFERRAL_FURTHER_INFORMATION, oldValue, newValue, referralDetails.referralId, actor, update)
    }
  }
  private fun generateDescription(values: List<String>): String {
    val booleanValue = values.first { it == "true" || it == "false" }
    return if (booleanValue == "true") "Yes-${values.first { it != "true" }}" else "No"
  }

  fun amendAccessibilityNeeds(
    referralId: UUID,
    amendNeedsAndRequirementsDTO: AmendNeedsAndRequirementsDTO,
    authentication: JwtAuthenticationToken,
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
      userMapper.fromToken(authentication),
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
      userMapper.fromToken(authentication),
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
      userMapper.fromToken(authentication),
    )
    changelogRepository.save(changelog)
    val savedReferral = referralRepository.save(referral)
    referralEventPublisher.referralNeedsAndRequirementsChangedEvent(savedReferral)
  }
}
