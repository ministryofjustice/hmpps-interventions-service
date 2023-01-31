package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service

import mu.KotlinLogging
import net.logstash.logback.argument.StructuredArguments
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.web.server.ServerWebInputException
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.authorization.ReferralAccessChecker
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.authorization.ReferralAccessFilter
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.authorization.ServiceUserAccessChecker
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.authorization.UserTypeChecker
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.config.AccessError
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.config.Code
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.config.FieldError
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.config.ValidationError
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.DraftReferralDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.UpdateReferralDetailsDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.ReferralEventPublisher
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AuthUser
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.DraftReferral
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.PersonCurrentLocationType
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Referral
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ReferralDetails
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ReferralLocation
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.SelectedDesiredOutcomesMapping
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ServiceUserData
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.AuthUserRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.DeliverySessionRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.DraftReferralRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.InterventionRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ReferralDetailsRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ReferralLocationRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ReferralRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ServiceCategoryRepository
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID
import javax.transaction.Transactional

@Service
@Transactional
class DraftReferralService(
  val referralRepository: ReferralRepository,
  val draftReferralRepository: DraftReferralRepository,
  val referralAccessFilter: ReferralAccessFilter,
  val referralAccessChecker: ReferralAccessChecker,
  val serviceUserAccessChecker: ServiceUserAccessChecker,
  val authUserRepository: AuthUserRepository,
  val interventionRepository: InterventionRepository,
  val eventPublisher: ReferralEventPublisher,
  val referenceGenerator: ReferralReferenceGenerator,
  val deliverySessionRepository: DeliverySessionRepository,
  val serviceCategoryRepository: ServiceCategoryRepository,
  val userTypeChecker: UserTypeChecker,
  val communityAPIReferralService: CommunityAPIReferralService,
  val assessRisksAndNeedsService: RisksAndNeedsService,
  val supplierAssessmentService: SupplierAssessmentService,
  val hmppsAuthService: HMPPSAuthService,
  val referralDetailsRepository: ReferralDetailsRepository,
  val draftOasysRiskInformationService: DraftOasysRiskInformationService,
  val referralLocationRepository: ReferralLocationRepository,
  @Value("\${feature-flags.current-location.enabled}") private val currentLocationEnabled: Boolean
) {
  companion object {
    private val logger = KotlinLogging.logger {}
    private const val maxReferenceNumberTries = 10
  }

  fun createDraftReferral(
    user: AuthUser,
    crn: String,
    interventionId: UUID,
    overrideID: UUID? = null,
    overrideCreatedAt: OffsetDateTime? = null,
  ): DraftReferral {
    if (!userTypeChecker.isProbationPractitionerUser(user)) {
      throw AccessError(user, "user cannot create referral", listOf("only probation practitioners can create draft referrals"))
    }

    // PPs can't create referrals for service users they are not allowed to see
    serviceUserAccessChecker.forProbationPractitionerUser(crn, user)

    val intervention = interventionRepository.getById(interventionId)
    val serviceCategories = intervention.dynamicFrameworkContract.contractType.serviceCategories
    val selectedServiceCategories = if (serviceCategories.size == 1) serviceCategories.toMutableSet() else null

    return draftReferralRepository.save(
      DraftReferral(
        id = overrideID ?: UUID.randomUUID(),
        createdAt = overrideCreatedAt ?: OffsetDateTime.now(),
        createdBy = authUserRepository.save(user),
        serviceUserCRN = crn,
        intervention = interventionRepository.getById(interventionId),
        selectedServiceCategories = selectedServiceCategories,
      )
    )
  }

  fun updateDraftReferral(referral: DraftReferral, update: DraftReferralDTO): DraftReferral {
    validateDraftReferralUpdate(referral, update)

    updateDraftReferralDetails(
      referral,
      UpdateReferralDetailsDTO(
        update.maximumEnforceableDays,
        update.completionDeadline,
        update.furtherInformation,
        update.expectedReleaseDate,
        update.expectedReleaseDateMissingReason,
        "initial referral details",
      ),
      referral.createdBy,
    )
    updateServiceUserDetails(referral, update)
    updateServiceUserNeeds(referral, update)
    updateDraftRiskInformation(referral, update)
    updateServiceCategoryDetails(referral, update)
    if (currentLocationEnabled) {
      updatePersonCurrentLocation(referral, update)
      updatePersonExpectedReleaseDate(referral, update)
    }

    // this field doesn't fit into any other categories - is this a smell?
    update.relevantSentenceId?.let {
      referral.relevantSentenceId = it
    }

    return draftReferralRepository.save(referral)
  }

  private fun updateServiceCategoryDetails(referral: DraftReferral, update: DraftReferralDTO) {
    // The selected complexity levels and desired outcomes need to be removed if the user has unselected service categories that are linked to them
    update.serviceCategoryIds?.let { serviceCategoryIds ->
      referral.complexityLevelIds =
        referral.complexityLevelIds?.filterKeys { serviceCategoryId -> serviceCategoryIds.contains(serviceCategoryId) }
          ?.toMutableMap()
      referral.selectedDesiredOutcomes =
        referral.selectedDesiredOutcomes?.filter { desiredOutcome -> serviceCategoryIds.contains(desiredOutcome.serviceCategoryId) }
          ?.toMutableList()
    }

    // Need to save and flush the entity before removing selected service categories due to constraint violations being thrown from selected complexity levels and desired outcomes
    draftReferralRepository.saveAndFlush(referral)

    update.serviceCategoryIds?.let { serviceCategoryIds ->
      val updatedServiceCategories = serviceCategoryRepository.findByIdIn(serviceCategoryIds)
      // the following code looks like a strange way to do this ...
      // however we have to be very intentional about updating the service categories,
      // rather than replacing the collection entirely. in the former case, hibernate
      // runs an update as you would expect. in the latter case, hibernate deletes the
      // entity entirely and creates a new one. this causes SQL constraint violations
      // because these service category ids are referenced in the selected complexity
      // level and desired outcomes tables.
      referral.selectedServiceCategories?.let {
        it.removeIf { true }
        it.addAll(updatedServiceCategories)
      } ?: run {
        referral.selectedServiceCategories = updatedServiceCategories.toMutableSet()
      }
    }
  }

  fun updateDraftReferralDetails(referral: DraftReferral, update: UpdateReferralDetailsDTO, actor: AuthUser): ReferralDetails? {
    if (!update.isValidUpdate) {
      return null
    }

    val existingDetails = referralDetailsRepository.findLatestByReferralId(referral.id)

    // if we are updating a draft referral, and there is already a ReferralDetails record,
    // we update it. otherwise, we create a new one to record the changes.
    val newDetails = existingDetails
      ?: ReferralDetails(
        UUID.randomUUID(),
        null,
        referral.id,
        referral.createdAt,
        authUserRepository.save(actor).id,
        update.reasonForChange,
      )

    update.completionDeadline?.let {
      newDetails.completionDeadline = it
    }

    update.furtherInformation?.let {
      newDetails.furtherInformation = it
    }

    update.maximumEnforceableDays?.let {
      newDetails.maximumEnforceableDays = it
    }

    referralDetailsRepository.saveAndFlush(newDetails)

    return newDetails
  }

  fun updatePersonCurrentLocation(draftReferral: DraftReferral, update: DraftReferralDTO) {
    update.personCurrentLocationType?.let {
      draftReferral.personCurrentLocationType = it
      draftReferral.personCustodyPrisonId = if (it == PersonCurrentLocationType.CUSTODY) update.personCustodyPrisonId else null
    }
  }

  fun updatePersonExpectedReleaseDate(draftReferral: DraftReferral, update: DraftReferralDTO) {
    draftReferral.expectedReleaseDate = update.expectedReleaseDate
    draftReferral.expectedReleaseDateMissingReason = update.expectedReleaseDateMissingReason
  }

  private fun updateServiceUserNeeds(draftReferral: DraftReferral, update: DraftReferralDTO) {
    update.additionalNeedsInformation?.let {
      draftReferral.additionalNeedsInformation = it
    }

    update.accessibilityNeeds?.let {
      draftReferral.accessibilityNeeds = it
    }

    update.needsInterpreter?.let {
      draftReferral.needsInterpreter = it
      draftReferral.interpreterLanguage = if (it) update.interpreterLanguage else null
    }

    update.hasAdditionalResponsibilities?.let {
      draftReferral.hasAdditionalResponsibilities = it
      draftReferral.whenUnavailable = if (it) update.whenUnavailable else null
    }
  }

  private fun updateDraftRiskInformation(draftReferral: DraftReferral, update: DraftReferralDTO) {
    update.additionalRiskInformation?.let {
      draftReferral.additionalRiskInformation = it
      draftReferral.additionalRiskInformationUpdatedAt = OffsetDateTime.now()
    }
  }

  private fun updateServiceUserDetails(draftReferral: DraftReferral, update: DraftReferralDTO) {
    update.serviceUser?.let {
      draftReferral.serviceUserData = ServiceUserData(
        title = it.title,
        firstName = it.firstName,
        lastName = it.lastName,
        dateOfBirth = it.dateOfBirth,
        gender = it.gender,
        ethnicity = it.ethnicity,
        draftReferral = draftReferral,
        preferredLanguage = it.preferredLanguage,
        religionOrBelief = it.religionOrBelief,
        disabilities = it.disabilities,
      )
    }
  }

  fun updateDraftReferralComplexityLevel(referral: DraftReferral, serviceCategoryId: UUID, complexityLevelId: UUID): DraftReferral {
    if (referral.selectedServiceCategories.isNullOrEmpty()) {
      throw ServerWebInputException("complexity level cannot be updated: no service categories selected for this referral")
    }

    if (!referral.selectedServiceCategories!!.map { it.id }.contains(serviceCategoryId)) {
      throw ServerWebInputException("complexity level cannot be updated: specified service category not selected for this referral")
    }

    val validComplexityLevelIds = serviceCategoryRepository.findByIdOrNull(serviceCategoryId)?.complexityLevels?.map { it.id }
      ?: throw ServerWebInputException("complexity level cannot be updated: specified service category not found")

    if (!validComplexityLevelIds.contains(complexityLevelId)) {
      throw ServerWebInputException("complexity level cannot be updated: complexity level not valid for this service category")
    }

    if (referral.complexityLevelIds == null) {
      referral.complexityLevelIds = mutableMapOf()
    }

    referral.complexityLevelIds!![serviceCategoryId] = complexityLevelId
    return draftReferralRepository.save(referral)
  }

  fun updateDraftReferralDesiredOutcomes(referral: DraftReferral, serviceCategoryId: UUID, desiredOutcomeIds: List<UUID>): DraftReferral {
    if (desiredOutcomeIds.isEmpty()) {
      throw ServerWebInputException("desired outcomes cannot be empty")
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

    referral.selectedDesiredOutcomes!!.removeIf { desiredOutcome -> desiredOutcome.serviceCategoryId == serviceCategoryId }
    desiredOutcomeIds.forEach { desiredOutcomeId ->
      referral.selectedDesiredOutcomes!!.add(
        SelectedDesiredOutcomesMapping(serviceCategoryId, desiredOutcomeId)
      )
    }
    return draftReferralRepository.save(referral)
  }

  fun getDraftReferralForUser(id: UUID, user: AuthUser): DraftReferral? {
    if (!userTypeChecker.isProbationPractitionerUser(user)) {
      throw AccessError(user, "unsupported user type", listOf("only probation practitioners can access draft referrals"))
    }

    val referral = draftReferralRepository.findById(id)
    referral.ifPresent {
      referralAccessChecker.forUser(it, user)
    }
    return referral.map { it }.orElse(null)
  }

  fun getDraftReferralsForUser(user: AuthUser): List<DraftReferral> {
    if (!userTypeChecker.isProbationPractitionerUser(user)) {
      throw AccessError(user, "user does not have access to referrals", listOf("only probation practitioners can access draft referrals"))
    }

    val referrals = draftReferralRepository.findByCreatedById(user.id)
    return referralAccessFilter.probationPractitionerReferrals(referrals, user)
  }

  private fun validateDraftReferralUpdate(draftReferral: DraftReferral, update: DraftReferralDTO) {
    val errors = mutableListOf<FieldError>()

    if (currentLocationEnabled) {
      update.personCurrentLocationType?.let {
        if (it == PersonCurrentLocationType.CUSTODY && update.personCustodyPrisonId == null) {
          errors.add(FieldError(field = "personCustodyPrisonId", error = Code.CONDITIONAL_FIELD_MUST_BE_SET))
        }
      }
    }

    update.completionDeadline?.let {
      if (it.isBefore(LocalDate.now())) {
        errors.add(FieldError(field = "completionDeadline", error = Code.DATE_MUST_BE_IN_THE_FUTURE))
      }

      // fixme: error if completion deadline is after sentence end date
    }

    update.needsInterpreter?.let {
      if (it && update.interpreterLanguage == null) {
        errors.add(FieldError(field = "needsInterpreter", error = Code.CONDITIONAL_FIELD_MUST_BE_SET))
      }
    }

    update.hasAdditionalResponsibilities?.let {
      if (it && update.whenUnavailable == null) {
        errors.add(FieldError(field = "hasAdditionalResponsibilities", error = Code.CONDITIONAL_FIELD_MUST_BE_SET))
      }
    }

    update.serviceUser?.let {
      if (it.crn != draftReferral.serviceUserCRN) {
        errors.add(FieldError(field = "serviceUser.crn", error = Code.FIELD_CANNOT_BE_CHANGED))
      }
    }

    update.serviceCategoryIds?.let {
      if (!draftReferral.intervention.dynamicFrameworkContract.contractType.serviceCategories.map {
        serviceCategory ->
        serviceCategory.id
      }.containsAll(it)
      ) {
        errors.add(FieldError(field = "serviceCategoryIds", error = Code.INVALID_SERVICE_CATEGORY_FOR_CONTRACT))
      }
    }

    if (errors.isNotEmpty()) {
      throw ValidationError("draft referral update invalid", errors)
    }
  }

  fun sendDraftReferral(draftReferral: DraftReferral, user: AuthUser): Referral {
    val referral = createSentReferral(draftReferral, user)

    /*
     * This is a temporary solution until a robust asynchronous link is created between Interventions
     * and Delius/ARN. Once the asynchronous link is implemented, this feature can be turned off, and instead
     * Delius/ARN will be notified via the normal asynchronous route
     *
     * Order is important: ARN is more likely to fail via a timeout and thus when repeated by the user a
     * duplicate NSI gets created in Delius as the Delius call is not idempotent. This order avoids creating
     * duplicate NSIs in nDelius on user retry.
     */
    submitAdditionalRiskInformation(referral, user)
    communityAPIReferralService.send(referral)

    val sentReferral = referralRepository.save(referral)
    createReferralLocation(draftReferral, sentReferral)
    eventPublisher.referralSentEvent(sentReferral)
    supplierAssessmentService.createSupplierAssessment(referral)
    return sentReferral
  }

  private fun createSentReferral(draftReferral: DraftReferral, sentByUser: AuthUser): Referral {
    val sentBy = authUserRepository.save(sentByUser)
    return Referral(
      id = draftReferral.id,
      furtherInformation = draftReferral.furtherInformation,
      additionalNeedsInformation = draftReferral.additionalNeedsInformation,
      additionalRiskInformation = draftReferral.additionalRiskInformation,
      additionalRiskInformationUpdatedAt = draftReferral.additionalRiskInformationUpdatedAt,
      accessibilityNeeds = draftReferral.accessibilityNeeds,
      needsInterpreter = draftReferral.needsInterpreter,
      interpreterLanguage = draftReferral.interpreterLanguage,
      hasAdditionalResponsibilities = draftReferral.hasAdditionalResponsibilities,
      whenUnavailable = draftReferral.whenUnavailable,
      selectedDesiredOutcomes = draftReferral.selectedDesiredOutcomes,
      relevantSentenceId = draftReferral.relevantSentenceId,
      intervention = draftReferral.intervention,
      serviceUserCRN = draftReferral.serviceUserCRN,
      createdBy = draftReferral.createdBy,
      createdAt = draftReferral.createdAt,
      referralDetailsHistory = draftReferral.referralDetailsHistory,
      referenceNumber = generateReferenceNumber(draftReferral),
      sentAt = OffsetDateTime.now(),
      sentBy = sentBy,
    )
  }

  private fun createReferralLocation(draftReferral: DraftReferral, referral: Referral) {
    if (currentLocationEnabled) {
      referral.referralLocation = referralLocationRepository.save(
        ReferralLocation(
          id = UUID.randomUUID(),
          referral = referral,
          type = draftReferral.personCurrentLocationType
            ?: throw ServerWebInputException("can't submit a referral without current location"),
          prisonId = draftReferral.personCustodyPrisonId
        )
      )
      referralRepository.save(referral)
    }
  }

  private fun submitAdditionalRiskInformation(referral: Referral, user: AuthUser) {
    // todo: throw exception once posting full risk to ARN feature is enabled, however allow some time for in-flight
    //  make-referral requests to pass

    val oasysRiskInformation = draftOasysRiskInformationService.getDraftOasysRiskInformation(referral.id)
    val riskId = if (oasysRiskInformation != null) {
      assessRisksAndNeedsService.createSupplementaryRisk(
        referral.id,
        referral.serviceUserCRN,
        user,
        oasysRiskInformation.updatedAt,
        oasysRiskInformation.additionalInformation ?: "",
        RedactedRisk(
          riskWho = oasysRiskInformation.riskSummaryWhoIsAtRisk ?: "",
          riskWhen = oasysRiskInformation.riskSummaryRiskImminence ?: "",
          riskNature = oasysRiskInformation.riskSummaryNatureOfRisk ?: "",
          concernsSelfHarm = oasysRiskInformation.riskToSelfSelfHarm ?: "",
          concernsSuicide = oasysRiskInformation.riskToSelfSuicide ?: "",
          concernsHostel = oasysRiskInformation.riskToSelfHostelSetting ?: "",
          concernsVulnerability = oasysRiskInformation.riskToSelfVulnerability ?: "",
        )
      )
    } else {
      val additionalRiskInformation = referral.additionalRiskInformation
        ?: throw ServerWebInputException("can't submit a referral without risk information")

      val riskSubmittedAt = referral.additionalRiskInformationUpdatedAt!!

      assessRisksAndNeedsService.createSupplementaryRisk(
        referral.id,
        referral.serviceUserCRN,
        user,
        riskSubmittedAt,
        additionalRiskInformation,
        null
      )
    }

    referral.supplementaryRiskId = riskId
    // we do not store _any_ risk information in interventions once it has been sent to ARN
    referral.additionalRiskInformation = null
    draftOasysRiskInformationService.deleteDraftOasysRiskInformation(referral.id)
  }

  private fun generateReferenceNumber(draftReferral: DraftReferral): String? {
    val type = draftReferral.intervention.dynamicFrameworkContract.contractType.name

    for (i in 1..maxReferenceNumberTries) {
      val candidate = referenceGenerator.generate(type)
      if (!referralRepository.existsByReferenceNumber(candidate))
        return candidate
      else
        logger.warn(
          "Clash found for referral number {}",
          StructuredArguments.kv("candidate", candidate)
        )
    }

    logger.error(
      "Unable to generate a referral number {} {}",
      StructuredArguments.kv("tries", maxReferenceNumberTries),
      StructuredArguments.kv("referral_id", draftReferral.id)
    )
    return null
  }
}
