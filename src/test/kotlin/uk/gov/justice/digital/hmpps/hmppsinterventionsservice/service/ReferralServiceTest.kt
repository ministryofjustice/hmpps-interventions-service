package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.mockito.AdditionalAnswers
import org.mockito.ArgumentMatchers.anyList
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.data.jpa.domain.Specification
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.reactive.function.client.WebClientResponseException
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.authorization.ReferralAccessChecker
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.authorization.ReferralAccessFilter
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.authorization.ServiceProviderAccessScope
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.authorization.ServiceProviderAccessScopeMapper
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.authorization.ServiceUserAccessChecker
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.authorization.UserTypeChecker
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.config.ValidationError
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.DraftReferralDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.ReferralEventPublisher
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AuthUser
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.DesiredOutcome
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Intervention
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Referral
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ReferralAssignment
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.SampleData
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ServiceProvider
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ActionPlanRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.AuthUserRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.CancellationReasonRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.DeliverySessionRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.EndOfServiceReportRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.InterventionRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ReferralRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ServiceCategoryRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ActionPlanFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.AppointmentFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.AuthUserFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.CancellationReasonFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ContractTypeFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.DynamicFrameworkContractFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.EndOfServiceReportFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.InterventionFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ReferralFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.RepositoryTest
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ServiceCategoryFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ServiceProviderFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ServiceUserFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.SupplierAssessmentFactory
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

@RepositoryTest
class ReferralServiceTest @Autowired constructor(
  val entityManager: TestEntityManager,
  val referralRepository: ReferralRepository,
  val authUserRepository: AuthUserRepository,
  val interventionRepository: InterventionRepository,
  val cancellationReasonRepository: CancellationReasonRepository,
  val deliverySessionRepository: DeliverySessionRepository,
  val actionPlanRepository: ActionPlanRepository,
  val endOfServiceReportRepository: EndOfServiceReportRepository,
  val serviceCategoryRepository: ServiceCategoryRepository,
) {

  private val userFactory = AuthUserFactory(entityManager)
  private val serviceUserFactory = ServiceUserFactory(entityManager)
  private val interventionFactory = InterventionFactory(entityManager)
  private val contractFactory = DynamicFrameworkContractFactory(entityManager)
  private val serviceProviderFactory = ServiceProviderFactory(entityManager)
  private val referralFactory = ReferralFactory(entityManager)
  private val serviceCategoryFactory = ServiceCategoryFactory(entityManager)
  private val contractTypeFactory = ContractTypeFactory(entityManager)
  private val dynamicFrameworkContractFactory = DynamicFrameworkContractFactory(entityManager)
  private val cancellationReasonFactory = CancellationReasonFactory(entityManager)
  private val endOfServiceReportFactory = EndOfServiceReportFactory(entityManager)
  private val actionPlanFactory = ActionPlanFactory(entityManager)
  private val appointmentFactory = AppointmentFactory(entityManager)
  private val supplierAssessmentFactory = SupplierAssessmentFactory(entityManager)

  private val referralEventPublisher: ReferralEventPublisher = mock()
  private val referenceGenerator: ReferralReferenceGenerator = spy(ReferralReferenceGenerator())
  private val referralConcluder: ReferralConcluder = mock()
  private val referralAccessChecker: ReferralAccessChecker = mock()
  private val userTypeChecker = UserTypeChecker()
  private val serviceProviderAccessScopeMapper: ServiceProviderAccessScopeMapper = mock()
  private val referralAccessFilter: ReferralAccessFilter = mock()
  private val communityAPIReferralService: CommunityAPIReferralService = mock()
  private val serviceUserAccessChecker: ServiceUserAccessChecker = mock()
  private val authentication: JwtAuthenticationToken = mock()
  private val assessRisksAndNeedsService: RisksAndNeedsService = mock()
  private val communityAPIOffenderService: CommunityAPIOffenderService = mock()
  private val supplierAssessmentService: SupplierAssessmentService = mock()
  private val hmppsAuthService: HMPPSAuthService = mock()
  private val telemetryService: TelemetryService = mock()
  private val draftOasysRiskInformationService: DraftOasysRiskInformationService = mock()

  private val referralService = ReferralService(
    referralRepository,
    authUserRepository,
    interventionRepository,
    referralConcluder,
    referralEventPublisher,
    referenceGenerator,
    cancellationReasonRepository,
    deliverySessionRepository,
    serviceCategoryRepository,
    referralAccessChecker,
    userTypeChecker,
    serviceProviderAccessScopeMapper,
    referralAccessFilter,
    communityAPIReferralService,
    serviceUserAccessChecker,
    assessRisksAndNeedsService,
    communityAPIOffenderService,
    supplierAssessmentService,
    hmppsAuthService,
    telemetryService,
    draftOasysRiskInformationService
  )

  // reset before each test
  private lateinit var sampleReferral: Referral
  private lateinit var sampleIntervention: Intervention
  private lateinit var sampleCohortIntervention: Intervention

  @BeforeEach
  fun beforeEach() {
    sampleReferral = SampleData.persistReferral(
      entityManager,
      SampleData.sampleReferral("X123456", "Harmony Living")
    )
    sampleIntervention = sampleReferral.intervention

    sampleCohortIntervention = SampleData.persistIntervention(
      entityManager,
      SampleData.sampleIntervention(
        dynamicFrameworkContract = SampleData.sampleContract(
          primeProvider = sampleReferral.intervention.dynamicFrameworkContract.primeProvider,
          contractType = SampleData.sampleContractType(
            name = "Personal Wellbeing",
            code = "PWB",
            serviceCategories = mutableSetOf(
              SampleData.sampleServiceCategory(),
              SampleData.sampleServiceCategory(name = "Social inclusion")
            )
          )
        )
      )
    )
  }

  @Test
  fun `assignment to a user adds a new assignment record to the trail of assignments`() {
    val r = referralFactory.createSent()
    val assigner = userFactory.create(id = "1000", userName = "test-admin@example.org")
    val assignedTo1 = userFactory.create(id = "5000", userName = "assignee1@example.org")
    val assignedTo2 = userFactory.create(id = "5001", userName = "assignee2@example.org")

    referralService.assignSentReferral(r, assigner, assignedTo1)
    referralService.assignSentReferral(r, assigner, assignedTo2)

    with(r.assignments[0]) {
      assertThat(this.assignedBy).isEqualTo(assigner)
      assertThat(this.assignedTo).isEqualTo(assignedTo1)
    }
    with(r.assignments[1]) {
      assertThat(this.assignedAt).isAfterOrEqualTo(r.assignments[0].assignedAt)
      assertThat(this.assignedBy).isEqualTo(assigner)
      assertThat(this.assignedTo).isEqualTo(assignedTo2)
    }
  }

  @Test
  fun `assignment to a user marks older records superseded`() {
    val r = referralFactory.createSent()
    val assigner = userFactory.create(id = "1000", userName = "test-admin@example.org")
    val assignedTo1 = userFactory.create(id = "5000", userName = "assignee1@example.org")
    val assignedTo2 = userFactory.create(id = "5001", userName = "assignee2@example.org")
    val assignedTo3 = userFactory.create(id = "5002", userName = "assignee3@example.org")

    referralService.assignSentReferral(r, assigner, assignedTo1)
    referralService.assignSentReferral(r, assigner, assignedTo2)
    referralService.assignSentReferral(r, assigner, assignedTo3)

    assertThat(r.assignments.map { it.superseded })
      .containsExactly(true, true, false)
  }

  @Test
  fun `update cannot overwrite identifier fields`() {
    val draftReferral = DraftReferralDTO(
      id = UUID.fromString("ce364949-7301-497b-894d-130f34a98bff"),
      createdAt = OffsetDateTime.of(LocalDate.of(2020, 12, 1), LocalTime.MIN, ZoneOffset.UTC)
    )

    val updated = referralService.updateDraftReferral(sampleReferral, draftReferral)
    assertThat(updated.id).isEqualTo(sampleReferral.id)
    assertThat(updated.createdAt).isEqualTo(sampleReferral.createdAt)
  }

  @Test
  fun `null fields in the update do not overwrite original fields`() {
    sampleReferral.completionDeadline = LocalDate.of(2021, 6, 26)
    entityManager.persistAndFlush(sampleReferral)

    val draftReferral = DraftReferralDTO(completionDeadline = null)

    val updated = referralService.updateDraftReferral(sampleReferral, draftReferral)
    assertThat(updated.completionDeadline).isEqualTo(LocalDate.of(2021, 6, 26))
  }

  @Test
  fun `non-null fields in the update overwrite original fields`() {
    sampleReferral.completionDeadline = LocalDate.of(2021, 6, 26)
    entityManager.persistAndFlush(sampleReferral)

    val today = LocalDate.now()
    val draftReferral = DraftReferralDTO(completionDeadline = today)

    val updated = referralService.updateDraftReferral(sampleReferral, draftReferral)
    assertThat(updated.completionDeadline).isEqualTo(today)
  }

  @Test
  fun `update mutates the original object`() {
    sampleReferral.completionDeadline = LocalDate.of(2021, 6, 26)
    entityManager.persistAndFlush(sampleReferral)

    val today = LocalDate.now()
    val draftReferral = DraftReferralDTO(completionDeadline = today)

    val updated = referralService.updateDraftReferral(sampleReferral, draftReferral)
    assertThat(updated.completionDeadline).isEqualTo(today)
  }

  @Test
  fun `update successfully persists the updated draft referral`() {
    sampleReferral.completionDeadline = LocalDate.of(2021, 6, 26)
    entityManager.persistAndFlush(sampleReferral)

    val today = LocalDate.now()
    val draftReferral = DraftReferralDTO(completionDeadline = today)
    referralService.updateDraftReferral(sampleReferral, draftReferral)
    val savedDraftReferral = referralService.getDraftReferralForUser(sampleReferral.id, userFactory.create())
    assertThat(savedDraftReferral!!.id).isEqualTo(sampleReferral.id)
    assertThat(savedDraftReferral.createdAt).isEqualTo(sampleReferral.createdAt)
    assertThat(savedDraftReferral.completionDeadline).isEqualTo(draftReferral.completionDeadline)
  }

  @Test
  fun `create and persist non-cohort draft referral`() {
    val authUser = AuthUser("user_id", "delius", "user_name")
    val draftReferral = referralService.createDraftReferral(authUser, "X123456", sampleIntervention.id)
    entityManager.flush()

    val savedDraftReferral = referralService.getDraftReferralForUser(draftReferral.id, authUser)
    assertThat(savedDraftReferral!!.id).isNotNull
    assertThat(savedDraftReferral.createdAt).isNotNull
    assertThat(savedDraftReferral.createdBy).isEqualTo(authUser)
    assertThat(savedDraftReferral.serviceUserCRN).isEqualTo("X123456")
    assertThat(savedDraftReferral.selectedServiceCategories).hasSize(1)
    assertThat(savedDraftReferral.selectedServiceCategories!!.elementAt(0).id).isEqualTo(sampleIntervention.dynamicFrameworkContract.contractType.serviceCategories.elementAt(0).id)
  }

  @Test
  fun `create and persist cohort draft referral`() {
    val authUser = AuthUser("user_id", "delius", "user_name")
    val draftReferral = referralService.createDraftReferral(authUser, "X123456", sampleCohortIntervention.id)
    entityManager.flush()

    val savedDraftReferral = referralService.getDraftReferralForUser(draftReferral.id, authUser)
    assertThat(savedDraftReferral!!.id).isNotNull
    assertThat(savedDraftReferral.createdAt).isNotNull
    assertThat(savedDraftReferral.createdBy).isEqualTo(authUser)
    assertThat(savedDraftReferral.serviceUserCRN).isEqualTo("X123456")
    assertThat(savedDraftReferral.selectedServiceCategories).isNull()
  }

  @Test
  fun `get a draft referral`() {
    sampleReferral.completionDeadline = LocalDate.of(2021, 6, 26)
    entityManager.persistAndFlush(sampleReferral)

    val savedDraftReferral = referralService.getDraftReferralForUser(sampleReferral.id, userFactory.create())
    assertThat(savedDraftReferral!!.id).isEqualTo(sampleReferral.id)
    assertThat(savedDraftReferral.createdAt).isEqualTo(sampleReferral.createdAt)
    assertThat(savedDraftReferral.completionDeadline).isEqualTo(sampleReferral.completionDeadline)
  }

  @Test
  fun `find by userID returns list of draft referrals`() {
    val user1 = AuthUser("123", "delius", "bernie.b")
    val user2 = AuthUser("456", "delius", "sheila.h")
    val user3 = AuthUser("789", "delius", "tom.myers")
    referralService.createDraftReferral(user1, "X123456", sampleIntervention.id)
    referralService.createDraftReferral(user1, "X123456", sampleIntervention.id)
    referralService.createDraftReferral(user2, "X123456", sampleIntervention.id)
    entityManager.flush()

    whenever(referralAccessFilter.probationPractitionerReferrals(anyList(), any())).then(AdditionalAnswers.returnsFirstArg<List<Referral>>())

    val single = referralService.getDraftReferralsForUser(user2)
    assertThat(single).hasSize(1)

    val multiple = referralService.getDraftReferralsForUser(user1)
    assertThat(multiple).hasSize(2)

    val none = referralService.getDraftReferralsForUser(user3)
    assertThat(none).hasSize(0)
  }

  @Test
  fun `completion date must be in the future`() {
    val update = DraftReferralDTO(completionDeadline = LocalDate.of(2020, 1, 1))
    val error = assertThrows<ValidationError> {
      referralService.updateDraftReferral(sampleReferral, update)
    }
    assertThat(error.errors.size).isEqualTo(1)
    assertThat(error.errors[0].field).isEqualTo("completionDeadline")
  }

  @Test
  fun `when needsInterpreter is true, interpreterLanguage must be set`() {
    // this is fine
    referralService.updateDraftReferral(sampleReferral, DraftReferralDTO(needsInterpreter = false))

    val error = assertThrows<ValidationError> {
      // this throws ValidationError
      referralService.updateDraftReferral(sampleReferral, DraftReferralDTO(needsInterpreter = true))
    }
    assertThat(error.errors.size).isEqualTo(1)
    assertThat(error.errors[0].field).isEqualTo("needsInterpreter")

    // this is also fine
    referralService.updateDraftReferral(sampleReferral, DraftReferralDTO(needsInterpreter = true, interpreterLanguage = "German"))
  }

  @Test
  fun `when hasAdditionalResponsibilities is true, whenUnavailable must be set`() {
    // this is fine
    referralService.updateDraftReferral(sampleReferral, DraftReferralDTO(hasAdditionalResponsibilities = false))

    val error = assertThrows<ValidationError> {
      // this throws ValidationError
      referralService.updateDraftReferral(sampleReferral, DraftReferralDTO(hasAdditionalResponsibilities = true))
    }
    assertThat(error.errors.size).isEqualTo(1)
    assertThat(error.errors[0].field).isEqualTo("hasAdditionalResponsibilities")

    // this is also fine
    referralService.updateDraftReferral(sampleReferral, DraftReferralDTO(hasAdditionalResponsibilities = true, whenUnavailable = "wednesdays"))
  }

  @Test
  fun `multiple errors at once`() {
    val update = DraftReferralDTO(completionDeadline = LocalDate.of(2020, 1, 1), needsInterpreter = true)
    val error = assertThrows<ValidationError> {
      referralService.updateDraftReferral(sampleReferral, update)
    }
    assertThat(error.errors.size).isEqualTo(2)
  }

  @Test
  fun `the referral isn't actually updated if any of the fields contain validation errors`() {
    // any invalid fields should mean that no fields are written to the db
    val update = DraftReferralDTO(
      // valid field
      additionalNeedsInformation = "requires wheelchair access",
      // invalid field
      completionDeadline = LocalDate.of(2020, 1, 1),
    )
    val error = assertThrows<ValidationError> {
      referralService.updateDraftReferral(sampleReferral, update)
    }

    entityManager.flush()
    assertThat(referralService.getDraftReferralForUser(sampleReferral.id, userFactory.create())!!.additionalNeedsInformation).isNull()
  }

  @Test
  fun `once a draft referral is sent it's id is no longer is a valid draft referral`() {
    val user = AuthUser("user_id", "delius", "user_name")
    val draftReferral = referralService.createDraftReferral(user, "X123456", sampleIntervention.id)
    draftReferral.additionalRiskInformation = "risk"

    assertThat(referralService.getDraftReferralForUser(draftReferral.id, user)).isNotNull()

    val sentReferral = referralService.sendDraftReferral(draftReferral, user)

    assertThat(referralService.getDraftReferralForUser(draftReferral.id, user)).isNull()
    assertThat(referralService.getSentReferralForUser(draftReferral.id, user)).isNotNull()
  }

  @Test
  fun `sending a draft referral generates a referral reference number`() {
    val user = AuthUser("user_id", "delius", "user_name")
    val draftReferral = referralService.createDraftReferral(user, "X123456", sampleIntervention.id)
    draftReferral.additionalRiskInformation = "risk"

    assertThat(draftReferral.referenceNumber).isNull()

    val sentReferral = referralService.sendDraftReferral(draftReferral, user)
    assertThat(sentReferral.referenceNumber).isNotNull()
  }

  @Test
  fun `sending a draft referral generates a unique reference, even if the previous reference already exists`() {
    val user = AuthUser("user_id", "delius", "user_name")
    val draft1 = referralService.createDraftReferral(user, "X123456", sampleIntervention.id)
    val draft2 = referralService.createDraftReferral(user, "X123456", sampleIntervention.id)
    draft1.additionalRiskInformation = "risk"
    draft2.additionalRiskInformation = "risk"

    whenever(referenceGenerator.generate(sampleIntervention.dynamicFrameworkContract.contractType.name))
      .thenReturn("AA0000ZZ", "AA0000ZZ", "AA0000ZZ", "AA0000ZZ", "BB0000ZZ")

    val sent1 = referralService.sendDraftReferral(draft1, user)
    assertThat(sent1.referenceNumber).isEqualTo("AA0000ZZ")

    val sent2 = referralService.sendDraftReferral(draft2, user)
    assertThat(sent2.referenceNumber).isNotEqualTo("AA0000ZZ").isEqualTo("BB0000ZZ")
  }

  @Test
  fun `sending a draft referral triggers an event`() {
    val user = AuthUser("user_id", "delius", "user_name")
    val draftReferral = referralService.createDraftReferral(user, "X123456", sampleIntervention.id)
    draftReferral.additionalRiskInformation = "risk"

    referralService.sendDraftReferral(draftReferral, user)
    verify(communityAPIReferralService).send(draftReferral)
    verify(referralEventPublisher).referralSentEvent(draftReferral)
  }

  @Test
  fun `multiple draft referrals can be started by the same user`() {
    val user = AuthUser("multi_user_id", "delius", "user_name")
    whenever(referralAccessFilter.probationPractitionerReferrals(anyList(), any())).then(AdditionalAnswers.returnsFirstArg<List<Referral>>())

    for (i in 1..3) {
      assertDoesNotThrow { referralService.createDraftReferral(user, "X123456", sampleIntervention.id) }
    }
    assertThat(referralService.getDraftReferralsForUser(user)).hasSize(3)
  }

  @Nested
  @DisplayName("get sent referrals with a probation practitioner user")
  inner class GetSentReferralsPPUser {
    @BeforeEach
    fun setup() {
      // do not test access restrictions in these tests; only test selection of referrals
      val anySpec: Specification<Referral> = any()
      whenever(referralAccessFilter.probationPractitionerReferrals(anySpec, any()))
        .then(AdditionalAnswers.returnsFirstArg<Specification<Referral>>())
    }

    @Test
    fun `returns referrals started by the user`() {
      val user = userFactory.create("pp_user_1", "delius")
      val startedReferrals = (1..3).map { referralFactory.createSent(createdBy = user) }

      val result = referralService.getSentReferralsForUser(user, null, null, null, null)
      assertThat(result).containsExactlyElementsOf(startedReferrals)
    }

    @Test
    fun `must not return referrals sent by the user`() {
      val user = userFactory.create("pp_user_1", "delius")
      val sentReferral = referralFactory.createSent(sentBy = user)

      val result = referralService.getSentReferralsForUser(user, null, null, null, null)
      assertThat(result).doesNotContain(sentReferral)
      assertThat(result).isEmpty()
    }

    @Test
    fun `must not propagate errors from community-api`() {
      val user = userFactory.create("pp_user_1", "delius")
      val createdReferral = referralFactory.createSent(createdBy = user)

      whenever(communityAPIOffenderService.getManagedOffendersForDeliusUser(user))
        .thenThrow(WebClientResponseException::class.java)

      val result = assertDoesNotThrow { referralService.getSentReferralsForUser(user, null, null, null, null) }
      assertThat(result).containsExactly(createdReferral)
    }

    @Test
    fun `includes referrals for offenders managed by the user`() {
      val someoneElse = userFactory.create("helper_pp_user", "delius")
      val user = userFactory.create("pp_user_1", "delius")

      val managedReferral1 = referralFactory.createSent(serviceUserCRN = "CRN129876234", createdBy = someoneElse)
      val managedReferral2 = referralFactory.createSent(serviceUserCRN = "CRN129876235", createdBy = someoneElse)
      referralFactory.createSent(serviceUserCRN = "CRN129876236", createdBy = someoneElse)
      whenever(communityAPIOffenderService.getManagedOffendersForDeliusUser(user))
        .thenReturn(listOf(Offender("CRN129876234"), Offender("CRN129876235")))

      val result = referralService.getSentReferralsForUser(user, null, null, null, null)
      assertThat(result).containsExactlyInAnyOrder(managedReferral1, managedReferral2)
    }

    @Test
    fun `returns referrals both managed and started by the user only once`() {
      val user = userFactory.create("pp_user_1", "delius")
      val managedAndStartedReferral = referralFactory.createSent(serviceUserCRN = "CRN129876234", createdBy = user)

      whenever(communityAPIOffenderService.getManagedOffendersForDeliusUser(user))
        .thenReturn(listOf(Offender("CRN129876234")))

      val result = referralService.getSentReferralsForUser(user, null, null, null, null)
      assertThat(result).containsExactly(managedAndStartedReferral)
    }
  }

  @Nested
  @DisplayName("get sent referrals with filter options")
  inner class GetSentReferralsFilterOptionsTest {
    lateinit var user: AuthUser
    lateinit var otherUser: AuthUser
    lateinit var provider: ServiceProvider
    lateinit var completedReferral: Referral
    lateinit var liveReferral: Referral
    lateinit var cancelledReferral: Referral
    lateinit var draftReferral: Referral
    lateinit var selfAssignedReferral: Referral
    lateinit var otherAssignedReferral: Referral

    @BeforeEach
    fun `setup referrals`() {

      user = userFactory.create("test_user", "auth")
      otherUser = userFactory.create(id = "randomId1236798", userName = "otherUserName")
      provider = serviceProviderFactory.create("test")
      val intervention = interventionFactory.create(contract = contractFactory.create(primeProvider = provider))

      completedReferral = referralFactory.createEnded(
        intervention = intervention,
        endRequestedReason = cancellationReasonFactory.create("ANY"),
        concludedAt = OffsetDateTime.now(),
      ).also { referral ->
        referral.endOfServiceReport = endOfServiceReportFactory.create(referral = referral)
      }

      liveReferral = referralFactory.createSent(
        intervention = intervention
      )

      cancelledReferral = referralFactory.createEnded(
        intervention = intervention,
        endRequestedReason = cancellationReasonFactory.create("ANY"),
        concludedAt = OffsetDateTime.now(),
        endOfServiceReport = null,
      )

      draftReferral = referralFactory.createDraft(intervention = intervention)

      selfAssignedReferral = referralFactory.createAssigned(
        intervention = intervention,
        assignments = listOf(
          ReferralAssignment(OffsetDateTime.now(), user, user)
        )
      )

      otherAssignedReferral = referralFactory.createAssigned(
        intervention = intervention,
        assignments = listOf(
          ReferralAssignment(OffsetDateTime.now(), otherUser, otherUser)
        )
      )

      // not testing access restrictions; only test selection of referrals
      val anySpec: Specification<Referral> = any()
      whenever(referralAccessFilter.serviceProviderReferrals(anySpec, any()))
        .then(AdditionalAnswers.returnsFirstArg<Specification<Referral>>())
      whenever(serviceProviderAccessScopeMapper.fromUser(user))
        .thenReturn(ServiceProviderAccessScope(setOf(provider), setOf(intervention.dynamicFrameworkContract)))
    }

    @Test
    fun `by default only non sent referrals are filtered`() {
      val result = referralService.getSentReferralsForUser(user, null, null, null, null)
      assertThat(result).containsExactlyInAnyOrder(completedReferral, liveReferral, cancelledReferral, selfAssignedReferral, otherAssignedReferral)
      assertThat(result).doesNotContain(draftReferral)
    }

    @Test
    fun `setting concluded returns only concluded referrals`() {
      val result = referralService.getSentReferralsForUser(user, true, null, null, null)
      assertThat(result).containsExactlyInAnyOrder(completedReferral, cancelledReferral)
      assertThat(result).doesNotContain(draftReferral, liveReferral, selfAssignedReferral, otherAssignedReferral)
    }

    @Test
    fun `setting not concluded returns only non concluded referrals`() {
      val result = referralService.getSentReferralsForUser(user, false, null, null, null)
      assertThat(result).containsExactlyInAnyOrder(liveReferral, selfAssignedReferral, otherAssignedReferral)
      assertThat(result).doesNotContain(draftReferral, completedReferral, cancelledReferral)
    }

    @Test
    fun `setting cancelled returns only cancelled referrals`() {
      val result = referralService.getSentReferralsForUser(user, null, true, null, null)
      assertThat(result).containsExactlyInAnyOrder(cancelledReferral)
      assertThat(result).doesNotContain(draftReferral, completedReferral, liveReferral, selfAssignedReferral, otherAssignedReferral)
    }

    @Test
    fun `setting not cancelled returns only non cancelled referrals`() {
      val result = referralService.getSentReferralsForUser(user, null, false, null, null)
      assertThat(result).containsExactlyInAnyOrder(completedReferral, liveReferral, selfAssignedReferral, otherAssignedReferral)
      assertThat(result).doesNotContain(draftReferral, cancelledReferral)
    }

    @Test
    fun `setting unassigned returns only unassigned referrals`() {
      val result = referralService.getSentReferralsForUser(user, null, null, true, null)
      assertThat(result).containsExactlyInAnyOrder(completedReferral, liveReferral, cancelledReferral)
      assertThat(result).doesNotContain(draftReferral, selfAssignedReferral, otherAssignedReferral)
    }

    @Test
    fun `setting not unassigned returns only referrals with assignments`() {
      val result = referralService.getSentReferralsForUser(user, null, null, false, null)
      assertThat(result).containsExactlyInAnyOrder(selfAssignedReferral, otherAssignedReferral)
      assertThat(result).doesNotContain(draftReferral, completedReferral, liveReferral, cancelledReferral)
    }

    @Test
    fun `setting assigned to returns referrals with correct assignments`() {
      val result = referralService.getSentReferralsForUser(user, null, null, false, otherUser.id)
      assertThat(result).containsExactlyInAnyOrder(otherAssignedReferral)
      assertThat(result).doesNotContain(draftReferral, completedReferral, liveReferral, cancelledReferral, selfAssignedReferral)
    }

    @Test
    fun `setting assigned to with unknown user returns no referrals`() {
      val result = referralService.getSentReferralsForUser(user, null, null, false, "unknown")
      assertThat(result).isEmpty()
      assertThat(result).doesNotContain(draftReferral, completedReferral, liveReferral, cancelledReferral, otherAssignedReferral, selfAssignedReferral)
    }

    @Test
    fun `setting all filter options to true will return cancelled and unassigned referral`() {
      val result = referralService.getSentReferralsForUser(user, true, true, true, null)
      assertThat(result).containsExactlyInAnyOrder(cancelledReferral)
      assertThat(result).doesNotContain(draftReferral, completedReferral, liveReferral, selfAssignedReferral, otherAssignedReferral)
    }

    @Test
    fun `setting all filter options to false will return an assigned referral`() {
      val result = referralService.getSentReferralsForUser(user, false, false, false, user.id)
      assertThat(result).containsExactlyInAnyOrder(selfAssignedReferral)
      assertThat(result).doesNotContain(draftReferral, completedReferral, liveReferral, cancelledReferral, otherAssignedReferral)
    }
  }

  @Nested
  @DisplayName("get sent referrals with a provider user")
  inner class GetSentReferralsSPUser {
    private lateinit var otherPrime: ServiceProvider
    private lateinit var otherSub: ServiceProvider

    private fun newReferralWithProviders(prime: ServiceProvider, vararg subs: ServiceProvider): Referral {
      val intervention = interventionFactory.create(
        contract = contractFactory.create(primeProvider = prime, subcontractorProviders = subs.toSet())
      )
      return referralFactory.createSent(intervention = intervention)
    }

    @BeforeEach
    fun setup() {
      val anySpec: Specification<Referral> = any()
      whenever(referralAccessFilter.serviceProviderReferrals(anySpec, any()))
        .then(AdditionalAnswers.returnsFirstArg<Specification<Referral>>())
      otherPrime = serviceProviderFactory.create("other_prime")
      otherSub = serviceProviderFactory.create("other_sub")
    }

    @Test
    fun `user with multiple providers can see referrals where the providers are subcontractors`() {

      val userProviders = listOf("test_org_1", "test_org_2").map(serviceProviderFactory::create)

      val primeRef1 = newReferralWithProviders(userProviders[0])
      val primeRef2 = newReferralWithProviders(userProviders[1])
      val primeAndSubRef = newReferralWithProviders(userProviders[0], userProviders[1])
      val refWithAllProvidersBeingSubs = newReferralWithProviders(otherPrime, userProviders[0], userProviders[1])
      val subRef = newReferralWithProviders(otherPrime, userProviders[1])
      val noAccess = newReferralWithProviders(otherPrime, otherSub)

      val user = userFactory.create("test_user", "auth")

      // not testing access restrictions; only test selection of referrals
      val contacts = setOf(primeRef1, primeRef2, primeAndSubRef, refWithAllProvidersBeingSubs, subRef, noAccess).map { it.intervention.dynamicFrameworkContract }
      whenever(serviceProviderAccessScopeMapper.fromUser(user))
        .thenReturn(ServiceProviderAccessScope(userProviders.toSet(), contacts.toSet()))

      val result = referralService.getSentReferralsForUser(user, null, null, null, null)
      assertThat(result).doesNotContain(noAccess)
      assertThat(result).containsAll(listOf(primeRef1, primeRef2, primeAndSubRef, refWithAllProvidersBeingSubs, subRef))
      assertThat(result.size).isEqualTo(5)
    }
  }

  @Nested
  @DisplayName("get sent referrals summary with a service provider user")
  inner class GetServiceProviderSummaries {
    private lateinit var otherPrime: ServiceProvider
    private lateinit var otherSub: ServiceProvider

    private fun userWithProviders(providers: List<ServiceProvider>): AuthUser {
      val user = userFactory.create("test_user", "auth")
      whenever(serviceProviderAccessScopeMapper.fromUser(user))
        .thenReturn(ServiceProviderAccessScope(providers.toSet(), setOf()))
      return user
    }

    private fun newReferralWithProviders(prime: ServiceProvider, vararg subs: ServiceProvider): Referral {
      val intervention = interventionFactory.create(
        contract = contractFactory.create(primeProvider = prime, subcontractorProviders = subs.toSet())
      )
      return referralFactory.createSent(intervention = intervention)
    }

    @BeforeEach
    fun setup() {
      // do not test access restrictions in these tests; only test selection of referrals
      whenever(referralAccessFilter.serviceProviderReferralSummaries(any(), any()))
        .then(AdditionalAnswers.returnsFirstArg<List<Referral>>())

      otherPrime = serviceProviderFactory.create("other_prime")
      otherSub = serviceProviderFactory.create("other_sub")
    }

    @Test
    fun `user with multiple providers can see referrals where the providers are subcontractors`() {
      val userProviders = listOf("test_org_1", "test_org_2").map { id -> serviceProviderFactory.create(id = id, name = id) }

      val primeRef1 = newReferralWithProviders(userProviders[0])
      val primeRef2 = newReferralWithProviders(userProviders[1])
      val primeAndSubRef = newReferralWithProviders(userProviders[0], userProviders[1])
      val refWithAllProvidersBeingSubs = newReferralWithProviders(otherPrime, userProviders[0], userProviders[1])
      val subRef = newReferralWithProviders(otherPrime, userProviders[1])
      val noAccess = newReferralWithProviders(otherPrime, otherSub)

      val multiSubUser = userWithProviders(userProviders)

      val result = referralService.getServiceProviderSummaries(multiSubUser)
      assertThat(result.size).isEqualTo(5)
      val referralIds = result.map { summary -> UUID.fromString(summary.referralId) }
      assertThat(referralIds).doesNotContain(noAccess.id)
      assertThat(referralIds).containsAll(listOf(primeRef1.id, primeRef2.id, primeAndSubRef.id, refWithAllProvidersBeingSubs.id, subRef.id))
    }

    @Test
    fun `referrals that are sent, premature end requested or prematurely ended are returned`() {
      val provider = serviceProviderFactory.create(id = "test")
      val intervention = interventionFactory.create(contract = contractFactory.create(primeProvider = provider))

      val refLive = referralFactory.createSent(intervention = intervention)
      val refPrematureEnded = referralFactory.createEnded(
        intervention = intervention,
        endRequestedReason = cancellationReasonFactory.create("ANY"),
        endRequestedAt = OffsetDateTime.now(),
        concludedAt = OffsetDateTime.now(),
      ).also { referral ->
        referral.endOfServiceReport = endOfServiceReportFactory.create(referral = referral)
      }

      val refPrematureEndRequested = referralFactory.createEnded(
        intervention = intervention,
        endRequestedReason = cancellationReasonFactory.create("ANY"),
        endRequestedAt = OffsetDateTime.now(),
        concludedAt = null
      )

      val user = userWithProviders(listOf(provider))
      val result = referralService.getServiceProviderSummaries(user)
      val referralIds = result.map { summary -> UUID.fromString(summary.referralId) }
      assertThat(referralIds).containsExactlyInAnyOrder(refLive.id, refPrematureEnded.id, refPrematureEndRequested.id)
    }

    @Test
    fun `referrals that are cancelled with no SAA feedback are not returned`() {
      val provider = serviceProviderFactory.create(id = "test")
      val intervention = interventionFactory.create(contract = contractFactory.create(primeProvider = provider))

      val refCancelled = referralFactory.createEnded(
        intervention = intervention,
        endRequestedReason = cancellationReasonFactory.create("ANY"),
        concludedAt = OffsetDateTime.now(),
        endOfServiceReport = null,
      )

      val appointment = appointmentFactory.create(referral = refCancelled)
      val supplierAssessmentAppointment = supplierAssessmentFactory.create(appointment = appointment, referral = refCancelled)
      refCancelled.supplierAssessment = supplierAssessmentAppointment
      entityManager.refresh(refCancelled)

      val user = userWithProviders(listOf(provider))
      val result = referralService.getServiceProviderSummaries(user)
      val referralIds = result.map { summary -> UUID.fromString(summary.referralId) }
      assertThat(referralIds).isEmpty()
    }

    @Test
    fun `referrals that are cancelled with SAA feedback are returned`() {
      val provider = serviceProviderFactory.create(id = "test")
      val intervention = interventionFactory.create(contract = contractFactory.create(primeProvider = provider))

      val refCancelled = referralFactory.createEnded(
        intervention = intervention,
        endRequestedReason = cancellationReasonFactory.create("ANY"),
        concludedAt = OffsetDateTime.now(),
        endOfServiceReport = null,
      )

      val appointment = appointmentFactory.create(referral = refCancelled, attendanceSubmittedAt = OffsetDateTime.now())
      val supplierAssessmentAppointment = supplierAssessmentFactory.create(appointment = appointment, referral = refCancelled)
      refCancelled.supplierAssessment = supplierAssessmentAppointment
      entityManager.refresh(refCancelled)

      val user = userWithProviders(listOf(provider))
      val result = referralService.getServiceProviderSummaries(user)
      val referralIds = result.map { summary -> UUID.fromString(summary.referralId) }
      assertThat(referralIds).containsExactly(refCancelled.id)
    }

    @Test
    fun `referrals that are cancelled with Action Plan created but not submitted are not returned`() {
      val provider = serviceProviderFactory.create(id = "test")
      val intervention = interventionFactory.create(contract = contractFactory.create(primeProvider = provider))

      val refCancelled = referralFactory.createEnded(
        intervention = intervention,
        endRequestedReason = cancellationReasonFactory.create("ANY"),
        concludedAt = OffsetDateTime.now(),
        endOfServiceReport = null,
      )

      actionPlanFactory.create(referral = refCancelled)

      val user = userWithProviders(listOf(provider))
      val result = referralService.getServiceProviderSummaries(user)
      val referralIds = result.map { summary -> UUID.fromString(summary.referralId) }
      assertThat(referralIds).isEmpty()
    }

    @Test
    fun `referrals that are cancelled with Action Plan submitted are returned`() {
      val provider = serviceProviderFactory.create(id = "test")
      val intervention = interventionFactory.create(contract = contractFactory.create(primeProvider = provider))

      val refCancelled = referralFactory.createEnded(
        intervention = intervention,
        endRequestedReason = cancellationReasonFactory.create("ANY"),
        concludedAt = OffsetDateTime.now(),
        endOfServiceReport = null,
      )

      actionPlanFactory.createSubmitted(referral = refCancelled)

      val user = userWithProviders(listOf(provider))
      val result = referralService.getServiceProviderSummaries(user)
      val referralIds = result.map { summary -> UUID.fromString(summary.referralId) }
      assertThat(referralIds).containsExactly(refCancelled.id)
    }

    @Test
    fun `sent referral summary provides correct details for end of service report`() {
      val provider = serviceProviderFactory.create(id = "test")
      val intervention = interventionFactory.create(contract = contractFactory.create(primeProvider = provider))

      val refLive = referralFactory.createSent(intervention = intervention)
      val endOfServiceReportCreated = referralFactory.createSent(
        intervention = intervention
      ).also { referral ->
        referral.endOfServiceReport = endOfServiceReportFactory.create(referral = referral)
      }
      val endOfServiceReportSubmitted = referralFactory.createEnded(
        intervention = intervention,
        endRequestedReason = cancellationReasonFactory.create("ANY"),
        concludedAt = OffsetDateTime.now(),
      ).also { referral ->
        referral.endOfServiceReport = endOfServiceReportFactory.create(referral = referral, submittedAt = OffsetDateTime.now())
      }

      val user = userWithProviders(listOf(provider))
      val result = referralService.getServiceProviderSummaries(user)

      val refLiveSummary = result.find { it.referralId == refLive.id.toString() }
      assertThat(refLiveSummary!!.endOfServiceReportId).isNull()
      assertThat(refLiveSummary!!.endOfServiceReportSubmittedAt).isNull()
      val endOfServiceReportCreatedSummary = result.find { it.referralId == endOfServiceReportCreated.id.toString() }
      assertThat(endOfServiceReportCreatedSummary!!.endOfServiceReportId).isNotNull()
      assertThat(endOfServiceReportCreatedSummary!!.endOfServiceReportSubmittedAt).isNull()
      val endOfServiceReportSubmittedSummary = result.find { it.referralId == endOfServiceReportSubmitted.id.toString() }
      assertThat(endOfServiceReportSubmittedSummary!!.endOfServiceReportId).isNotNull()
      assertThat(endOfServiceReportSubmittedSummary!!.endOfServiceReportSubmittedAt).isNotNull()
    }
  }

  @Test
  fun `ensure that desired outcomes are actually removed via orphan removal`() {

    val serviceCategoryId1 = UUID.randomUUID()
    val serviceCategoryId2 = UUID.randomUUID()
    val desiredOutcome1 = DesiredOutcome(UUID.randomUUID(), "title", serviceCategoryId = serviceCategoryId1)
    val desiredOutcome2 = DesiredOutcome(UUID.randomUUID(), "title", serviceCategoryId = serviceCategoryId2)
    val serviceCategory1 = serviceCategoryFactory.create(id = serviceCategoryId1, desiredOutcomes = mutableListOf(desiredOutcome1))
    val serviceCategory2 = serviceCategoryFactory.create(id = serviceCategoryId2, desiredOutcomes = mutableListOf(desiredOutcome2))

    val contractType = contractTypeFactory.create(serviceCategories = setOf(serviceCategory1, serviceCategory2))
    val referral = referralFactory.createDraft(
      intervention = interventionFactory.create(
        contract = dynamicFrameworkContractFactory.create(
          contractType = contractType
        )
      ),
      selectedServiceCategories = setOf(serviceCategory1).toMutableSet(),
      desiredOutcomes = listOf(desiredOutcome1).toMutableList()
    )
    referralService.updateDraftReferral(referral, DraftReferralDTO(serviceCategoryIds = listOf(serviceCategoryId2)))

    referralRepository.flush()
    val updatedReferral = referralRepository.findById(referral.id).get()
    assertThat(updatedReferral.selectedServiceCategories).hasSize(1)
    assertThat(updatedReferral.selectedServiceCategories!!.elementAt(0).id).isEqualTo(serviceCategoryId2)
    assertThat(updatedReferral.selectedDesiredOutcomes).hasSize(0)
  }

  @Test
  fun `ensure that service categories constraint is not thrown when service categories is reselected with an already selected desired outcome`() {
    val serviceCategoryId = UUID.randomUUID()
    val desiredOutcome = DesiredOutcome(UUID.randomUUID(), "title", serviceCategoryId = serviceCategoryId)
    val serviceCategory = serviceCategoryFactory.create(id = serviceCategoryId, desiredOutcomes = mutableListOf(desiredOutcome))

    val contractType = contractTypeFactory.create(serviceCategories = setOf(serviceCategory))
    val referral = referralFactory.createDraft(
      intervention = interventionFactory.create(
        contract = dynamicFrameworkContractFactory.create(
          contractType = contractType
        )
      ),
      selectedServiceCategories = setOf(serviceCategory).toMutableSet(),
      desiredOutcomes = listOf(desiredOutcome).toMutableList(),
    )
    referralService.updateDraftReferral(referral, DraftReferralDTO(serviceCategoryIds = listOf(serviceCategoryId)))

    referralRepository.flush()
    val updatedReferral = referralRepository.findById(referral.id).get()
    assertThat(updatedReferral.selectedServiceCategories).hasSize(1)
    assertThat(updatedReferral.selectedServiceCategories!!.elementAt(0).id).isEqualTo(serviceCategoryId)
    assertThat(updatedReferral.selectedDesiredOutcomes).hasSize(1)
  }
}
