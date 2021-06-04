package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.integration

import com.microsoft.applicationinsights.boot.dependencies.apachecommons.lang3.RandomStringUtils
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ActionPlan
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ActionPlanActivity
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ActionPlanAppointment
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Attended
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AuthUser
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.CancellationReason
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ComplexityLevel
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ContractType
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.DesiredOutcome
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.DynamicFrameworkContract
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.EndOfServiceReport
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Intervention
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.NPSRegion
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Referral
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.SelectedDesiredOutcomesMapping
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ServiceCategory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ServiceUserData
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ActionPlanAppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ActionPlanRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.AuthUserRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.CancellationReasonRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ContractTypeRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.DesiredOutcomeRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.DynamicFrameworkContractRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.EndOfServiceReportRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.InterventionRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.NPSRegionRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ReferralRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ServiceCategoryRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ServiceProviderRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ContractTypeFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.DynamicFrameworkContractFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.EndOfServiceReportFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.InterventionFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ReferralFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ServiceProviderFactory
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.Random
import java.util.UUID

// allows random selection of service category, region etc
val random = Random()
fun <T, U> Map<T, U>.random(): Map.Entry<T, U> = entries.elementAt(random.nextInt(size))
fun <T> Collection<T>.random(): T = elementAt(random.nextInt(size))

class SetupAssistant(
  private val authUserRepository: AuthUserRepository,
  private val referralRepository: ReferralRepository,
  private val interventionRepository: InterventionRepository,
  private val actionPlanRepository: ActionPlanRepository,
  private val actionPlanAppointmentRepository: ActionPlanAppointmentRepository,
  private val serviceCategoryRepository: ServiceCategoryRepository,
  private val serviceProviderRepository: ServiceProviderRepository,
  private val npsRegionRepository: NPSRegionRepository,
  private val dynamicFrameworkContractRepository: DynamicFrameworkContractRepository,
  private val desiredOutcomeRepository: DesiredOutcomeRepository,
  private val endOfServiceReportRepository: EndOfServiceReportRepository,
  private val cancellationReasonRepository: CancellationReasonRepository,
  private val contractTypeRepository: ContractTypeRepository,
) {
  private val dynamicFrameworkContractFactory = DynamicFrameworkContractFactory()
  private val interventionFactory = InterventionFactory()
  private val referralFactory = ReferralFactory()
  private val serviceProviderFactory = ServiceProviderFactory()
  private val endOfServiceReportFactory = EndOfServiceReportFactory()
  private val contractTypeFactory = ContractTypeFactory()

  val serviceCategories = serviceCategoryRepository.findAll().associateBy { it.name }
  val npsRegions = npsRegionRepository.findAll().associateBy { it.id }
  val cancellationReasons = cancellationReasonRepository.findAll().associateBy { it.code }
  val contractTypes = contractTypeRepository.findAll().associateBy { it.code }

  fun cleanAll() {
    // order of cleanup is important here to avoid breaking foreign key constraints
    actionPlanAppointmentRepository.deleteAll()
    actionPlanRepository.deleteAll()
    endOfServiceReportRepository.deleteAll()

    referralRepository.deleteAll()
    interventionRepository.deleteAll()
    dynamicFrameworkContractRepository.deleteAll()

    serviceProviderRepository.deleteAll()
    authUserRepository.deleteAll()
  }

  fun randomServiceCategory(): ServiceCategory {
    return serviceCategories.random().value
  }
  fun randomCancellationReason(): CancellationReason {
    return cancellationReasons.random().value
  }

  fun randomDesiredOutcome(): DesiredOutcome {
    return desiredOutcomeRepository.findAll().random()
  }

  fun randomComplexityLevel(serviceCategory: ServiceCategory): ComplexityLevel {
    return serviceCategoryRepository.findByIdOrNull(serviceCategory.id)!!.complexityLevels.random()
  }

  fun randomContractType(): ContractType {
    return contractTypes.random().value
  }

  fun randomNPSRegion(): NPSRegion {
    return npsRegions.random().value
  }

  fun desiredOutcomesForServiceCategory(serviceCategoryId: UUID): List<DesiredOutcome> {
    return desiredOutcomeRepository.findByServiceCategoryId(serviceCategoryId)
  }

  fun createDesiredOutcome(id: UUID, description: String, serviceCategoryId: UUID): DesiredOutcome {
    return desiredOutcomeRepository.save(DesiredOutcome(id, description, serviceCategoryId))
  }

  fun createPPUser(id: String = "8751622134"): AuthUser {
    val user = AuthUser(id, "delius", "BERNARD.BEAKS")
    return authUserRepository.save(user)
  }

  fun createSPUser(): AuthUser {
    val user = AuthUser("608955ae-52ed-44cc-884c-011597a77949", "auth", "AUTH_USER")
    return authUserRepository.save(user)
  }

  fun createIntervention(
    id: UUID = UUID.randomUUID(),
    dynamicFrameworkContract: DynamicFrameworkContract? = null
  ): Intervention {
    val contractType = contractTypes["ACC"]!!
    val region = npsRegions['C']!!

    val primeProvider = serviceProviderRepository.save(serviceProviderFactory.create())

    val contract = dynamicFrameworkContract
      ?: createDynamicFrameworkContract(
        contractType = contractType,
        primeProviderId = primeProvider.id,
        npsRegion = region,
      )

    return interventionRepository.save(interventionFactory.create(id = id, contract = contract))
  }

  fun createDraftReferral(
    id: UUID = UUID.randomUUID(),
    intervention: Intervention = createIntervention(),
    createdBy: AuthUser = createPPUser(),
    createdAt: OffsetDateTime = OffsetDateTime.now(),
    serviceUserCRN: String = "X123456",
  ): Referral {
    return referralRepository.save(
      referralFactory.createDraft(
        id = id,
        intervention = intervention,
        createdAt = createdAt,
        createdBy = createdBy,
        serviceUserCRN = serviceUserCRN,
      )
    )
  }

  fun createEndedReferral(id: UUID = UUID.randomUUID(), intervention: Intervention = createIntervention(), endRequestedReason: CancellationReason? = randomCancellationReason(), endRequestedComments: String? = null): Referral {
    val ppUser = createPPUser()
    val spUser = createSPUser()
    return referralRepository.save(referralFactory.createEnded(id = id, intervention = intervention, createdBy = ppUser, sentBy = ppUser, endRequestedBy = ppUser, assignedTo = spUser, endRequestedReason = endRequestedReason, endRequestedComments = endRequestedComments))
  }

  fun createSentReferral(id: UUID = UUID.randomUUID(), intervention: Intervention = createIntervention(), ppUser: AuthUser = createPPUser()): Referral {
    return referralRepository.save(referralFactory.createSent(id = id, intervention = intervention, createdBy = ppUser, sentBy = ppUser))
  }

  fun createDynamicFrameworkContract(
    id: UUID = UUID.randomUUID(),
    contractType: ContractType = randomContractType(),
    contractReference: String = RandomStringUtils.randomAlphanumeric(8),
    primeProviderId: String,
    subContractorServiceProviderIds: Set<String> = emptySet(),
    npsRegion: NPSRegion = randomNPSRegion(),
  ): DynamicFrameworkContract {
    val primeProvider = serviceProviderRepository.save(serviceProviderFactory.create(id = primeProviderId, name = primeProviderId))
    val serviceProviders = subContractorServiceProviderIds.map {
      serviceProviderRepository.save(serviceProviderFactory.create(id = it, name = it))
    }.toSet()

    val contract = dynamicFrameworkContractFactory.create(
      id = id,
      contractType = contractType,
      contractReference = contractReference,
      primeProvider = primeProvider,
      subcontractorProviders = serviceProviders,
      npsRegion = npsRegion
    )
    return dynamicFrameworkContractRepository.save(contract)
  }

  fun createAssignedReferral(id: UUID = UUID.randomUUID(), intervention: Intervention? = null): Referral {
    val intervention = intervention ?: createIntervention()
    val ppUser = createPPUser()
    val spUser = createSPUser()
    return referralRepository.save(
      referralFactory.createSent(
        id = id, intervention = intervention, createdBy = ppUser, sentBy = ppUser,
        assignedTo = spUser, assignedBy = spUser, assignedAt = OffsetDateTime.now()
      )
    )
  }

  fun createActionPlan(
    referral: Referral = createSentReferral(),
    id: UUID = UUID.randomUUID(),
    numberOfSessions: Int? = null,
    activities: MutableList<ActionPlanActivity> = mutableListOf(),
    createdAt: OffsetDateTime = OffsetDateTime.now(),
    createdBy: AuthUser = createSPUser(),
    submittedAt: OffsetDateTime? = null,
    submittedBy: AuthUser? = null
  ): ActionPlan {
    return actionPlanRepository.save(
      ActionPlan(
        id = id,
        createdAt = createdAt,
        createdBy = createdBy,
        referral = referral,
        activities = activities,
        numberOfSessions = numberOfSessions,
        submittedAt = submittedAt,
        submittedBy = submittedBy,
      )
    )
  }

  fun createActionPlanAppointment(
    actionPlan: ActionPlan,
    sessionNumber: Int,
    duration: Int,
    appointmentTime: OffsetDateTime,
    attended: Attended? = null,
    attendanceInfo: String? = null,
    behaviour: String? = null,
    notifyPPOfBehaviour: Boolean? = null
  ): ActionPlanAppointment {
    val now = OffsetDateTime.now()
    val user = createSPUser()
    return actionPlanAppointmentRepository.save(
      ActionPlanAppointment(
        id = UUID.randomUUID(),
        sessionNumber = sessionNumber,
        attended = attended,
        additionalAttendanceInformation = attendanceInfo,
        attendanceSubmittedAt = if (attended != null) now else null,
        attendanceBehaviour = behaviour,
        attendanceBehaviourSubmittedAt = if (behaviour != null) now else null,
        notifyPPOfAttendanceBehaviour = notifyPPOfBehaviour,
        appointmentTime = appointmentTime,
        durationInMinutes = duration,
        createdBy = user,
        createdAt = now,
        actionPlan = actionPlan,
      )
    )
  }

  fun fillReferralFields(
    referral: Referral,
    selectedServiceCategories: LinkedHashSet<ServiceCategory> = referral.intervention.dynamicFrameworkContract.contractType.serviceCategories,
    complexityLevelIds: MutableMap<UUID, UUID>? = mutableMapOf(selectedServiceCategories.elementAt(0).id to randomComplexityLevel(selectedServiceCategories.elementAt(0)).id),
    desiredOutcomes: List<DesiredOutcome> = referral.intervention.dynamicFrameworkContract.contractType.serviceCategories.first().desiredOutcomes,
    serviceUserData: ServiceUserData = ServiceUserData(
      referral = referral,
      title = "Mr",
      firstName = "Alex",
      lastName = "River",
      dateOfBirth = LocalDate.of(1980, 1, 1),
      gender = "Male",
      preferredLanguage = "English",
      ethnicity = "British",
      religionOrBelief = "Agnostic",
      disabilities = listOf("Autism spectrum condition"),
    ),
    accessibilityNeeds: String = "She uses a wheelchair",
    additionalNeedsInformation: String = "Alex is currently sleeping on her aunt's sofa",
    additionalRiskInformation: String = "A danger to the elderly",
    completionDeadline: LocalDate = LocalDate.of(2021, 4, 1),
    furtherInformation: String = "Some information about the service user",
    hasAdditionalResponsibilities: Boolean = true,
    interpreterLanguage: String = "Spanish",
    maximumRarDays: Int = 10,
    needsInterpreter: Boolean = true,
    relevantSentenceId: Long = 2600295124,
    usingRarDays: Boolean = true,
    whenUnavailable: String = "She works Mondays 9am - midday",
  ): Referral {
    referral.selectedServiceCategories = selectedServiceCategories
    // required to satisfy foreign key constrains on desired outcomes and complexity levels
    referralRepository.saveAndFlush(referral)

    referral.serviceUserData = serviceUserData
    referral.selectedDesiredOutcomes = desiredOutcomes.map { SelectedDesiredOutcomesMapping(it.serviceCategoryId, it.id) }.toMutableList()
    referral.accessibilityNeeds = accessibilityNeeds
    referral.additionalNeedsInformation = additionalNeedsInformation
    referral.additionalRiskInformation = additionalRiskInformation
    referral.completionDeadline = completionDeadline
    referral.complexityLevelIds = complexityLevelIds
    referral.furtherInformation = furtherInformation
    referral.hasAdditionalResponsibilities = hasAdditionalResponsibilities
    referral.interpreterLanguage = interpreterLanguage
    referral.maximumRarDays = maximumRarDays
    referral.needsInterpreter = needsInterpreter
    referral.relevantSentenceId = relevantSentenceId
    referral.usingRarDays = usingRarDays
    referral.whenUnavailable = whenUnavailable

    return referralRepository.save(referral)
  }

  fun createEndOfServiceReport(id: UUID = UUID.randomUUID(), referral: Referral = createAssignedReferral()): EndOfServiceReport {
    val eosr = endOfServiceReportRepository.save(
      endOfServiceReportFactory.create(id = id, referral = referral, createdBy = referral.assignedTo!!)
    )
    referral.endOfServiceReport = eosr
    return eosr
  }
}
