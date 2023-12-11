package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service

import mu.KotlinLogging
import net.logstash.logback.argument.StructuredArguments.kv
import org.springframework.context.annotation.Lazy
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.domain.Specification.not
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.reactive.function.client.WebClientResponseException
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.authorization.ReferralAccessChecker
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.authorization.ReferralAccessFilter
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.authorization.UserTypeChecker
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.config.AccessError
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.UpdateReferralDetailsDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.ReferralEventPublisher
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AuthUser
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.CancellationReason
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Referral
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ReferralAssignment
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ReferralDetails
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.model.ReferralSummary
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.AuthUserRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.CancellationReasonRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.DeliverySessionRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.InterventionRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ReferralDetailsRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ReferralRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ReferralSummariesRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ServiceCategoryRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.specification.ReferralSummarySpecifications
import java.time.OffsetDateTime
import java.util.UUID

data class ResponsibleProbationPractitioner(
  override val firstName: String,
  override val email: String,
  val deliusStaffCode: String?,
  val authUser: AuthUser?,
  override val lastName: String,
) : ContactablePerson

@Service
@Transactional
class ReferralService(
  val referralRepository: ReferralRepository,
  val referralSummariesRepository: ReferralSummariesRepository,
  val authUserRepository: AuthUserRepository,
  val interventionRepository: InterventionRepository,
  val referralConcluder: ReferralConcluder,
  val eventPublisher: ReferralEventPublisher,
  val cancellationReasonRepository: CancellationReasonRepository,
  val deliverySessionRepository: DeliverySessionRepository,
  val serviceCategoryRepository: ServiceCategoryRepository,
  val referralAccessChecker: ReferralAccessChecker,
  val userTypeChecker: UserTypeChecker,
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
  private val referenceNumberRegexPattern = "[A-Z]{2}[0-9]{4}[A-Z]{2}"

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
      authUserRepository.save(assignedTo),
    )
    referral.assignments.forEach { it.superseded = true }
    referral.assignments.add(assignment)

    val assignedReferral = referralRepository.save(referral)
    eventPublisher.referralAssignedEvent(assignedReferral)
    return assignedReferral
  }

  fun getSentReferralSummaryForUser(user: AuthUser, concluded: Boolean?, cancelled: Boolean?, unassigned: Boolean?, assignedToUserId: String?, page: Pageable, searchText: String? = null): Iterable<ReferralSummary> {
    if (userTypeChecker.isServiceProviderUser(user)) {
      val filteredSpec = referralAccessFilter.serviceProviderContracts(user)
      val findSentReferralsSpec: Specification<ReferralSummary> = createSpecification(concluded, cancelled, unassigned, assignedToUserId, searchText, filteredSpec)
      return referralSummariesRepository.findAll(findSentReferralsSpec, page)
    }

    if (userTypeChecker.isProbationPractitionerUser(user)) {
      val filteredSpec = createSpecificationForProbationPractitionerUser(user)
      val findSentReferralsSpec: Specification<ReferralSummary> = createSpecification(concluded, cancelled, unassigned, assignedToUserId, searchText, filteredSpec)
      return referralSummariesRepository.findAll(findSentReferralsSpec, page)
    }

    throw AccessError(user, "unsupported user type", listOf("logins from ${user.authSource} are not supported"))
  }

  private fun <T> applyOptionalConjunction(existingSpec: Specification<T>, predicate: Boolean?, specToJoin: Specification<T>): Specification<T> {
    if (predicate == null) return existingSpec
    return existingSpec.and(if (predicate) specToJoin else not(specToJoin))
  }

  private fun createSpecification(
    concluded: Boolean?,
    cancelled: Boolean?,
    unassigned: Boolean?,
    assignedToUserId: String?,
    searchText: String?,
    findSentReferralsSpec: Specification<ReferralSummary>,
  ): Specification<ReferralSummary> {
    var amendSpec = findSentReferralsSpec
    amendSpec = applyOptionalConjunction(amendSpec, concluded, ReferralSummarySpecifications.concluded(concluded))
    amendSpec = applyOptionalConjunction(amendSpec, cancelled, ReferralSummarySpecifications.cancelled())
    amendSpec = applyOptionalConjunction(amendSpec, unassigned, ReferralSummarySpecifications.unassigned())
    assignedToUserId?.let {
      amendSpec = applyOptionalConjunction(amendSpec, true, ReferralSummarySpecifications.currentlyAssignedTo(it))
    }
    searchText?.let {
      amendSpec = applyOptionalConjunction(amendSpec, true, searchSpec(searchText))
    }
    return amendSpec
  }

  private fun searchSpec(searchText: String): Specification<ReferralSummary> {
    return if (searchText.matches(Regex(referenceNumberRegexPattern))) {
      ReferralSummarySpecifications.searchByReferenceNumber(searchText)
    } else {
      ReferralSummarySpecifications.searchByPoPName(searchText)
    }
  }

  private fun createSpecificationForProbationPractitionerUser(
    user: AuthUser,
  ): Specification<ReferralSummary> {
    var referralsForPPUser = ReferralSummarySpecifications.createdBy(user)
    try {
      val serviceUserCRNs = communityAPIOffenderService.getManagedOffendersForDeliusUser(user).map { it.crnNumber }
      referralsForPPUser = referralsForPPUser.or(ReferralSummarySpecifications.matchingServiceUserReferrals(serviceUserCRNs))
    } catch (e: WebClientResponseException) {
      // don't stop users seeing their own referrals just because delius is not playing nice
      logger.error(
        "failed to get managed offenders for user {}",
        e,
        kv("username", user.userName),
      )
    }
    return referralsForPPUser
  }

  fun requestReferralEnd(referral: Referral, user: AuthUser, reason: CancellationReason, comments: String?): Referral {
    val now = OffsetDateTime.now()
    referral.endRequestedAt = now
    referral.endRequestedBy = authUserRepository.save(user)
    referral.endRequestedReason = reason
    comments?.let { referral.endRequestedComments = it }

    return referralRepository.save(referral)
      .also { referralConcluder.concludeIfEligible(it) }
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

    referralDetailsRepository.save(newDetails)

    if (existingDetails !== newDetails) {
      existingDetails.supersededById = newDetails.id
      referralDetailsRepository.save(existingDetails)

      eventPublisher.referralDetailsChangedEvent(referral, newDetails, existingDetails)
    }

    return newDetails
  }

  fun getCancellationReasons(): List<CancellationReason> {
    return cancellationReasonRepository.findAll()
  }

  fun getResponsibleProbationPractitioner(referral: Referral): ResponsibleProbationPractitioner {
    return getResponsibleProbationPractitioner(referral.serviceUserCRN, referral.sentBy, referral.createdBy)
  }

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

  fun isUserTheResponsibleOfficer(responsibleOfficer: ResponsibleProbationPractitioner, user: AuthUser): Boolean {
    return userTypeChecker.isProbationPractitionerUser(user) && (responsibleOfficer.authUser?.let { it.userName == user.userName } ?: false)
  }
}
