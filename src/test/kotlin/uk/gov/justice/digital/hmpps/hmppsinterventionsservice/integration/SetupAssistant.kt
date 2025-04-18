package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.integration

import org.apache.commons.lang3.RandomStringUtils
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.AddressDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.ReferralAmendmentDetails
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ActionPlan
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ActionPlanActivity
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Appointment
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AppointmentDeliveryType
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AppointmentSessionType
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Attended
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AuthUser
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.CancellationReason
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.CaseNote
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Changelog
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ComplexityLevel
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ContractType
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.DeliverySession
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.DesiredOutcome
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.DraftReferral
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.DynamicFrameworkContract
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.EndOfServiceReport
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.EndOfServiceReportOutcome
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Intervention
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.NPSRegion
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.NoSessionReasonType
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.PersonCurrentLocationType
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ProbationPractitionerDetails
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Referral
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ReferralAssignment
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ReferralDetails
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ReferralLocation
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ReferralServiceUserData
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.SelectedDesiredOutcomesMapping
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ServiceCategory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ServiceUserData
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Status
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.SupplierAssessment
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.WithdrawalReason
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ActionPlanRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.AppointmentDeliveryAddressRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.AppointmentDeliveryRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.AppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.AuthUserRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.CancellationReasonRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.CaseNoteRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ChangelogRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ContractTypeRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.DeliverySessionRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.DesiredOutcomeRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.DraftOasysRiskInformationRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.DraftReferralRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.DynamicFrameworkContractRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.EndOfServiceReportRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.InterventionRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.NPSRegionRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ProbationPractitionerDetailsRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ReferralDetailsRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ReferralLocationRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ReferralRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ServiceCategoryRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ServiceProviderRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.SupplierAssessmentRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.WithdrawalReasonRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.AppointmentDeliveryAddressFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.AppointmentDeliveryFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.AppointmentFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.CaseNoteFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ChangeLogFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.DynamicFrameworkContractFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.EndOfServiceReportFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.EndOfServiceReportOutcomeFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.InterventionFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ProbationPractitionerDetailsFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ReferralFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ReferralServiceUserFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ServiceProviderFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ServiceUserFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.SupplierAssessmentFactory
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
  private val draftReferralRepository: DraftReferralRepository,
  private val interventionRepository: InterventionRepository,
  private val actionPlanRepository: ActionPlanRepository,
  private val deliverySessionRepository: DeliverySessionRepository,
  private val serviceCategoryRepository: ServiceCategoryRepository,
  private val serviceProviderRepository: ServiceProviderRepository,
  private val npsRegionRepository: NPSRegionRepository,
  private val dynamicFrameworkContractRepository: DynamicFrameworkContractRepository,
  private val desiredOutcomeRepository: DesiredOutcomeRepository,
  private val endOfServiceReportRepository: EndOfServiceReportRepository,
  private val cancellationReasonRepository: CancellationReasonRepository,
  private val withdrawalReasonRepository: WithdrawalReasonRepository,
  private val contractTypeRepository: ContractTypeRepository,
  private val appointmentRepository: AppointmentRepository,
  private val supplierAssessmentRepository: SupplierAssessmentRepository,
  private val appointmentDeliveryRepository: AppointmentDeliveryRepository,
  private val appointmentDeliveryAddressRepository: AppointmentDeliveryAddressRepository,
  private val caseNoteRepository: CaseNoteRepository,
  private val referralDetailsRepository: ReferralDetailsRepository,
  private val changeLogRepository: ChangelogRepository,
  private val referralLocationRepository: ReferralLocationRepository,
  private val probationPractitionerDetailsRepository: ProbationPractitionerDetailsRepository,
  private val draftOasysRiskInformationRepository: DraftOasysRiskInformationRepository,
) {
  private val dynamicFrameworkContractFactory = DynamicFrameworkContractFactory()
  private val interventionFactory = InterventionFactory()
  private val referralFactory = ReferralFactory()
  private val serviceProviderFactory = ServiceProviderFactory()
  private val endOfServiceReportFactory = EndOfServiceReportFactory()
  private val appointmentFactory = AppointmentFactory()
  private val appointmentDeliveryFactory = AppointmentDeliveryFactory()
  private val appointmentDeliveryAddressFactory = AppointmentDeliveryAddressFactory()
  private val supplierAssessmentFactory = SupplierAssessmentFactory()
  private val serviceUserFactory = ServiceUserFactory()
  private val referralServiceUserFactory = ReferralServiceUserFactory()
  private val caseNoteFactory = CaseNoteFactory()
  private val changelogFactory = ChangeLogFactory()
  private val endOfServiceReportOutcomeFactory = EndOfServiceReportOutcomeFactory()
  private val probationPractitionerDetailsFactory = ProbationPractitionerDetailsFactory()

  val serviceCategories = serviceCategoryRepository.findAll().associateBy { it.name }
  val npsRegions = npsRegionRepository.findAll().associateBy { it.id }
  val cancellationReasons = cancellationReasonRepository.findAll().associateBy { it.code }
  val withdrawalReasons = withdrawalReasonRepository.findAll().associateBy { it.code }
  val contractTypes = contractTypeRepository.findAll().associateBy { it.code }

  fun cleanAll() {
    // order of cleanup is important here to avoid breaking foreign key constraints
    caseNoteRepository.deleteAll()
    deliverySessionRepository.deleteAll()
    supplierAssessmentRepository.deleteAll()
    actionPlanRepository.deleteAll()

    referralLocationRepository.deleteAll()
    endOfServiceReportRepository.deleteAll()
    appointmentDeliveryAddressRepository.deleteAll()
    appointmentDeliveryRepository.deleteAll()
    appointmentRepository.deleteAllInBatch()
    changeLogRepository.deleteAll()
    deleteAllReferralDetails()
    probationPractitionerDetailsRepository.deleteAll()
    draftOasysRiskInformationRepository.deleteAll()
    referralRepository.deleteAll()
    draftReferralRepository.deleteAll()
    interventionRepository.deleteAll()
    dynamicFrameworkContractRepository.deleteAll()

    serviceProviderRepository.deleteAll()
    authUserRepository.deleteAll()
    draftOasysRiskInformationRepository.deleteAll()
  }

  private fun deleteAllReferralDetails() {
    // this is kinda annoying - foreign key constraints on referral details
    // 'superseded_by_id' mean we have to delete the rows in creation order
    referralDetailsRepository
      .findAll()
      .sortedBy { it.createdAt }
      .forEach { referralDetailsRepository.delete(it) }
  }

  fun serviceCategory(id: UUID): ServiceCategory = serviceCategoryRepository.findById(id).get()

  fun randomServiceCategory(): ServiceCategory = serviceCategories.random().value
  fun randomCancellationReason(): CancellationReason = cancellationReasons.random().value

  fun randomWithDrawReason(): WithdrawalReason = withdrawalReasons.random().value

  fun randomDesiredOutcome(): DesiredOutcome = desiredOutcomeRepository.findAll().random()

  fun randomComplexityLevel(serviceCategory: ServiceCategory): ComplexityLevel = serviceCategoryRepository.findByIdOrNull(serviceCategory.id)!!.complexityLevels.random()

  fun randomContractType(): ContractType = contractTypes.random().value

  fun randomNPSRegion(): NPSRegion = npsRegions.random().value

  fun desiredOutcomesForServiceCategory(serviceCategoryId: UUID): List<DesiredOutcome> = desiredOutcomeRepository.findByServiceCategoryId(serviceCategoryId)

  fun getDesiredOutcome(desiredOutcomeId: UUID): DesiredOutcome = desiredOutcomeRepository.findByIdOrNull(desiredOutcomeId)!!

  fun createDesiredOutcome(id: UUID, description: String, serviceCategoryId: UUID): DesiredOutcome = desiredOutcomeRepository.save(DesiredOutcome(id, description, serviceCategoryId, deprecatedAt = null, desiredOutcomeFilterRules = mutableSetOf()))

  fun createPPUser(id: String = "8751622134"): AuthUser {
    val user = AuthUser(id, "delius", "BERNARD.BEAKS")
    return authUserRepository.save(user)
  }
  fun createPPUserSecond(id: String = "2500128586"): AuthUser {
    val user = AuthUser(id, "delius", "joe.smith")
    return authUserRepository.save(user)
  }

  fun createSPUser(username: String = "AUTH_USER"): AuthUser {
    val user = AuthUser("608955ae-52ed-44cc-884c-011597a77949", "auth", username)
    return authUserRepository.save(user)
  }

  fun createIntervention(
    id: UUID = UUID.randomUUID(),
    dynamicFrameworkContract: DynamicFrameworkContract? = null,
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

  fun createIntervention(
    id: UUID = UUID.randomUUID(),
    interventionTitle: String,
    serviceProviderId: String,
    dynamicFrameworkContract: DynamicFrameworkContract? = null,
  ): Intervention {
    val contractType = contractTypes["ACC"]!!
    val region = npsRegions['C']!!

    var contract = dynamicFrameworkContract

    if (contract == null) {
      val primeProvider = serviceProviderRepository.save(serviceProviderFactory.create(id = serviceProviderId))
      contract = createDynamicFrameworkContract(
        contractType = contractType,
        primeProviderId = primeProvider.id,
        npsRegion = region,
      )
    }

    return interventionRepository.save(interventionFactory.create(id = id, contract = contract, title = interventionTitle))
  }

  fun createDraftReferral(
    id: UUID = UUID.randomUUID(),
    intervention: Intervention = createIntervention(),
    createdBy: AuthUser = createPPUser(),
    createdAt: OffsetDateTime = OffsetDateTime.now(),
    serviceUserCRN: String = "X123456",
    selectedServiceCategories: MutableSet<ServiceCategory>? = null,
    personCurrentLocationType: PersonCurrentLocationType? = null,
    personCustodyPrisonId: String? = null,
    expectedReleaseDate: LocalDate? = null,
    ndeliusPPName: String? = "Bob",
    ndeliusPPEmailAddress: String? = "bob@example.com",
    ndeliusPDU: String? = "Hackney and City",
    ppName: String? = "Alice",
    ppEmailAddress: String? = "alice@example.com",
    ppProbationOffice: String? = "London",
    ppPdu: String? = "East Sussex",
    hasValidDeliusPPDetails: Boolean = false,
    isReferralReleasingIn12Weeks: Boolean = false,
    hasMainPointOfContactDetails: Boolean = false,
    roleOrJobTitle: String? = "Probation Practitioner",
    ppEstablishment: String? = "aaa",
    alreadyKnowPrisonName: Boolean? = null,
    expectedProbationOffice: String? = "London",
    reasonForReferralCreationBeforeAllocation: String? = "for quick assessment",
  ): DraftReferral = draftReferralRepository.save(
    referralFactory.createDraft(
      id = id,
      intervention = intervention,
      createdAt = createdAt,
      createdBy = createdBy,
      serviceUserCRN = serviceUserCRN,
      selectedServiceCategories = selectedServiceCategories,
      completionDeadline = LocalDate.now(),
      personCurrentLocationType = personCurrentLocationType,
      personCustodyPrisonId = personCustodyPrisonId,
      expectedReleaseDate = expectedReleaseDate,
      ndeliusPPName = ndeliusPPName,
      ndeliusPPEmailAddress = ndeliusPPEmailAddress,
      ndeliusPDU = ndeliusPDU,
      ppName = ppName,
      ppEmailAddress = ppEmailAddress,
      ppProbationOffice = ppProbationOffice,
      ppPdu = ppPdu,
      hasValidDeliusPPDetails = hasValidDeliusPPDetails,
      hasMainPointOfContactDetails = hasMainPointOfContactDetails,
      roleOrJobTitle = roleOrJobTitle,
      isReferralReleasingIn12Weeks = isReferralReleasingIn12Weeks,
      ppEstablishment = ppEstablishment,
      alreadyKnowPrisonName = alreadyKnowPrisonName,
      expectedProbationOffice = expectedProbationOffice,
      reasonForReferralCreationBeforeAllocation = reasonForReferralCreationBeforeAllocation,
    ),
  )

  fun createChangeLogEntry(
    changedAt: OffsetDateTime,
    referralId: UUID,
  ): Changelog {
    val user = createPPUserSecond()
    val referral = referralRepository.save(referralFactory.createSent(id = referralId, createdBy = user, sentBy = user, intervention = createIntervention()))
    return changeLogRepository.save(
      changelogFactory.create(referralId = referral.id, changedAt = changedAt, newVal = ReferralAmendmentDetails(emptyList()), oldVal = ReferralAmendmentDetails(emptyList()), changedBy = user),
    )
  }

  fun createEndedReferral(
    id: UUID = UUID.randomUUID(),
    intervention: Intervention = createIntervention(),
    endRequestedReason: CancellationReason? = randomCancellationReason(),
    endRequestedComments: String? = null,
    personCurrentLocationType: PersonCurrentLocationType? = null,
    personCustodyPrisonId: String? = null,
    expectedReleaseDate: LocalDate? = null,
    ndeliusPPName: String? = "Bob",
    ndeliusPPEmailAddress: String? = "bob@example.com",
    ndeliusPDU: String? = "Hackney and City",
    ppName: String? = "Alice",
    ppEmailAddress: String? = "alice@example.com",
    ppProbationOffice: String? = "London",
    ppPdu: String? = "East Sussex",
    ndeliusPhoneNumber: String? = "073232324232",
    ndeliusTeamPhoneNumber: String? = "020-32352323213",
    ppPhoneNumber: String? = "073232324233",
    ppTeamTelephoneNumber: String? = "020-32352323212",
    hasValidDeliusPPDetails: Boolean = false,
    isReferralReleasingIn12Weeks: Boolean = false,
    hasMainPointOfContactDetails: Boolean = false,
    roleOrJobTitle: String? = "Probation Practitioner",
    ppEstablishment: String? = "London",
  ): Referral {
    val ppUser = createPPUser()
    val spUser = createSPUser()
    draftReferralRepository.save(
      referralFactory.createDraft(
        id = id,
        createdBy = ppUser,
        intervention = intervention,
        personCurrentLocationType = personCurrentLocationType,
        personCustodyPrisonId = personCustodyPrisonId,
        expectedReleaseDate = expectedReleaseDate,
        ndeliusPPName = ndeliusPPName,
        ndeliusPPEmailAddress = ndeliusPPEmailAddress,
        ndeliusPDU = ndeliusPDU,
        ppName = ppName,
        ppEmailAddress = ppEmailAddress,
        ppProbationOffice = ppProbationOffice,
        ppPdu = ppPdu,
        hasValidDeliusPPDetails = hasValidDeliusPPDetails,
        isReferralReleasingIn12Weeks = isReferralReleasingIn12Weeks,
        hasMainPointOfContactDetails = hasMainPointOfContactDetails,
        roleOrJobTitle = roleOrJobTitle,
        ppEstablishment = ppEstablishment,
        ndeliusPhoneNumber = ndeliusPhoneNumber,
        ndeliusTeamPhoneNumber = ndeliusTeamPhoneNumber,
        ppPhoneNumber = ppPhoneNumber,
        ppTeamTelephoneNumber = ppTeamTelephoneNumber,
      ),
    )

    val endedReferral = referralRepository.save(
      referralFactory.createEnded(
        id = id,
        intervention = intervention,
        createdBy = ppUser,
        sentBy = ppUser,
        endRequestedBy = ppUser,
        assignments = listOf(
          ReferralAssignment(OffsetDateTime.now(), spUser, spUser),
        ),
        endRequestedReason = endRequestedReason,
        endRequestedComments = endRequestedComments,
        ndeliusPPName = ndeliusPPName,
        ndeliusPPEmailAddress = ndeliusPPEmailAddress,
        ndeliusPDU = ndeliusPDU,
        ppName = ppName,
        ppEmailAddress = ppEmailAddress,
        ppProbationOffice = ppProbationOffice,
        ppPdu = ppPdu,
      ),
    )
    val probationPractitionerDetails = probationPractitionerDetailsRepository.save(
      probationPractitionerDetailsFactory.create(
        referral = endedReferral,
        nDeliusName = ndeliusPPName,
        nDeliusEmailAddress = ndeliusPPEmailAddress,
        nDeliusPdu = ndeliusPDU,
        name = ppName,
        emailAddress = ppEmailAddress,
        pdu = ppPdu,
        probationOffice = ppProbationOffice,
      ),
    )
    endedReferral.probationPractitionerDetails = probationPractitionerDetails
    return referralRepository.save(endedReferral)
  }

  fun createCancelledReferral(id: UUID = UUID.randomUUID(), intervention: Intervention = createIntervention(), endRequestedReason: CancellationReason? = randomCancellationReason(), endRequestedComments: String? = null): Referral {
    val ppUser = createPPUser()
    val spUser = createSPUser()
    draftReferralRepository.save(
      referralFactory.createDraft(
        id = id,
        createdBy = ppUser,
        intervention = intervention,
      ),
    )
    return referralRepository.save(referralFactory.createEnded(id = id, intervention = intervention, createdBy = ppUser, sentBy = ppUser, endRequestedBy = ppUser, assignments = listOf(ReferralAssignment(OffsetDateTime.now(), spUser, spUser)), endRequestedReason = endRequestedReason, endRequestedComments = endRequestedComments, concludedAt = OffsetDateTime.now()))
  }

  fun createCompletedReferral(id: UUID = UUID.randomUUID(), intervention: Intervention = createIntervention(), endRequestedReason: CancellationReason? = randomCancellationReason(), endRequestedComments: String? = null): Referral {
    val ppUser = createPPUser()
    val spUser = createSPUser()
    draftReferralRepository.save(
      referralFactory.createDraft(
        id = id,
        createdBy = ppUser,
        intervention = intervention,
      ),
    )
    val referral = referralRepository.save(referralFactory.createEnded(id = id, intervention = intervention, createdBy = ppUser, sentBy = ppUser, endRequestedBy = ppUser, assignments = listOf(ReferralAssignment(OffsetDateTime.now(), spUser, spUser)), endRequestedReason = endRequestedReason, endRequestedComments = endRequestedComments, concludedAt = OffsetDateTime.now()))
    createEndOfServiceReport(referral = referral)
    return referral
  }

  fun createSentReferral(
    id: UUID = UUID.randomUUID(),
    intervention: Intervention = createIntervention(),
    ppUser: AuthUser = createPPUser(),
    sentAt: OffsetDateTime = OffsetDateTime.now(),
    completionDeadline: LocalDate = LocalDate.now(),
    needsInterpreter: Boolean? = null,
    interpreterLanguage: String? = null,
    personCurrentLocationType: PersonCurrentLocationType = PersonCurrentLocationType.CUSTODY,
    personCustodyPrisonId: String? = null,
    expectedReleaseDate: LocalDate? = null,
    probationPractitionerDetails: ProbationPractitionerDetails? = null,
    relevantSentenceEndDate: LocalDate? = null,
    status: Status? = null,
  ): Referral {
    createDraftReferral(
      id = id,
      intervention = intervention,
      createdBy = ppUser,
      personCurrentLocationType = personCurrentLocationType,
      personCustodyPrisonId = personCustodyPrisonId,
      expectedReleaseDate = expectedReleaseDate,
      alreadyKnowPrisonName = null,
    )
    var referral = referralFactory.createSent(
      id = id,
      intervention = intervention,
      createdBy = ppUser,
      sentBy = ppUser,
      sentAt = sentAt,
      completionDeadline = completionDeadline,
      needsInterpreter = needsInterpreter,
      interpreterLanguage = interpreterLanguage,
      probationPractitionerDetails = probationPractitionerDetails,
      relevantSentenceEndDate = relevantSentenceEndDate,
      status = status,
    )

    referral = referralRepository.save(
      referral,
    )
    val updatedProbationPractitionerDetails = probationPractitionerDetailsFactory.create(referral = referral)
    probationPractitionerDetailsRepository.save(updatedProbationPractitionerDetails)
    referral.probationPractitionerDetails = updatedProbationPractitionerDetails
    referral.supplierAssessment = createSupplierAssessment(referral = referral)
    referralRepository.save(referral)
    return referral
  }

  fun addSupplierAssessmentAppointment(
    supplierAssessment: SupplierAssessment,
    referral: Referral = createSentReferral(),
    appointmentDeliveryType: AppointmentDeliveryType,
    appointmentDeliveryAddress: AddressDTO? = null,
    attended: Attended? = null,
    additionalAttendanceInformation: String? = null,
    sessionSummary: String? = null,
    sessionResponse: String? = null,
    sessionConcerns: String? = null,
    notifyPPOfAttendanceBehaviour: Boolean? = null,
    appointmentTime: OffsetDateTime = OffsetDateTime.now().plusMonths(2),
    appointmentFeedbackSubmittedAt: OffsetDateTime? = null,
  ) {
    val appointment: Appointment = appointmentFactory.create(createdBy = createPPUser(), referral = referral, appointmentTime = appointmentTime)
    if (attended !== null) {
      appointment.attended = attended
      appointment.additionalAttendanceInformation = additionalAttendanceInformation
      appointment.attendanceSubmittedAt = OffsetDateTime.now()
      appointment.attendanceSubmittedBy = createSPUser()
    }

    if (sessionSummary !== null && sessionResponse !== null) {
      appointment.sessionSummary = sessionSummary
      appointment.sessionResponse = sessionResponse
      appointment.sessionConcerns = sessionConcerns
      appointment.notifyPPOfAttendanceBehaviour = notifyPPOfAttendanceBehaviour
      appointment.sessionFeedbackSubmittedAt = OffsetDateTime.now()
      appointment.sessionFeedbackSubmittedBy = createSPUser()
      appointment.appointmentFeedbackSubmittedAt = appointmentFeedbackSubmittedAt
    }

    appointmentRepository.save(appointment)
    supplierAssessment.appointments.add(appointment)
    supplierAssessmentRepository.saveAndFlush(supplierAssessment)
    val appointmentDelivery = appointmentDeliveryFactory.create(appointmentId = appointment.id, appointmentDeliveryType = appointmentDeliveryType)
    appointment.appointmentDelivery = appointmentDeliveryRepository.saveAndFlush(appointmentDelivery)
    if (appointmentDeliveryAddress != null) {
      val updatedAppointmentDeliveryAddress = appointmentDeliveryAddressFactory.create(
        appointmentDeliveryId = appointmentDelivery.appointmentId,
        firstAddressLine = appointmentDeliveryAddress.firstAddressLine,
        secondAddressLine = appointmentDeliveryAddress.secondAddressLine,
        townCity = appointmentDeliveryAddress.townOrCity,
        county = appointmentDeliveryAddress.county,
        postCode = appointmentDeliveryAddress.postCode,
      )
      appointmentDelivery.appointmentDeliveryAddress = appointmentDeliveryAddressRepository.save(updatedAppointmentDeliveryAddress)
    }
  }

  fun createSupplierAssessment(
    id: UUID = UUID.randomUUID(),
    referral: Referral = referralRepository.save(
      referralFactory.createSent(
        createdBy = createPPUser(),
        sentBy = createPPUser(),
        intervention = createIntervention(),
      ),
    ),
  ): SupplierAssessment = supplierAssessmentRepository.save(
    SupplierAssessment(
      id = id,
      referral = referral,
    ),
  )

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
    }.toMutableSet()

    val contract = dynamicFrameworkContractFactory.create(
      id = id,
      contractType = contractType,
      contractReference = contractReference,
      primeProvider = primeProvider,
      subcontractorProviders = serviceProviders,
      npsRegion = npsRegion,
    )
    return dynamicFrameworkContractRepository.save(contract)
  }

  fun createAssignedReferral(id: UUID = UUID.randomUUID(), intervention: Intervention? = null): Referral {
    val updatedIntervention = intervention ?: createIntervention()
    val ppUser = createPPUser()
    val spUser = createSPUser()
    createDraftReferral(
      id = id,
      intervention = updatedIntervention,
      createdBy = ppUser,
      alreadyKnowPrisonName = null,
    )
    val referral = referralRepository.save(
      referralFactory.createSent(
        id = id,
        intervention = updatedIntervention,
        createdBy = ppUser,
        sentBy = ppUser,
        assignments = listOf(ReferralAssignment(OffsetDateTime.now(), spUser, spUser)),
        supplierAssessment = supplierAssessmentFactory.createWithNoAppointment(),
        completionDeadline = LocalDate.now(),
      ),
    )
    val probationPractitionerDetails = probationPractitionerDetailsFactory.create(referral = referral)
    probationPractitionerDetailsRepository.save(probationPractitionerDetails)
    referral.probationPractitionerDetails = probationPractitionerDetails

    return referralRepository.saveAndFlush(referral)
  }

  fun createAssignedReferral(
    id: UUID = UUID.randomUUID(),
    interventionTitle: String,
    serviceProviderId: String,
    sentAt: OffsetDateTime,
    referenceNumber: String,
    assignedToUsername: String? = null,
    serviceUserFirstName: String? = null,
    serviceUserLastName: String,
  ): Referral {
    val contractType = contractTypes["ACC"]!!
    val region = npsRegions['C']!!
    val primeProvider = serviceProviderRepository.save(serviceProviderFactory.create(id = serviceProviderId))
    val dynamicFrameworkContract = createDynamicFrameworkContract(
      contractReference = "PACT_TEST",
      contractType = contractType,
      primeProviderId = primeProvider.id,
      npsRegion = region,
    )
    val intervention = createIntervention(interventionTitle = interventionTitle, serviceProviderId = serviceProviderId, dynamicFrameworkContract = dynamicFrameworkContract)
    val ppUser = createPPUser()
    val spUser = if (assignedToUsername != null) createSPUser(assignedToUsername) else null

    val draftReferral = createDraftReferral(
      id = id,
      intervention = intervention,
      createdBy = ppUser,
      alreadyKnowPrisonName = null,
    )

    val referral = referralRepository.save(
      referralFactory.createSent(
        id = id,
        intervention = intervention,
        createdBy = ppUser,
        sentBy = ppUser,
        assignments = if (spUser != null) listOf(ReferralAssignment(OffsetDateTime.now(), spUser, spUser)) else listOf(),
        supplierAssessment = supplierAssessmentFactory.createWithNoAppointment(),
        sentAt = sentAt,
        referenceNumber = referenceNumber,
      ),
    )
    val probationPractitionerDetails = probationPractitionerDetailsFactory.create(referral = referral)
    probationPractitionerDetailsRepository.save(probationPractitionerDetails)
    referral.probationPractitionerDetails = probationPractitionerDetails
    val serviceUser = serviceUserFactory.create(firstName = serviceUserFirstName, lastName = serviceUserLastName, referral = draftReferral)
    val referralServiceUser = referralServiceUserFactory.create(firstName = serviceUserFirstName, lastName = serviceUserLastName, referral = referral)
    draftReferral.serviceUserData = serviceUser
    referral.serviceUserData = referralServiceUser
    draftReferralRepository.save(draftReferral)
    referralRepository.save(referral)
    return referral
  }

  fun createActionPlan(
    referral: Referral = createSentReferral(),
    id: UUID = UUID.randomUUID(),
    numberOfSessions: Int? = null,
    activities: MutableList<ActionPlanActivity> = mutableListOf(),
    createdAt: OffsetDateTime = OffsetDateTime.now(),
    createdBy: AuthUser = createSPUser(),
    submittedAt: OffsetDateTime? = null,
    submittedBy: AuthUser? = null,
    approvedAt: OffsetDateTime? = null,
    approvedBy: AuthUser? = null,
  ): ActionPlan = actionPlanRepository.save(
    ActionPlan(
      id = id,
      createdAt = createdAt,
      createdBy = createdBy,
      referral = referral,
      activities = activities,
      numberOfSessions = numberOfSessions,
      submittedAt = submittedAt,
      submittedBy = submittedBy,
      approvedAt = approvedAt,
      approvedBy = approvedBy,
    ),
  )

  fun createDeliverySession(
    sessionNumber: Int,
    duration: Int,
    appointmentTime: OffsetDateTime,
    attended: Attended? = null,
    attendanceFailureInformation: String? = null,
    didSessionHappen: Boolean? = null,
    sessionSummary: String? = null,
    sessionResponse: String? = null,
    sessionConcerns: String? = null,
    futureSessionPlans: String? = null,
    late: Boolean? = null,
    lateReason: String? = null,
    noAttendanceInformation: String? = null,
    noSessionReasonLogistics: String? = null,
    noSessionReasonPopAcceptable: String? = null,
    noSessionReasonPopUnacceptable: String? = null,
    noSessionReasonType: NoSessionReasonType? = null,
    sessionFeedbackSubmittedAt: OffsetDateTime? = null,
    sessionFeedbackSubmittedBy: AuthUser? = null,
    behaviour: String? = null,
    notifyPPOfBehaviour: Boolean? = null,
    appointmentFeedbackSubmittedAt: OffsetDateTime? = null,
    appointmentDeliveryType: AppointmentDeliveryType? = null,
    appointmentSessionType: AppointmentSessionType = AppointmentSessionType.ONE_TO_ONE,
    appointmentDeliveryAddress: AddressDTO? = null,
    referral: Referral = createSentReferral(),
  ): DeliverySession {
    val now = OffsetDateTime.now()
    val user = createSPUser()
    val appointment = appointmentFactory.create(
      appointmentTime = appointmentTime,
      durationInMinutes = duration,
      createdBy = user,
      createdAt = now,
      attended = attended,
      attendanceFailureInformation = attendanceFailureInformation,
      didSessionHappen = didSessionHappen,
      sessionSummary = sessionSummary,
      sessionResponse = sessionResponse,
      sessionConcerns = sessionConcerns,
      futureSessionPlans = futureSessionPlans,
      late = late,
      lateReason = lateReason,
      noAttendanceInformation = noAttendanceInformation,
      noSessionReasonLogistics = noSessionReasonLogistics,
      noSessionReasonPopAcceptable = noSessionReasonPopAcceptable,
      noSessionReasonPopUnacceptable = noSessionReasonPopUnacceptable,
      noSessionReasonType = noSessionReasonType,
      sessionFeedbackSubmittedAt = sessionFeedbackSubmittedAt,
      sessionFeedbackSubmittedBy = sessionFeedbackSubmittedBy,
      attendanceSubmittedAt = if (attended != null) now else null,
      attendanceBehaviour = behaviour,
      attendanceBehaviourSubmittedAt = if (behaviour != null) now else null,
      notifyPPOfAttendanceBehaviour = notifyPPOfBehaviour,
      appointmentFeedbackSubmittedAt = appointmentFeedbackSubmittedAt,
      referral = referral,
    )
    appointmentRepository.save(appointment)
    if (appointmentDeliveryType != null) {
      val appointmentDelivery = appointmentDeliveryFactory.create(
        appointmentId = appointment.id,
        appointmentDeliveryType = appointmentDeliveryType,
        appointmentSessionType = appointmentSessionType,
      )
      appointmentDeliveryRepository.save(appointmentDelivery)
      if (appointmentDeliveryAddress != null) {
        val updatedAppointmentDeliveryAddress = appointmentDeliveryAddressFactory.create(
          appointmentDeliveryId = appointmentDelivery.appointmentId,
          firstAddressLine = appointmentDeliveryAddress.firstAddressLine,
          secondAddressLine = appointmentDeliveryAddress.secondAddressLine,
          townCity = appointmentDeliveryAddress.townOrCity,
          county = appointmentDeliveryAddress.county,
          postCode = appointmentDeliveryAddress.postCode,
        )
        appointmentDeliveryAddressRepository.save(updatedAppointmentDeliveryAddress)
      }
    }
    val session = DeliverySession(
      id = UUID.randomUUID(),
      sessionNumber = sessionNumber,
      appointments = mutableSetOf(appointment),
      referral = referral,
    )
    return deliverySessionRepository.save(session)
  }

  fun fillReferralFields(
    referral: Referral,
    selectedServiceCategories: List<ServiceCategory> = referral.intervention.dynamicFrameworkContract.contractType.serviceCategories.toList(),
    complexityLevelIds: MutableMap<UUID, UUID>? = mutableMapOf(selectedServiceCategories[0].id to randomComplexityLevel(selectedServiceCategories[0]).id),
    desiredOutcomes: List<DesiredOutcome> = referral.intervention.dynamicFrameworkContract.contractType.serviceCategories.first().desiredOutcomes,
    serviceUserData: ServiceUserData = ServiceUserData(
      title = "Mr",
      firstName = "Alex",
      lastName = "River",
      dateOfBirth = LocalDate.of(1980, 1, 1),
      gender = "Male",
      preferredLanguage = "English",
      draftReferral = draftReferralRepository.findById(referral.id).get(),
      referralID = referral.id,
      ethnicity = "British",
      religionOrBelief = "Agnostic",
      disabilities = listOf("Autism spectrum condition"),
    ),
    referralServiceUserData: ReferralServiceUserData = ReferralServiceUserData(
      title = "Mr",
      firstName = "Alex",
      lastName = "River",
      dateOfBirth = LocalDate.of(1980, 1, 1),
      gender = "Male",
      preferredLanguage = "English",
      referral = referralRepository.findById(referral.id).get(),
      referralID = referral.id,
      ethnicity = "British",
      religionOrBelief = "Agnostic",
      disabilities = listOf("Autism spectrum condition"),
    ),
    accessibilityNeeds: String = "She uses a wheelchair",
    additionalNeedsInformation: String = "Alex is currently sleeping on her aunt's sofa",
    additionalRiskInformation: String = "A danger to the elderly",
    completionDeadline: LocalDate = LocalDate.of(2021, 4, 1),
    furtherInformation: String = "Some information about the service user",
    reasonForReferral: String = "For crs",
    reasonForReferralFurtherInformation: String = "more info",
    reasonForReferralCreationBeforeAllocation: String = "for quick assessment",
    hasAdditionalResponsibilities: Boolean = true,
    interpreterLanguage: String = "Spanish",
    maximumEnforceableDays: Int = 10,
    needsInterpreter: Boolean = true,
    relevantSentenceId: Long = 2600295124,
    whenUnavailable: String = "She works Mondays 9am - midday",
    ndeliusPPName: String? = "Bob",
    ndeliusPPEmailAddress: String? = "bob@example.com",
    ndeliusPDU: String? = "Hackney and City",
    ppName: String? = "Alice",
    ppEmailAddress: String? = "alice@example.com",
    ppProbationOffice: String? = "London",
    ppPdu: String? = "East Sussex",
    allocatedCommunityPP: Boolean? = true,
    alreadyKnowPrisonName: Boolean? = false,
    referralLocation: ReferralLocation = ReferralLocation(
      UUID.randomUUID(),
      referral = referral,
      type = PersonCurrentLocationType.CUSTODY,
      prisonId = "aaa",
      expectedReleaseDate = LocalDate.now().plusDays(1),
      expectedReleaseDateMissingReason = null,
      isReferralReleasingIn12Weeks = false,
      expectedProbationOffice = "London",
      expectedProbationOfficeUnknownReason = null,
    ),
    probationPractitionerDetails: ProbationPractitionerDetails = referral.probationPractitionerDetails ?: ProbationPractitionerDetails(
      id = UUID.randomUUID(),
      referral = referral,
      nDeliusName = ndeliusPPName,
      nDeliusEmailAddress = ndeliusPPEmailAddress,
      nDeliusPDU = ndeliusPDU,
      name = ppName,
      emailAddress = ppEmailAddress,
      pdu = ppPdu,
      probationOffice = ppProbationOffice,
    ),
  ): Referral {
    referral.selectedServiceCategories = selectedServiceCategories.toMutableSet()
    // required to satisfy foreign key constrains on desired outcomes and complexity levels
    val draftReferral = serviceUserData.draftReferral!!
    draftReferral.serviceUserData = serviceUserData
    draftReferral.expectedReleaseDate = referralLocation.expectedReleaseDate
    draftReferral.personCurrentLocationType = referralLocation.type
    draftReferral.personCustodyPrisonId = referralLocation.prisonId
    draftReferral.expectedReleaseDate = referralLocation.expectedReleaseDate
    draftReferral.ppProbationOffice = probationPractitionerDetails.probationOffice
    draftReferral.allocatedCommunityPP = allocatedCommunityPP
    draftReferral.alreadyKnowPrisonName = alreadyKnowPrisonName
    draftReferralRepository.save(draftReferral)
    referralRepository.saveAndFlush(referral)

    referral.serviceUserData = referralServiceUserData
    referral.selectedDesiredOutcomes = desiredOutcomes.map { SelectedDesiredOutcomesMapping(it.serviceCategoryId, it.id) }.toMutableList()
    referral.accessibilityNeeds = accessibilityNeeds
    referral.additionalNeedsInformation = additionalNeedsInformation
    referral.additionalRiskInformation = additionalRiskInformation
    referral.complexityLevelIds = complexityLevelIds
    referral.hasAdditionalResponsibilities = hasAdditionalResponsibilities
    referral.interpreterLanguage = interpreterLanguage
    referral.needsInterpreter = needsInterpreter
    referral.relevantSentenceId = relevantSentenceId
    referral.whenUnavailable = whenUnavailable
    referral.referralLocation = referralLocation
    referral.probationPractitionerDetails = probationPractitionerDetails
    return referralRepository.save(referral).also {
      referralLocationRepository.save(referralLocation)
      val details = referralDetailsRepository.save(
        ReferralDetails(
          UUID.randomUUID(),
          null,
          it.id,
          it.createdAt,
          it.createdBy.id,
          "initial referral details",
          completionDeadline,
          furtherInformation,
          maximumEnforceableDays,
          reasonForReferral,
          reasonForReferralFurtherInformation,
          reasonForReferralCreationBeforeAllocation,
        ),
      )
      it.referralDetails?.let { existingDetails ->
        existingDetails.supersededById = details.id
        referralDetailsRepository.save(existingDetails)
      }
    }
  }

  fun fillDraftReferralFields(
    referral: DraftReferral,
    selectedServiceCategories: List<ServiceCategory> = referral.intervention.dynamicFrameworkContract.contractType.serviceCategories.toList(),
    complexityLevelIds: MutableMap<UUID, UUID>? = mutableMapOf(selectedServiceCategories[0].id to randomComplexityLevel(selectedServiceCategories[0]).id),
    desiredOutcomes: List<DesiredOutcome> = referral.intervention.dynamicFrameworkContract.contractType.serviceCategories.first().desiredOutcomes,
    serviceUserData: ServiceUserData = ServiceUserData(
      title = "Mr",
      firstName = "Alex",
      lastName = "River",
      dateOfBirth = LocalDate.of(1980, 1, 1),
      gender = "Male",
      preferredLanguage = "English",
      referralID = referral.id,
      ethnicity = "British",
      religionOrBelief = "Agnostic",
      draftReferral = referral,
      disabilities = listOf("Autism spectrum condition"),
    ),
    accessibilityNeeds: String = "She uses a wheelchair",
    additionalNeedsInformation: String = "Alex is currently sleeping on her aunt's sofa",
    additionalRiskInformation: String = "A danger to the elderly",
    additionalRiskInformationUpdatedAt: OffsetDateTime = OffsetDateTime.now(),
    completionDeadline: LocalDate = LocalDate.of(2021, 4, 1),
    furtherInformation: String = "Some information about the service user",
    hasAdditionalResponsibilities: Boolean = true,
    reasonForReferral: String = "for crs",
    reasonForReferralFurtherInformation: String = "more info",
    reasonForReferralCreationBeforeAllocation: String? = "for quick assessment",
    interpreterLanguage: String = "Spanish",
    maximumEnforceableDays: Int = 10,
    needsInterpreter: Boolean = true,
    relevantSentenceId: Long = 2600295124,
    whenUnavailable: String = "She works Mondays 9am - midday",
    personCurrentLocationType: PersonCurrentLocationType? = PersonCurrentLocationType.CUSTODY,
    personCustodyPrisonId: String? = "test",
    expectedReleaseDate: LocalDate? = LocalDate.of(2050, 11, 1),
    probationOffice: String? = "probation-office",
  ): DraftReferral {
    referral.selectedServiceCategories = selectedServiceCategories.toMutableSet()
    // required to satisfy foreign key constrains on desired outcomes and complexity levels
    draftReferralRepository.saveAndFlush(referral)

    referral.serviceUserData = serviceUserData
    referral.selectedDesiredOutcomes = desiredOutcomes.map { SelectedDesiredOutcomesMapping(it.serviceCategoryId, it.id) }.toMutableList()
    referral.accessibilityNeeds = accessibilityNeeds
    referral.additionalNeedsInformation = additionalNeedsInformation
    referral.additionalRiskInformation = additionalRiskInformation
    referral.additionalRiskInformationUpdatedAt = additionalRiskInformationUpdatedAt
    referral.complexityLevelIds = complexityLevelIds
    referral.hasAdditionalResponsibilities = hasAdditionalResponsibilities
    referral.interpreterLanguage = interpreterLanguage
    referral.needsInterpreter = needsInterpreter
    referral.relevantSentenceId = relevantSentenceId
    referral.whenUnavailable = whenUnavailable
    referral.personCurrentLocationType = personCurrentLocationType
    referral.personCustodyPrisonId = personCustodyPrisonId
    referral.expectedReleaseDate = expectedReleaseDate
    referral.ppProbationOffice = probationOffice

    return draftReferralRepository.save(referral).also {
      val details = referralDetailsRepository.save(
        ReferralDetails(
          UUID.randomUUID(),
          null,
          it.id,
          it.createdAt,
          it.createdBy.id,
          "initial referral details",
          completionDeadline,
          furtherInformation,
          maximumEnforceableDays,
          reasonForReferral,
          reasonForReferralFurtherInformation,
          reasonForReferralCreationBeforeAllocation,
        ),
      )
      it.referralDetails?.let { existingDetails ->
        existingDetails.supersededById = details.id
        referralDetailsRepository.save(existingDetails)
      }
    }
  }

  fun createEndOfServiceReport(id: UUID = UUID.randomUUID(), referral: Referral = createAssignedReferral()): EndOfServiceReport {
    val eosr = endOfServiceReportRepository.save(
      endOfServiceReportFactory.create(id = id, referral = referral, createdBy = referral.currentAssignee!!),
    )
    referral.endOfServiceReport = eosr
    return eosr
  }

  fun addEndOfServiceReportWithOutcome(id: UUID = UUID.randomUUID(), referral: Referral) {
    val desiredOutcomeId = referral.selectedDesiredOutcomes?.first()?.desiredOutcomeId
    var outcomes = mutableSetOf<EndOfServiceReportOutcome>()
    if (desiredOutcomeId != null) {
      outcomes = mutableSetOf(endOfServiceReportOutcomeFactory.create(getDesiredOutcome(desiredOutcomeId)))
    }

    val endOfServiceReport = endOfServiceReportRepository.save(
      endOfServiceReportFactory.create(id = id, referral = referral, createdBy = referral.createdBy, outcomes = outcomes),
    )
    referral.endOfServiceReport = endOfServiceReport
  }

  fun createCaseNote(
    referral: Referral,
    subject: String,
    body: String,
    id: UUID = UUID.randomUUID(),
    authUser: AuthUser = createSPUser(),
  ): CaseNote {
    val caseNote = caseNoteFactory.create(id = id, referral = referral, subject = subject, body = body, sentBy = authUser)
    return caseNoteRepository.save(caseNote)
  }

  fun createAppointment(
    referral: Referral,
    id: UUID = UUID.randomUUID(),
    authUser: AuthUser = createSPUser(),
    superseded: Boolean = false,
  ): Appointment {
    val deliverySession = deliverySessionRepository.findByReferralIdAndSessionNumber(referral.id, 1)

    val appointment = appointmentFactory.create(
      id,
      createdAt = OffsetDateTime.now(),
      authUser,
      OffsetDateTime.now().plusMonths(2),
      superseded = superseded,
      referral = referral,
    )

    if (deliverySession != null) {
      deliverySession.appointments.add(appointment)
    }

    appointmentRepository.save(appointment)
    if (deliverySession != null) {
      deliverySessionRepository.save(deliverySession)
    }
    return appointment
  }
  fun changeAppointmentId(targetAppointment: Appointment, newId: UUID, deliverySession: DeliverySession) {
    targetAppointment.id = newId
    appointmentRepository.save(targetAppointment)
    deliverySessionRepository.save(deliverySession)
  }
  fun setSuperseded(supersededAppointment: Appointment, deliverySession: DeliverySession, superseded: Boolean = true) {
    supersededAppointment.superseded = superseded
    appointmentRepository.save(supersededAppointment)
    deliverySessionRepository.save(deliverySession)
  }
}
