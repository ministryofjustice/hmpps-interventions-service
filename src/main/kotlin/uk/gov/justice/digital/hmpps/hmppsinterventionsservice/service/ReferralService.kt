package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service

import mu.KotlinLogging
import net.logstash.logback.argument.StructuredArguments.kv
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.domain.Specification.not
import org.springframework.data.jpa.domain.Specification.where
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.server.ServerWebInputException
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.authorization.ReferralAccessChecker
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.authorization.ReferralAccessFilter
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.authorization.ServiceProviderAccessScopeMapper
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.authorization.ServiceUserAccessChecker
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.authorization.UserTypeChecker
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.config.AccessError
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.config.Code
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.config.FieldError
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.config.ValidationError
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.DashboardType
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.DraftReferralDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.ReferralAmendmentDetails
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.UpdateReferralDetailsDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.ReferralEventPublisher
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AuthUser
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.CancellationReason
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Changelog
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.DraftReferral
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Referral
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ReferralAssignment
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ReferralDetails
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.SelectedDesiredOutcomesMapping
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.SentReferralSummary
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ServiceProviderSentReferralSummary
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ServiceUserData
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.AuthUserRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.CancellationReasonRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ChangelogRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.DeliverySessionRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.DraftReferralRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.InterventionRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ReferralDetailsRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ReferralRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.SentReferralSummariesRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ServiceCategoryRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.specification.ReferralSpecifications
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID
import javax.transaction.Transactional

data class ResponsibleProbationPractitioner(
  override val firstName: String,
  override val email: String,
  val deliusStaffId: Long?,
  val authUser: AuthUser?,
  override val lastName: String,
) : ContactablePerson

@Service
@Transactional
class ReferralService(
  val referralRepository: ReferralRepository,
  val draftReferralRepository: DraftReferralRepository,
  val sentReferralSummariesRepository: SentReferralSummariesRepository,
  val authUserRepository: AuthUserRepository,
  val interventionRepository: InterventionRepository,
  val referralConcluder: ReferralConcluder,
  val eventPublisher: ReferralEventPublisher,
  val referenceGenerator: ReferralReferenceGenerator,
  val cancellationReasonRepository: CancellationReasonRepository,
  val deliverySessionRepository: DeliverySessionRepository,
  val serviceCategoryRepository: ServiceCategoryRepository,
  val referralAccessChecker: ReferralAccessChecker,
  val userTypeChecker: UserTypeChecker,
  val serviceProviderUserAccessScopeMapper: ServiceProviderAccessScopeMapper,
  val referralAccessFilter: ReferralAccessFilter,
  val communityAPIReferralService: CommunityAPIReferralService,
  val serviceUserAccessChecker: ServiceUserAccessChecker,
  val assessRisksAndNeedsService: RisksAndNeedsService,
  val communityAPIOffenderService: CommunityAPIOffenderService,
  val supplierAssessmentService: SupplierAssessmentService,
  val hmppsAuthService: HMPPSAuthService,
  val telemetryService: TelemetryService,
  val draftOasysRiskInformationService: DraftOasysRiskInformationService,
  val referralDetailsRepository: ReferralDetailsRepository,
  val changelogRepository: ChangelogRepository,
) {
  companion object {
    private val logger = KotlinLogging.logger {}
    private const val maxReferenceNumberTries = 10
  }

  fun getSentReferralForUser(id: UUID, user: AuthUser): Referral? {
    val referral = referralRepository.findByIdAndSentAtIsNotNull(id)

    referral?.let {
      referralAccessChecker.forUser(it, user)
    }

    return referral
  }

  // This is required for Client API access where the authority to access ALL referrals is pre-checked in the controller
  fun getSentReferral(id: UUID): Referral? {
    return referralRepository.findByIdAndSentAtIsNotNull(id)
  }
  fun getReferralDetailsById(id: UUID?): ReferralDetails? {
    return referralDetailsRepository.findByIdOrNull(id)
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

  fun assignSentReferral(referral: Referral, assignedBy: AuthUser, assignedTo: AuthUser): Referral {
    val assignment = ReferralAssignment(
      OffsetDateTime.now(),
      authUserRepository.save(assignedBy),
      authUserRepository.save(assignedTo)
    )
    referral.assignments.forEach { it.superseded = true }
    referral.assignments.add(assignment)

    val assignedReferral = referralRepository.save(referral)
    eventPublisher.referralAssignedEvent(assignedReferral)
    return assignedReferral
  }

  fun getSentReferralSummaryForUser(user: AuthUser, concluded: Boolean?, cancelled: Boolean?, unassigned: Boolean?, assignedToUserId: String?, page: Pageable, searchText: String? = null): Iterable<SentReferralSummary> {
    val findSentReferralsSpec: Specification<SentReferralSummary> = createSpecification(concluded, cancelled, unassigned, assignedToUserId, searchText)

    if (userTypeChecker.isServiceProviderUser(user)) {
      return getSentReferralSummaryForServiceProviderUser(user, findSentReferralsSpec, page)
    }

    if (userTypeChecker.isProbationPractitionerUser(user)) {
      return getSentReferralSummaryForProbationPractitionerUser(user, findSentReferralsSpec, page)
    }

    throw AccessError(user, "unsupported user type", listOf("logins from ${user.authSource} are not supported"))
  }

  fun getServiceProviderSummaries(user: AuthUser, dashboardType: DashboardType? = null): List<ServiceProviderSentReferralSummary> {
    if (userTypeChecker.isServiceProviderUser(user)) {
      return getSentReferralSummariesForServiceProviderUser(user, dashboardType)
    }
    throw AccessError(user, "unsupported user type", listOf("logins from ${user.authSource} are not supported"))
  }

  private fun <T> applyOptionalConjunction(existingSpec: Specification<T>, predicate: Boolean?, specToJoin: Specification<T>): Specification<T> {
    if (predicate == null) return existingSpec
    return existingSpec.and(if (predicate) specToJoin else not(specToJoin))
  }

  private fun getSentReferralSummaryForServiceProviderUser(user: AuthUser, sentReferralFilterSpecification: Specification<SentReferralSummary>, page: Pageable): Iterable<SentReferralSummary> {
    // todo: query for referrals where the service provider has been granted nominated access only
    val filteredSpec = referralAccessFilter.serviceProviderReferrals(sentReferralFilterSpecification, user)
    return sentReferralSummariesRepository.findAll(filteredSpec, page)
  }

  private fun getSentReferralSummariesForServiceProviderUser(user: AuthUser, dashboardType: DashboardType?): List<ServiceProviderSentReferralSummary> {
    val serviceProviders = serviceProviderUserAccessScopeMapper.fromUser(user).serviceProviders
    val referralSummaries = referralRepository.getSentReferralSummaries(user, serviceProviders = serviceProviders.map { serviceProvider -> serviceProvider.id }, dashboardType)
    return referralAccessFilter.serviceProviderReferralSummaries(referralSummaries, user)
  }

  private fun getSentReferralSummaryForProbationPractitionerUser(user: AuthUser, sentReferralFilterSpecification: Specification<SentReferralSummary>, page: Pageable): Iterable<SentReferralSummary> {
    val filteredSpec = createSpecificationForProbationPractitionerUser(user, sentReferralFilterSpecification)
    return sentReferralSummariesRepository.findAll(filteredSpec, page)
  }

  private inline fun <reified T> createSpecification(
    concluded: Boolean?,
    cancelled: Boolean?,
    unassigned: Boolean?,
    assignedToUserId: String?,
    searchText: String?,
  ): Specification<T> {
    var findSentReferralsSpec = ReferralSpecifications.sent<T>()

    findSentReferralsSpec = applyOptionalConjunction(findSentReferralsSpec, concluded, ReferralSpecifications.concluded())
    findSentReferralsSpec = applyOptionalConjunction(findSentReferralsSpec, cancelled, ReferralSpecifications.cancelled())
    findSentReferralsSpec = applyOptionalConjunction(findSentReferralsSpec, unassigned, ReferralSpecifications.unassigned())
    assignedToUserId?.let {
      findSentReferralsSpec = applyOptionalConjunction(findSentReferralsSpec, true, ReferralSpecifications.currentlyAssignedTo(it))
    }
    searchText?.let {
      findSentReferralsSpec = applyOptionalConjunction(findSentReferralsSpec, true, searchSpec(searchText))
    }
    return findSentReferralsSpec
  }

  private fun <T> searchSpec(searchText: String): Specification<T> {
    return if (searchText.matches(Regex("[A-Z]{2}[0-9]{4}[A-Z]{2}"))) ReferralSpecifications.searchByReferenceNumber(searchText)
    else ReferralSpecifications.searchByPoPName(searchText)
  }

  private fun <T> createSpecificationForProbationPractitionerUser(
    user: AuthUser,
    sentReferralFilterSpecification: Specification<T>
  ): Specification<T> {
    var referralsForPPUser = ReferralSpecifications.createdBy<T>(user)
    try {
      val serviceUserCRNs = communityAPIOffenderService.getManagedOffendersForDeliusUser(user).map { it.crnNumber }
      referralsForPPUser = referralsForPPUser.or(ReferralSpecifications.matchingServiceUserReferrals(serviceUserCRNs))
    } catch (e: WebClientResponseException) {
      // don't stop users seeing their own referrals just because delius is not playing nice
      logger.error(
        "failed to get managed offenders for user {}",
        e,
        kv("username", user.userName),
      )
    }

    // todo: filter out referrals for limited access offenders (LAOs)
    val referralSpecification = where(referralsForPPUser).and(sentReferralFilterSpecification)
    return referralAccessFilter.probationPractitionerReferrals(referralSpecification, user)
  }

  fun requestReferralEnd(referral: Referral, user: AuthUser, reason: CancellationReason, comments: String?): Referral {
    val now = OffsetDateTime.now()
    referral.endRequestedAt = now
    referral.endRequestedBy = authUserRepository.save(user)
    referral.endRequestedReason = reason
    comments?.let { referral.endRequestedComments = it }

    val savedReferral = referralRepository.save(referral)
    referralConcluder.concludeIfEligible(referral)

    return savedReferral
  }

  fun sendDraftReferral(draftReferral: DraftReferral, user: AuthUser): Referral {
    val referral = createReferral(draftReferral)
    referral.sentAt = OffsetDateTime.now()
    referral.sentBy = authUserRepository.save(user)

    referral.referenceNumber = generateReferenceNumber(draftReferral)

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
    eventPublisher.referralSentEvent(sentReferral)
    supplierAssessmentService.createSupplierAssessment(referral)
    return sentReferral
  }

  private fun createReferral(draftReferral: DraftReferral): Referral {
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
      maximumEnforceableDays = draftReferral.maximumEnforceableDays,
      selectedDesiredOutcomes = draftReferral.selectedDesiredOutcomes,
      completionDeadline = draftReferral.completionDeadline,
      relevantSentenceId = draftReferral.relevantSentenceId,
      intervention = draftReferral.intervention,
      serviceUserCRN = draftReferral.serviceUserCRN,
      createdBy = draftReferral.createdBy,
      createdAt = draftReferral.createdAt,
      referralDetailsHistory = draftReferral.referralDetailsHistory,
    )
  }

  private fun submitAdditionalRiskInformation(referral: Referral, user: AuthUser) {
    // todo: throw exception once posting full risk to ARN feature is enabled, however allow some time for in-flight
    //  make-referral requests to pass

    val oasysRiskInformation = draftOasysRiskInformationService.getDraftOasysRiskInformation(referral.id)
    val riskId = if (oasysRiskInformation != null && assessRisksAndNeedsService.canPostFullRiskInformation()) {
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

      val riskSubmittedAt = referral.additionalRiskInformationUpdatedAt
        ?: run {
          // this should NEVER happen. i'm not really sure why we have this as a fallback.
          logger.warn("no additionalRiskInformationUpdatedAt on referral; setting to current time")
          OffsetDateTime.now()
        }

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

  private fun validateDraftReferralUpdate(draftReferral: DraftReferral, update: DraftReferralDTO) {
    val errors = mutableListOf<FieldError>()

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

  @Deprecated(
    """
    currently we are duplicating these fields in both the referral 
    and referral_details tables. once we solely rely on the latter, 
    we can remove this method entirely.
  """
  )
  private fun legacyUpdateReferralDetails(draftReferral: DraftReferral, update: DraftReferralDTO) {
    update.completionDeadline?.let {
      draftReferral.completionDeadline = it
    }

    update.furtherInformation?.let {
      draftReferral.furtherInformation = it
    }

    update.maximumEnforceableDays?.let {
      draftReferral.maximumEnforceableDays = it
    }
  }

  fun updateReferralDetails(referral: Referral, update: UpdateReferralDetailsDTO, actor: AuthUser): ReferralDetails? {
    if (!update.isValidUpdate) {
      return null
    }
    if (update.completionDeadline != null) {
      handleChangLog(AmendTopic.COMPLETION_DATETIME, referral, update, actor)
    } else {
      handleChangLog(AmendTopic.MAXIMUM_ENFORCEABLE_DAYS, referral, update, actor)
    }

    val isDraftUpdate = referral.sentAt == null
    val existingDetails = referralDetailsRepository.findLatestByReferralId(referral.id)

    // we do not create fully fledged updates for draft referrals. to avoid misleading
    // created_at timestamps we always ensure draft referral details were 'created at'
    // the same time as the draft referral itself.
    val updateTime = if (isDraftUpdate) referral.createdAt else OffsetDateTime.now()

    // if we are updating a draft referral, and there is already a ReferralDetails record,
    // we update it. otherwise, we create a new one to record the changes.
    val newDetails = if (isDraftUpdate && existingDetails != null) existingDetails else
      ReferralDetails(
        UUID.randomUUID(),
        null,
        referral.id,
        updateTime,
        actor.id,
        update.reasonForChange,
        existingDetails?.completionDeadline,
        existingDetails?.furtherInformation,
        existingDetails?.maximumEnforceableDays,
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

    if (existingDetails !== null && existingDetails !== newDetails) {
      existingDetails.supersededById = newDetails.id
      referralDetailsRepository.saveAndFlush(existingDetails)

      eventPublisher.referralDetailsChangedEvent(referral, newDetails, existingDetails)
    }

    return newDetails
  }
  private fun proccessChangeLog(ammendTopic: AmendTopic, oldValue: ReferralAmendmentDetails, newValue: ReferralAmendmentDetails, referralId: UUID, actor: AuthUser, update: UpdateReferralDetailsDTO) {
    val changelog = Changelog(
      referralId,
      UUID.randomUUID(),
      ammendTopic,
      oldValue,
      newValue,
      update.reasonForChange,
      OffsetDateTime.now(),
      actor
    )

    changelogRepository.save(changelog)
  }

  private fun handleChangLog(topic: AmendTopic, referral: Referral, update: UpdateReferralDetailsDTO, actor: AuthUser) {
    when (topic) {
      AmendTopic.COMPLETION_DATETIME -> {
        val completionDeadline = referral?.completionDeadline
        val oldValue = ReferralAmendmentDetails(listOf(completionDeadline.toString()))
        val newValue = ReferralAmendmentDetails(listOf(update.completionDeadline.toString()))
        proccessChangeLog(AmendTopic.COMPLETION_DATETIME, oldValue, newValue, referral.id, actor, update)
      }
      AmendTopic.MAXIMUM_ENFORCEABLE_DAYS -> {
        val maximumEnforceableDays = referral?.maximumEnforceableDays
        val oldValue = ReferralAmendmentDetails(listOf(maximumEnforceableDays.toString()))
        val newValue = ReferralAmendmentDetails(listOf(update.maximumEnforceableDays.toString()))
        proccessChangeLog(AmendTopic.MAXIMUM_ENFORCEABLE_DAYS, oldValue, newValue, referral.id, actor, update)
      }
      AmendTopic.COMPLEXITY_LEVEL -> return
      AmendTopic.DESIRED_OUTCOMES -> return
      AmendTopic.NEEDS_AND_REQUIREMENTS_ACCESSIBILITY_NEEDS -> return
      AmendTopic.NEEDS_AND_REQUIREMENTS_ADDITIONAL_INFORMATION -> return
      AmendTopic.NEEDS_AND_REQUIREMENTS_HAS_ADDITIONAL_RESPONSIBILITIES -> return
      AmendTopic.NEEDS_AND_REQUIREMENTS_INTERPRETER_REQUIRED -> return
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
        actor.id,
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

  fun updateDraftReferral(referral: DraftReferral, update: DraftReferralDTO): DraftReferral {
    validateDraftReferralUpdate(referral, update)

    legacyUpdateReferralDetails(referral, update)
    updateDraftReferralDetails(
      referral,
      UpdateReferralDetailsDTO(
        update.maximumEnforceableDays,
        update.completionDeadline,
        update.furtherInformation,
        "initial referral details",
      ),
      referral.createdBy,
    )
    updateServiceUserDetails(referral, update)
    updateServiceUserNeeds(referral, update)
    updateDraftRiskInformation(referral, update)
    updateServiceCategoryDetails(referral, update)

    // this field doesn't fit into any other categories - is this a smell?
    update.relevantSentenceId?.let {
      referral.relevantSentenceId = it
    }

    return draftReferralRepository.save(referral)
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
    desiredOutcomeIds.forEach { desiredOutcomeId -> referral.selectedDesiredOutcomes!!.add(SelectedDesiredOutcomesMapping(serviceCategoryId, desiredOutcomeId)) }
    return draftReferralRepository.save(referral)
  }

  fun getDraftReferralsForUser(user: AuthUser): List<DraftReferral> {
    if (!userTypeChecker.isProbationPractitionerUser(user)) {
      throw AccessError(user, "user does not have access to referrals", listOf("only probation practitioners can access draft referrals"))
    }

    val referrals = draftReferralRepository.findByCreatedById(user.id)
    return referralAccessFilter.probationPractitionerReferrals(referrals, user)
  }

  fun getCancellationReasons(): List<CancellationReason> {
    return cancellationReasonRepository.findAll()
  }

  private fun generateReferenceNumber(draftReferral: DraftReferral): String? {
    val type = draftReferral.intervention.dynamicFrameworkContract.contractType.name

    for (i in 1..maxReferenceNumberTries) {
      val candidate = referenceGenerator.generate(type)
      if (!referralRepository.existsByReferenceNumber(candidate))
        return candidate
      else
        logger.warn("Clash found for referral number {}", kv("candidate", candidate))
    }

    logger.error("Unable to generate a referral number {} {}", kv("tries", maxReferenceNumberTries), kv("referral_id", draftReferral.id))
    return null
  }

  fun getResponsibleProbationPractitioner(referral: Referral): ResponsibleProbationPractitioner {
    val sentBy = referral.sentBy?.let { it } ?: null
    return getResponsibleProbationPractitioner(referral.serviceUserCRN, sentBy, referral.createdBy)
  }

  fun getResponsibleProbationPractitioner(crn: String, sentBy: AuthUser?, createdBy: AuthUser): ResponsibleProbationPractitioner {
    try {
      val responsibleOfficer = communityAPIOffenderService.getResponsibleOfficer(crn)
      if (responsibleOfficer.email != null) {
        return ResponsibleProbationPractitioner(
          responsibleOfficer.firstName ?: "",
          responsibleOfficer.email,
          responsibleOfficer.staffId,
          null,
          responsibleOfficer.lastName ?: "",
        )
      }

      telemetryService.reportInvalidAssumption(
        "all responsible officers have email addresses",
        mapOf("staffId" to responsibleOfficer.staffId.toString())
      )

      logger.warn("no email address for responsible officer; falling back to referring probation practitioner")
    } catch (e: Exception) {
      logger.error(
        "could not get responsible officer due to unexpected error; falling back to referring probation practitioner",
        e
      )
    }

    val referringProbationPractitioner = sentBy ?: createdBy
    val userDetail = hmppsAuthService.getUserDetail(referringProbationPractitioner)
    return ResponsibleProbationPractitioner(
      userDetail.firstName,
      userDetail.email,
      null,
      referringProbationPractitioner,
      userDetail.lastName
    )
  }

  fun isUserTheResponsibleOfficer(responsibleOfficer: ResponsibleProbationPractitioner, user: AuthUser): Boolean {
    return userTypeChecker.isProbationPractitionerUser(user) &&
      (
        (responsibleOfficer.authUser?.let { it == user } ?: false) ||
          (
            responsibleOfficer.deliusStaffId?.let { it == communityAPIOffenderService.getStaffIdentifier(user) }
              ?: false // if the RO doesn't have a staff ID we cannot determine if they are the sender, so assume not
            )
        )
  }
}
