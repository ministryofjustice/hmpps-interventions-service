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
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.authorization.ReferralAccessChecker
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.authorization.ReferralAccessFilter
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.authorization.ServiceProviderAccessScopeMapper
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.authorization.UserTypeChecker
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.config.AccessError
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.DashboardType
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.ReferralAmendmentDetails
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.UpdateReferralDetailsDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.ReferralEventPublisher
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AuthUser
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.CancellationReason
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Changelog
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Referral
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ReferralAssignment
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ReferralDetails
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.SentReferralSummary
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ServiceProviderSentReferralSummary
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
  val cancellationReasonRepository: CancellationReasonRepository,
  val deliverySessionRepository: DeliverySessionRepository,
  val serviceCategoryRepository: ServiceCategoryRepository,
  val referralAccessChecker: ReferralAccessChecker,
  val userTypeChecker: UserTypeChecker,
  val serviceProviderUserAccessScopeMapper: ServiceProviderAccessScopeMapper,
  val referralAccessFilter: ReferralAccessFilter,
  val communityAPIOffenderService: CommunityAPIOffenderService,
  val hmppsAuthService: HMPPSAuthService,
  val telemetryService: TelemetryService,
  val referralDetailsRepository: ReferralDetailsRepository,
  val changelogRepository: ChangelogRepository,
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
  fun getSentReferral(id: UUID): Referral? {
    return referralRepository.findByIdAndSentAtIsNotNull(id)
  }

  fun getReferralDetailsById(id: UUID?): ReferralDetails? {
    return referralDetailsRepository.findByIdOrNull(id)
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
    )
    logChanges(
      existingDetails!!,
      update,
      actor
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

    if (existingDetails !== newDetails) {
      existingDetails.supersededById = newDetails.id
      referralDetailsRepository.saveAndFlush(existingDetails)

      eventPublisher.referralDetailsChangedEvent(referral, newDetails, existingDetails)
    }

    return newDetails
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

  private fun logChanges(referralDetails: ReferralDetails, update: UpdateReferralDetailsDTO, actor: AuthUser) {
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

  fun getCancellationReasons(): List<CancellationReason> {
    return cancellationReasonRepository.findAll()
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
