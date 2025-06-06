package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service

import mu.KotlinLogging
import net.logstash.logback.argument.StructuredArguments.kv
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Lazy
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.domain.Specification.not
import org.springframework.data.jpa.domain.Specification.where
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.server.ResponseStatusException
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.authorization.ReferralAccessChecker
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.authorization.ReferralAccessFilter
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.authorization.ServiceProviderAccessScopeMapper
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.authorization.UserTypeChecker
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.config.AccessError
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.DashboardType
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.UpdateReferralDetailsDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.WithdrawReferralRequestDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.ReferralEventPublisher
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AuthUser
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.CancellationReason
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.DynamicFrameworkContract
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Referral
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ReferralAssignment
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ReferralDetails
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ServiceProviderSentReferralSummary
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Status
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.WithdrawalReason
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.AuthUserRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.CancellationReasonRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.DeliverySessionRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.InterventionRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ReferralDetailsRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ReferralRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ReferralSummary
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ReferralSummaryRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.SentReferralSummariesRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ServiceCategoryRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.WithdrawalReasonRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.specification.ReferralSpecifications
import java.time.OffsetDateTime
import java.util.UUID

data class ResponsibleProbationPractitioner(
  override val firstName: String,
  override val email: String,
  val deliusStaffCode: String?,
  val authUser: AuthUser?,
  override val lastName: String,
) : ContactablePerson

data class ReferralSummaryQuery(
  val completed: Boolean?,
  val cancelled: Boolean?,
  val unassigned: Boolean?,
  val assignedToUserId: String?,
  val page: Pageable,
  val searchText: String?,
  val isSpUser: Boolean,
  val isPPUser: Boolean,
  val createdById: String?,
  val serviceUserCrns: List<String> = emptyList(),
  val contracts: Set<DynamicFrameworkContract> = emptySet(),
)

@Service
@Transactional
class ReferralService(
  val referralRepository: ReferralRepository,
  val sentReferralSummariesRepository: SentReferralSummariesRepository,
  @Qualifier("referralSummaryRepositoryImpl") val referralSummaryRepository: ReferralSummaryRepository,
  val authUserRepository: AuthUserRepository,
  val interventionRepository: InterventionRepository,
  val referralConcluder: ReferralConcluder,
  val eventPublisher: ReferralEventPublisher,
  val cancellationReasonRepository: CancellationReasonRepository,
  val withdrawalReasonRepository: WithdrawalReasonRepository,
  val deliverySessionRepository: DeliverySessionRepository,
  val serviceCategoryRepository: ServiceCategoryRepository,
  val referralAccessChecker: ReferralAccessChecker,
  val userTypeChecker: UserTypeChecker,
  val serviceProviderUserAccessScopeMapper: ServiceProviderAccessScopeMapper,
  val referralAccessFilter: ReferralAccessFilter,
  val communityAPIOffenderService: CommunityAPIOffenderService,
  val ramDeliusAPIOffenderService: RamDeliusAPIOffenderService,
  @Lazy val amendReferralService: AmendReferralService,
  val hmppsAuthService: HMPPSAuthService,
  val telemetryService: TelemetryService,
  val referralDetailsRepository: ReferralDetailsRepository,
) {
  companion object {
    private val logger = KotlinLogging.logger {}
  }

  fun getSentReferralForUser(id: UUID, user: AuthUser): Referral? {
    val referral = referralRepository.findByIdAndSentAtIsNotNull(id)

    referral?.let {
      referralAccessChecker.forUser(it, user)
    }

    return referral
  }

  // This is required for Client API access where the authority to access ALL referrals is pre-checked in the controller
  fun getSentReferral(id: UUID): Referral? = referralRepository.findByIdAndSentAtIsNotNull(id)

  fun getReferralDetailsById(id: UUID): ReferralDetails? = referralDetailsRepository.findByIdOrNull(id)

  fun assignSentReferral(referral: Referral, assignedBy: AuthUser, assignedTo: AuthUser): Referral {
    val assignment = ReferralAssignment(
      OffsetDateTime.now(),
      authUserRepository.save(assignedBy),
      authUserRepository.save(assignedTo),
    )
    referral.assignments.forEach { it.superseded = true }
    referral.assignments.add(assignment)

    val assignedReferral = referralRepository.save(referral)
    eventPublisher.referralAssignedEvent(assignedReferral)
    return assignedReferral
  }

  fun setReferralStatus(referral: Referral, status: Status) {
    referral.status = status
    referralRepository.save(referral)
  }

  fun getSentReferralSummaryForUser(user: AuthUser, completed: Boolean?, cancelled: Boolean?, unassigned: Boolean?, assignedToUserId: String?, page: Pageable, searchText: String? = null): Iterable<ReferralSummary> {
    if (userTypeChecker.isServiceProviderUser(user)) {
      val contracts = serviceProviderUserAccessScopeMapper.fromUser(user).contracts
      val referralSummary = ReferralSummaryQuery(
        completed,
        cancelled,
        unassigned,
        assignedToUserId,
        page,
        searchText,
        isSpUser = true,
        isPPUser = false,
        user.id,
        contracts = contracts,
      )
      return referralSummaryRepository.getReferralSummaries(referralSummary)
    }

    if (userTypeChecker.isProbationPractitionerUser(user)) {
      val serviceUserCrns = crnsAssociatedWithPP(user)
      val referralSummary = ReferralSummaryQuery(
        completed,
        cancelled,
        unassigned,
        assignedToUserId,
        page,
        searchText,
        isSpUser = false,
        isPPUser = true,
        user.id,
        serviceUserCrns = serviceUserCrns,
      )
      return referralSummaryRepository.getReferralSummaries(referralSummary)
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

  private fun getSentReferralSummariesForServiceProviderUser(user: AuthUser, dashboardType: DashboardType?): List<ServiceProviderSentReferralSummary> {
    val serviceProviders = serviceProviderUserAccessScopeMapper.fromUser(user).serviceProviders
    val referralSummaries = referralRepository.getSentReferralSummaries(user, serviceProviders = serviceProviders.map { serviceProvider -> serviceProvider.id }, dashboardType)
    return referralAccessFilter.serviceProviderReferralSummaries(referralSummaries, user)
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

  private fun <T> searchSpec(searchText: String): Specification<T> = if (searchText.matches(Regex("[A-Z]{2}[0-9]{4}[A-Z]{2}"))) {
    ReferralSpecifications.searchByReferenceNumber(searchText)
  } else {
    ReferralSpecifications.searchByPoPName(searchText)
  }

  private fun <T> createSpecificationForProbationPractitionerUser(
    user: AuthUser,
    sentReferralFilterSpecification: Specification<T>,
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

  private fun crnsAssociatedWithPP(
    user: AuthUser,
  ): List<String> {
    try {
      return communityAPIOffenderService.getManagedOffendersForDeliusUser(user).map { it.crnNumber }
    } catch (e: WebClientResponseException) {
      // don't stop users seeing their own referrals just because delius is not playing nice
      logger.error(
        "failed to get managed offenders for user {}, {}",
        e,
        kv("username", user.userName),
      )
    }
    return emptyList()
  }

  fun requestReferralEnd(referral: Referral, user: AuthUser, withdrawReferralRequestDTO: WithdrawReferralRequestDTO): Referral {
    val now = OffsetDateTime.now()
    referral.endRequestedAt = now
    referral.endRequestedBy = authUserRepository.save(user)
    updateWithdrawalInformation(referral, withdrawReferralRequestDTO)
    val result = referralRepository.save(referral)
    referralConcluder.withdrawReferral(result, ReferralWithdrawalState.valueOf(withdrawReferralRequestDTO.withdrawalState))
    return result
  }

  private fun updateWithdrawalInformation(referral: Referral, withdrawReferralRequestDTO: WithdrawReferralRequestDTO) {
    validateWithdrawalReasonCode(withdrawReferralRequestDTO.code)
    referral.withdrawalReasonCode = withdrawReferralRequestDTO.code
    referral.withdrawalComments = withdrawReferralRequestDTO.comments
    telemetryService.reportWithdrawalInformation(referral, withdrawReferralRequestDTO)
  }

  private fun validateWithdrawalReasonCode(withdrawalReasonCode: String) {
    withdrawalReasonRepository.findByCode(withdrawalReasonCode)
      ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid withdrawal code. [code=$withdrawalReasonCode]")
  }

  fun updateReferralDetails(referral: Referral, update: UpdateReferralDetailsDTO, actor: AuthUser): ReferralDetails? {
    if (!update.isValidUpdate) {
      return null
    }

    val existingDetails = referralDetailsRepository.findLatestByReferralId(referral.id)

    val newDetails = ReferralDetails(
      UUID.randomUUID(),
      null,
      referral.id,
      OffsetDateTime.now(),
      authUserRepository.save(actor).id,
      update.reasonForChange,
      existingDetails?.completionDeadline,
      existingDetails?.furtherInformation,
      existingDetails?.maximumEnforceableDays,
      existingDetails?.reasonForReferral,
      existingDetails?.reasonForReferralFurtherInformation,
    )

    amendReferralService.logChanges(
      existingDetails!!,
      update,
      actor,
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

    update.reasonForReferral?.let {
      newDetails.reasonForReferral = it
    }

    update.reasonForReferralFurtherInformation?.let {
      newDetails.reasonForReferralFurtherInformation = it
    }

    referralDetailsRepository.save(newDetails)

    if (existingDetails !== newDetails) {
      existingDetails.supersededById = newDetails.id
      referralDetailsRepository.save(existingDetails)

      eventPublisher.referralDetailsChangedEvent(referral, newDetails, existingDetails)
    }

    return newDetails
  }

  fun getCancellationReasons(): List<CancellationReason> = cancellationReasonRepository.findAll()

  fun getWithdrawalReasons(): List<WithdrawalReason> = withdrawalReasonRepository.findAll()

  fun getWithdrawalReason(code: String): WithdrawalReason? = withdrawalReasonRepository.findByCode(code)

  fun getResponsibleProbationPractitioner(referral: Referral): ResponsibleProbationPractitioner = getResponsibleProbationPractitioner(referral.serviceUserCRN, referral.sentBy, referral.createdBy)

  fun getResponsibleProbationPractitioner(crn: String, sentBy: AuthUser?, createdBy: AuthUser): ResponsibleProbationPractitioner {
    try {
      val officerDetails = ramDeliusAPIOffenderService.getResponsibleOfficerDetails(crn)!!
      val responsibleOfficer = officerDetails.responsibleManager ?: officerDetails.communityManager
      if (responsibleOfficer.email != null) {
        return ResponsibleProbationPractitioner(
          responsibleOfficer.name.forename,
          responsibleOfficer.email,
          responsibleOfficer.code,
          responsibleOfficer.username?.let { authUserRepository.findByUserName(it) },
          responsibleOfficer.name.surname,
        )
      }

      telemetryService.reportInvalidAssumption(
        "all responsible officers have email addresses",
        listOfNotNull(
          "communityManager" to officerDetails.communityManager.code,
          officerDetails.prisonManager?.let { "prisonManager" to it.code },
        ).toMap(),
      )

      logger.warn("no email address for responsible officer; falling back to referring probation practitioner")
    } catch (e: Exception) {
      logger.error(
        "could not get responsible officer due to unexpected error; falling back to referring probation practitioner",
        e,
      )
    }

    val referringProbationPractitioner = sentBy ?: createdBy
    val userDetail = hmppsAuthService.getUserDetail(referringProbationPractitioner)
    return ResponsibleProbationPractitioner(
      userDetail.firstName,
      userDetail.email,
      null,
      referringProbationPractitioner,
      userDetail.lastName,
    )
  }

  fun isUserTheResponsibleOfficer(responsibleOfficer: ResponsibleProbationPractitioner, user: AuthUser): Boolean = userTypeChecker.isProbationPractitionerUser(user) && (responsibleOfficer.authUser?.let { it.userName == user.userName } ?: false)
}
