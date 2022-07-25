package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.provider.Arguments
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
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
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.DraftReferral
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Intervention
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Referral
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ReferralAssignment
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.SampleData
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.SentReferralSummary
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ServiceProvider
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ActionPlanRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.AuthUserRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.CancellationReasonRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.DeliverySessionRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.DraftReferralRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.EndOfServiceReportRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.InterventionRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ReferralDetailsRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ReferralRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.SentReferralSummariesRepository
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
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.SentReferralSummariesFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ServiceCategoryFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ServiceProviderFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ServiceUserFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.SupplierAssessmentFactory
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.UUID
import kotlin.random.Random

@RepositoryTest
class ReferralServiceTest @Autowired constructor(
  val entityManager: TestEntityManager,
  val referralRepository: ReferralRepository,
  val draftReferralRepository: DraftReferralRepository,
  val sentReferralSummariesRepository: SentReferralSummariesRepository,
  val authUserRepository: AuthUserRepository,
  val interventionRepository: InterventionRepository,
  val cancellationReasonRepository: CancellationReasonRepository,
  val deliverySessionRepository: DeliverySessionRepository,
  val actionPlanRepository: ActionPlanRepository,
  val endOfServiceReportRepository: EndOfServiceReportRepository,
  val serviceCategoryRepository: ServiceCategoryRepository,
) {

  private val userFactory = AuthUserFactory(entityManager)
  private val interventionFactory = InterventionFactory(entityManager)
  private val contractFactory = DynamicFrameworkContractFactory(entityManager)
  private val serviceProviderFactory = ServiceProviderFactory(entityManager)
  private val referralFactory = ReferralFactory(entityManager)
  private val sentReferralSummariesFactory = SentReferralSummariesFactory(entityManager)
  private val serviceCategoryFactory = ServiceCategoryFactory(entityManager)
  private val contractTypeFactory = ContractTypeFactory(entityManager)
  private val dynamicFrameworkContractFactory = DynamicFrameworkContractFactory(entityManager)
  private val cancellationReasonFactory = CancellationReasonFactory(entityManager)
  private val endOfServiceReportFactory = EndOfServiceReportFactory(entityManager)
  private val actionPlanFactory = ActionPlanFactory(entityManager)
  private val appointmentFactory = AppointmentFactory(entityManager)
  private val supplierAssessmentFactory = SupplierAssessmentFactory(entityManager)
  private val serviceUserDataFactory = ServiceUserFactory(entityManager)

  private val referralEventPublisher: ReferralEventPublisher = mock()
  private val referenceGenerator: ReferralReferenceGenerator = spy(ReferralReferenceGenerator())
  private val referralConcluder: ReferralConcluder = mock()
  private val referralAccessChecker: ReferralAccessChecker = mock()
  private val userTypeChecker = UserTypeChecker()
  private val serviceProviderAccessScopeMapper: ServiceProviderAccessScopeMapper = mock()
  private val referralAccessFilter = ReferralAccessFilter(serviceProviderAccessScopeMapper)
  private val communityAPIReferralService: CommunityAPIReferralService = mock()
  private val serviceUserAccessChecker: ServiceUserAccessChecker = mock()
  private val assessRisksAndNeedsService: RisksAndNeedsService = mock()
  private val communityAPIOffenderService: CommunityAPIOffenderService = mock()
  private val supplierAssessmentService: SupplierAssessmentService = mock()
  private val hmppsAuthService: HMPPSAuthService = mock()
  private val telemetryService: TelemetryService = mock()
  private val draftOasysRiskInformationService: DraftOasysRiskInformationService = mock()
  private val referralDetailsRepository: ReferralDetailsRepository = mock()
  private val recursiveComparisonConfigurationBuilder = RecursiveComparisonConfiguration.builder()

  private val referralService = ReferralService(
    referralRepository,
    draftReferralRepository,
    sentReferralSummariesRepository,
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
    draftOasysRiskInformationService,
    referralDetailsRepository,
  )

  companion object {
    @JvmStatic
    fun names1234(): List<Arguments> {
      return listOf(
        Arguments.of("bob-smith", "smith", "bob-smith smith"),
        Arguments.of("john", "blue-red", "john blue-red")
      )
    }
  }

  @AfterEach
  fun `clear referrals`() {
    draftReferralRepository.deleteAll()
    referralRepository.deleteAll()
  }

  @Nested
  @DisplayName("create / find / update / send draft referrals")
  inner class createFindUpdateAndSendDraftReferrals {

    private lateinit var sampleDraftReferral: DraftReferral
    private lateinit var sampleIntervention: Intervention
    private lateinit var sampleCohortIntervention: Intervention

    @BeforeEach
    fun beforeEach() {
      sampleDraftReferral = SampleData.persistReferral(
        entityManager,
        SampleData.sampleDraftReferral("X123456", "Harmony Living")
      )
      sampleIntervention = sampleDraftReferral.intervention

      sampleCohortIntervention = SampleData.persistIntervention(
        entityManager,
        SampleData.sampleIntervention(
          dynamicFrameworkContract = SampleData.sampleContract(
            primeProvider = sampleDraftReferral.intervention.dynamicFrameworkContract.primeProvider,
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

      val updated = referralService.updateDraftReferral(sampleDraftReferral, draftReferral)
      assertThat(updated.id).isEqualTo(sampleDraftReferral.id)
      assertThat(updated.createdAt).isEqualTo(sampleDraftReferral.createdAt)
    }

    @Test
    fun `null fields in the update do not overwrite original fields`() {
      val sentenceId = Random.nextLong()
      sampleDraftReferral.relevantSentenceId = sentenceId
      entityManager.persistAndFlush(sampleDraftReferral)

      val draftReferral = DraftReferralDTO(relevantSentenceId = null)

      val updated = referralService.updateDraftReferral(sampleDraftReferral, draftReferral)
      assertThat(updated.relevantSentenceId).isEqualTo(sentenceId)
    }

    @Test
    fun `non-null fields in the update overwrite original fields`() {
      val existingSentenceId = Random.nextLong()
      val newSentenceId = Random.nextLong()
      sampleDraftReferral.relevantSentenceId = existingSentenceId
      entityManager.persistAndFlush(sampleDraftReferral)

      val draftReferral = DraftReferralDTO(relevantSentenceId = newSentenceId)

      val updated = referralService.updateDraftReferral(sampleDraftReferral, draftReferral)
      assertThat(updated.relevantSentenceId).isEqualTo(newSentenceId)
    }

    @Test
    fun `update mutates the original object`() {
      val existingSentenceId = Random.nextLong()
      val newSentenceId = Random.nextLong()
      sampleDraftReferral.relevantSentenceId = existingSentenceId
      entityManager.persistAndFlush(sampleDraftReferral)

      val today = LocalDate.now()
      val draftReferral = DraftReferralDTO(relevantSentenceId = newSentenceId)

      val updated = referralService.updateDraftReferral(sampleDraftReferral, draftReferral)
      assertThat(updated.relevantSentenceId).isEqualTo(newSentenceId)
    }

    @Test
    fun `update successfully persists the updated draft referral`() {
      val existingSentenceId = Random.nextLong()
      val newSentenceId = Random.nextLong()
      sampleDraftReferral.relevantSentenceId = existingSentenceId
      entityManager.persistAndFlush(sampleDraftReferral)

      val draftReferral = DraftReferralDTO(relevantSentenceId = newSentenceId)
      referralService.updateDraftReferral(sampleDraftReferral, draftReferral)
      val savedDraftReferral = referralService.getDraftReferralForUser(sampleDraftReferral.id, userFactory.create())
      assertThat(savedDraftReferral).isNotNull
      assertThat(savedDraftReferral?.id).isEqualTo(sampleDraftReferral.id)
      assertThat(savedDraftReferral?.createdAt).isEqualTo(sampleDraftReferral.createdAt)
      assertThat(savedDraftReferral?.relevantSentenceId).isEqualTo(draftReferral.relevantSentenceId)
    }

    @Test
    fun `create and persist non-cohort draft referral`() {
      val authUser = AuthUser("user_id", "delius", "user_name")
      val draftReferral = referralService.createDraftReferral(authUser, "X123456", sampleIntervention.id)
      entityManager.flush()

      val savedDraftReferral = referralService.getDraftReferralForUser(draftReferral.id, authUser)
      assertThat(savedDraftReferral?.id).isNotNull
      assertThat(savedDraftReferral?.createdAt).isNotNull
      assertThat(savedDraftReferral?.createdBy).isEqualTo(authUser)
      assertThat(savedDraftReferral?.serviceUserCRN).isEqualTo("X123456")
      assertThat(savedDraftReferral?.selectedServiceCategories).hasSize(1)
      assertThat(savedDraftReferral?.selectedServiceCategories!!.elementAt(0).id).isEqualTo(
        sampleIntervention.dynamicFrameworkContract.contractType.serviceCategories.elementAt(
          0
        ).id
      )
    }

    @Test
    fun `create and persist cohort draft referral`() {
      val authUser = AuthUser("user_id", "delius", "user_name")
      val draftReferral = referralService.createDraftReferral(authUser, "X123456", sampleCohortIntervention.id)
      entityManager.flush()

      val savedDraftReferral = referralService.getDraftReferralForUser(draftReferral.id, authUser)
      assertThat(savedDraftReferral).isNotNull
      assertThat(savedDraftReferral?.id).isNotNull
      assertThat(savedDraftReferral?.createdAt).isNotNull
      assertThat(savedDraftReferral?.createdBy).isEqualTo(authUser)
      assertThat(savedDraftReferral?.serviceUserCRN).isEqualTo("X123456")
      assertThat(savedDraftReferral?.selectedServiceCategories).isNull()
    }

    @Test
    fun `get a draft referral`() {
      val sentenceId = Random.nextLong()
      sampleDraftReferral.relevantSentenceId = sentenceId
      entityManager.persistAndFlush(sampleDraftReferral)

      val savedDraftReferral = referralService.getDraftReferralForUser(sampleDraftReferral.id, userFactory.create())
      assertThat(savedDraftReferral?.id).isEqualTo(sampleDraftReferral.id)
      assertThat(savedDraftReferral?.createdAt).isEqualTo(sampleDraftReferral.createdAt)
      assertThat(savedDraftReferral?.relevantSentenceId).isEqualTo(sampleDraftReferral.relevantSentenceId)
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
        referralService.updateDraftReferral(sampleDraftReferral, update)
      }
      assertThat(error.errors.size).isEqualTo(1)
      assertThat(error.errors[0].field).isEqualTo("completionDeadline")
    }

    @Test
    fun `when needsInterpreter is true, interpreterLanguage must be set`() {
      // this is fine
      referralService.updateDraftReferral(sampleDraftReferral, DraftReferralDTO(needsInterpreter = false))

      val error = assertThrows<ValidationError> {
        // this throws ValidationError
        referralService.updateDraftReferral(sampleDraftReferral, DraftReferralDTO(needsInterpreter = true))
      }
      assertThat(error.errors.size).isEqualTo(1)
      assertThat(error.errors[0].field).isEqualTo("needsInterpreter")

      // this is also fine
      referralService.updateDraftReferral(
        sampleDraftReferral,
        DraftReferralDTO(needsInterpreter = true, interpreterLanguage = "German")
      )
    }

    @Test
    fun `when hasAdditionalResponsibilities is true, whenUnavailable must be set`() {
      // this is fine
      referralService.updateDraftReferral(sampleDraftReferral, DraftReferralDTO(hasAdditionalResponsibilities = false))

      val error = assertThrows<ValidationError> {
        // this throws ValidationError
        referralService.updateDraftReferral(sampleDraftReferral, DraftReferralDTO(hasAdditionalResponsibilities = true))
      }
      assertThat(error.errors.size).isEqualTo(1)
      assertThat(error.errors[0].field).isEqualTo("hasAdditionalResponsibilities")

      // this is also fine
      referralService.updateDraftReferral(
        sampleDraftReferral,
        DraftReferralDTO(hasAdditionalResponsibilities = true, whenUnavailable = "wednesdays")
      )
    }

    @Test
    fun `multiple errors at once`() {
      val update = DraftReferralDTO(completionDeadline = LocalDate.of(2020, 1, 1), needsInterpreter = true)
      val error = assertThrows<ValidationError> {
        referralService.updateDraftReferral(sampleDraftReferral, update)
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
      assertThrows<ValidationError> {
        referralService.updateDraftReferral(sampleDraftReferral, update)
      }

      entityManager.flush()
      val draftReferral = referralService.getDraftReferralForUser(
        sampleDraftReferral.id,
        userFactory.create()
      )
      assertThat(draftReferral?.additionalNeedsInformation).isNull()
    }

    @Test
    fun `once a draft referral is sent to referral it's id is still valid in draft referral`() {
      val user = AuthUser("user_id", "delius", "user_name")
      val draftReferral = referralService.createDraftReferral(user, "X123456", sampleIntervention.id)
      draftReferral.additionalRiskInformation = "risk"

      assertThat(referralService.getDraftReferralForUser(draftReferral.id, user)).isNotNull

      referralService.sendDraftReferral(draftReferral, user)

      assertThat(referralService.getDraftReferralForUser(draftReferral.id, user)).isNotNull
      assertThat(referralService.getSentReferralForUser(draftReferral.id, user)).isNotNull
    }

    @Test
    fun `sending a draft referral generates a referral reference number`() {
      val user = AuthUser("user_id", "delius", "user_name")
      val draftReferral = referralService.createDraftReferral(user, "X123456", sampleIntervention.id)
      draftReferral.additionalRiskInformation = "risk"

      val sentReferral = referralService.sendDraftReferral(draftReferral, user)
      assertThat(sentReferral.referenceNumber).isNotNull
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

      val referral = referralService.sendDraftReferral(draftReferral, user)
      val eventCaptor = argumentCaptor<Referral>()
      verify(communityAPIReferralService).send(eventCaptor.capture())
      val capturedReferral = eventCaptor.firstValue
      assertThat(capturedReferral.id).isEqualTo(referral.id)
      verify(referralEventPublisher).referralSentEvent(referral)
    }

    @Test
    fun `multiple draft referrals can be started by the same user`() {
      val user = AuthUser("multi_user_id", "delius", "user_name")

      for (i in 1..3) {
        assertDoesNotThrow { referralService.createDraftReferral(user, "X123456", sampleIntervention.id) }
      }
      assertThat(referralService.getDraftReferralsForUser(user)).hasSize(3)
    }
  }

  @Nested
  @DisplayName("get sent referrals with a probation practitioner user")
  inner class GetSentReferralsPPUser {
    private val truncateSeconds: Comparator<OffsetDateTime> = Comparator { a, exp ->
      if (exp != null && a != null) {
        if (a
          .truncatedTo(ChronoUnit.SECONDS)
          .isEqual(exp.truncatedTo(ChronoUnit.SECONDS))
        ) 0 else 1
      } else { 0 }
    }

    private val recursiveComparisonConfiguration: RecursiveComparisonConfiguration = recursiveComparisonConfigurationBuilder
      .withComparatorForType(truncateSeconds, OffsetDateTime::class.java)
      .build()

    var pageRequest: PageRequest = PageRequest.of(0, 20)
    @Test
    fun `returns referrals started by the user`() {
      val user = userFactory.create("pp_user_1", "delius")
      val startedReferrals = (1..3).map { sentReferralSummariesFactory.createSent(createdBy = user) }

      val result = referralService.getSentReferralSummaryForUser(user, null, null, null, null, pageRequest)
      assertThat(result)
        .usingRecursiveFieldByFieldElementComparator(recursiveComparisonConfiguration)
        .containsExactlyInAnyOrderElementsOf(startedReferrals)
    }

    @Test
    fun `to check for cancelled referral were pop did not attend appointment should return for PP user`() {
      val user = userFactory.create("pp_user_1", "delius")
      val startedReferrals = (1..3).map { sentReferralSummariesFactory.createSent(createdBy = user) }

      val cancelledReferral = referralFactory.createEnded(
        endRequestedReason = cancellationReasonFactory.create("ANY"),
        createdBy = user,
        endRequestedAt = OffsetDateTime.now(),
        concludedAt = OffsetDateTime.now(),
        endOfServiceReport = null,
        actionPlans = mutableListOf(actionPlanFactory.create(submittedAt = null))
      )
      val appointment =
        appointmentFactory.create(referral = cancelledReferral, attendanceSubmittedAt = null)
      val superSededAppointment =
        appointmentFactory.create(referral = cancelledReferral, attendanceSubmittedAt = null, superseded = true)
      val supplierAssessmentAppointment =
        supplierAssessmentFactory.createWithMultipleAppointments(appointments = mutableSetOf(appointment, superSededAppointment), referral = cancelledReferral)
      cancelledReferral.supplierAssessment = supplierAssessmentAppointment
      val cancelledWithoutAttendanceReferralSummary = sentReferralSummariesFactory.getReferralSummary(cancelledReferral)

      entityManager.refresh(cancelledReferral)
      val result = referralService.getSentReferralSummaryForUser(user, null, null, null, null, pageRequest)
      assertThat(result)
        .usingRecursiveFieldByFieldElementComparator(recursiveComparisonConfiguration)
        .containsExactlyInAnyOrderElementsOf(startedReferrals.plus(cancelledWithoutAttendanceReferralSummary))
    }

    fun `must not return referrals sent by the user`() {
      val user = userFactory.create("pp_user_1", "delius")
      val sentReferral = sentReferralSummariesFactory.createSent(sentBy = user)

      val result = referralService.getSentReferralSummaryForUser(user, null, null, null, null, pageRequest)
      assertThat(result).doesNotContain(sentReferral)
      assertThat(result).isEmpty()
    }

    @Test
    fun `must not propagate errors from community-api`() {
      val user = userFactory.create("pp_user_1", "delius")
      val createdReferral = sentReferralSummariesFactory.createSent(createdBy = user)

      whenever(communityAPIOffenderService.getManagedOffendersForDeliusUser(user))
        .thenThrow(WebClientResponseException::class.java)

      val result = assertDoesNotThrow { referralService.getSentReferralSummaryForUser(user, null, null, null, null, pageRequest) }
      assertThat(result)
        .usingRecursiveFieldByFieldElementComparator(recursiveComparisonConfiguration)
        .containsExactly(createdReferral)
    }

    @Test
    fun `includes referrals for offenders managed by the user`() {
      val someoneElse = userFactory.create("helper_pp_user", "delius")
      val user = userFactory.create("pp_user_1", "delius")

      val managedReferral1 = sentReferralSummariesFactory.createSent(serviceUserCRN = "CRN129876234", createdBy = someoneElse)
      val managedReferral2 = sentReferralSummariesFactory.createSent(serviceUserCRN = "CRN129876235", createdBy = someoneElse)
      referralFactory.createSent(serviceUserCRN = "CRN129876236", createdBy = someoneElse)
      whenever(communityAPIOffenderService.getManagedOffendersForDeliusUser(user))
        .thenReturn(listOf(Offender("CRN129876234"), Offender("CRN129876235")))

      val result = referralService.getSentReferralSummaryForUser(user, null, null, null, null, pageRequest)
      assertThat(result)
        .usingRecursiveFieldByFieldElementComparator(recursiveComparisonConfiguration)
        .containsExactlyInAnyOrder(managedReferral1, managedReferral2)
    }

    @Test
    fun `returns referrals both managed and started by the user only once`() {
      val user = userFactory.create("pp_user_1", "delius")
      val managedAndStartedReferral = sentReferralSummariesFactory.createSent(serviceUserCRN = "CRN129876234", createdBy = user)

      whenever(communityAPIOffenderService.getManagedOffendersForDeliusUser(user))
        .thenReturn(listOf(Offender("CRN129876234")))

      val result = referralService.getSentReferralSummaryForUser(user, null, null, null, null, pageRequest)
      assertThat(result)
        .usingRecursiveFieldByFieldElementComparator(recursiveComparisonConfiguration)
        .containsExactly(managedAndStartedReferral)
    }

    @Test
    fun `returns a Page of results if the page parameter is not null`() {
      val user = userFactory.create("pp_user_1", "delius")
      (1..8).map { referralFactory.createSent(createdBy = user) }

      val pageSize = 5
      val page = referralService.getSentReferralSummaryForUser(
        user,
        null,
        null,
        null,
        null,
        Pageable.ofSize(pageSize)
      ) as Page<SentReferralSummary>
      assertThat(page.content.size).isEqualTo(pageSize)
      assertThat(page.totalElements).isEqualTo(8)
    }
  }

  @Nested
  @DisplayName("get sent referrals with a provider user")
  inner class GetSentReferralsSPUser {

    @Test
    fun `returns a Page of results if the page parameter is not null`() {
      val referrals = (1..8).map { referralFactory.createSent() }
      val contracts = referrals.map { it.intervention.dynamicFrameworkContract }
      val user = userFactory.create("test_user", "auth")

      whenever(serviceProviderAccessScopeMapper.fromUser(user))
        .thenReturn(ServiceProviderAccessScope(setOf(serviceProviderFactory.create()), contracts.toSet()))

      val pageSize = 5
      val page = referralService.getSentReferralSummaryForUser(
        user,
        null,
        null,
        null,
        null,
        Pageable.ofSize(pageSize)
      ) as Page<SentReferralSummary>
      assertThat(page.content.size).isEqualTo(pageSize)
      assertThat(page.totalElements).isEqualTo(8)
    }

    @Test
    fun `filters sent referrals based on users contract groups`() {
      val validContracts = (1..3).map { contractFactory.create() }
      val invalidContracts = (1..2).map { contractFactory.create() }

      (validContracts + invalidContracts).forEach { referralFactory.createSent(intervention = interventionFactory.create(contract = it)) }
      val user = userFactory.create("test_user", "auth")

      whenever(serviceProviderAccessScopeMapper.fromUser(user))
        .thenReturn(ServiceProviderAccessScope(setOf(serviceProviderFactory.create()), validContracts.toSet()))

      val filteredReferrals = referralService.getSentReferralSummaryForUser(user, null, null, null, null, PageRequest.of(0, 10))
      assertThat((filteredReferrals as Page<SentReferralSummary>).content.size).isEqualTo(3)
      assertThat(filteredReferrals.map { it.intervention.dynamicFrameworkContract }).doesNotContain(*invalidContracts.toTypedArray())
    }
  }

  @Nested
  @DisplayName("get sent referrals summary with a service provider user")
  inner class GetServiceProviderSummaries {
    @Test
    fun `user with multiple providers can see referrals where the providers are subcontractors`() {
      val userProviders = listOf("test_org_1", "test_org_2").map { id -> serviceProviderFactory.create(id = id, name = id) }
      val contractWithUserProviderAsPrime = contractFactory.create(primeProvider = userProviders[0])
      val contractWithUserProviderAsSub = contractFactory.create(subcontractorProviders = setOf(userProviders[0]))
      val contractWithUserProviderAsBothPrimeAndSub = contractFactory.create(primeProvider = userProviders[0], subcontractorProviders = setOf(userProviders[1]))
      val contractWithNoUserProviders = contractFactory.create()

      val primeRef = referralFactory.createSent(intervention = interventionFactory.create(contract = contractWithUserProviderAsPrime))
      val primeAndSubRef = referralFactory.createSent(intervention = interventionFactory.create(contract = contractWithUserProviderAsBothPrimeAndSub))
      val refWithAllProvidersBeingSubs = referralFactory.createSent(intervention = interventionFactory.create(contract = contractWithUserProviderAsSub))
      val subRef = referralFactory.createSent(intervention = interventionFactory.create(contract = contractWithUserProviderAsSub))
      val noAccess = referralFactory.createSent(intervention = interventionFactory.create(contract = contractWithNoUserProviders))

      val user = userFactory.create("test_user", "auth")
      whenever(serviceProviderAccessScopeMapper.fromUser(user))
        .thenReturn(ServiceProviderAccessScope(userProviders.toSet(), setOf(contractWithUserProviderAsBothPrimeAndSub, contractWithUserProviderAsPrime, contractWithUserProviderAsSub)))

      val result = referralService.getServiceProviderSummaries(user)
      assertThat(result.size).isEqualTo(4)
      val referralIds = result.map { summary -> UUID.fromString(summary.referralId) }
      assertThat(referralIds).doesNotContain(noAccess.id)
      assertThat(referralIds).containsAll(
        listOf(
          primeRef.id,
          primeAndSubRef.id,
          refWithAllProvidersBeingSubs.id,
          subRef.id
        )
      )
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

      val user = userFactory.create("test_user", "auth")
      whenever(serviceProviderAccessScopeMapper.fromUser(user))
        .thenReturn(ServiceProviderAccessScope(setOf(provider), setOf(intervention.dynamicFrameworkContract)))
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
      val supplierAssessmentAppointment =
        supplierAssessmentFactory.create(appointment = appointment, referral = refCancelled)
      refCancelled.supplierAssessment = supplierAssessmentAppointment
      entityManager.refresh(refCancelled)

      val user = userFactory.create("test_user", "auth")
      whenever(serviceProviderAccessScopeMapper.fromUser(user))
        .thenReturn(ServiceProviderAccessScope(setOf(provider), setOf(intervention.dynamicFrameworkContract)))
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

      val appointment =
        appointmentFactory.create(referral = refCancelled, attendanceSubmittedAt = OffsetDateTime.now())
      val supplierAssessmentAppointment =
        supplierAssessmentFactory.create(appointment = appointment, referral = refCancelled)
      refCancelled.supplierAssessment = supplierAssessmentAppointment
      entityManager.refresh(refCancelled)

      val user = userFactory.create("test_user", "auth")
      whenever(serviceProviderAccessScopeMapper.fromUser(user))
        .thenReturn(ServiceProviderAccessScope(setOf(provider), setOf(intervention.dynamicFrameworkContract)))
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

      val user = userFactory.create("test_user", "auth")
      whenever(serviceProviderAccessScopeMapper.fromUser(user))
        .thenReturn(ServiceProviderAccessScope(setOf(provider), setOf(intervention.dynamicFrameworkContract)))
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

      val user = userFactory.create("test_user", "auth")
      whenever(serviceProviderAccessScopeMapper.fromUser(user))
        .thenReturn(ServiceProviderAccessScope(setOf(provider), setOf(intervention.dynamicFrameworkContract)))
      val result = referralService.getServiceProviderSummaries(user)
      val referralIds = result.map { summary -> UUID.fromString(summary.referralId) }
      assertThat(referralIds).containsExactly(refCancelled.id)
    }

    @Test
    fun `sent referral summary provides correct details for end of service report`() {
      val provider = serviceProviderFactory.create(id = "test")
      val contract = contractFactory.create(primeProvider = provider)
      val intervention = interventionFactory.create(contract = contract)

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
        referral.endOfServiceReport =
          endOfServiceReportFactory.create(referral = referral, submittedAt = OffsetDateTime.now())
      }

      val user = userFactory.create("test_user", "auth")
      whenever(serviceProviderAccessScopeMapper.fromUser(user))
        .thenReturn(ServiceProviderAccessScope(setOf(provider), setOf(intervention.dynamicFrameworkContract)))
      val result = referralService.getServiceProviderSummaries(user)

      val refLiveSummary = result.find { it.referralId == refLive.id.toString() }
      assertThat(refLiveSummary!!.endOfServiceReportId).isNull()
      assertThat(refLiveSummary!!.endOfServiceReportSubmittedAt).isNull()
      val endOfServiceReportCreatedSummary = result.find { it.referralId == endOfServiceReportCreated.id.toString() }
      assertThat(endOfServiceReportCreatedSummary!!.endOfServiceReportId).isNotNull()
      assertThat(endOfServiceReportCreatedSummary!!.endOfServiceReportSubmittedAt).isNull()
      val endOfServiceReportSubmittedSummary =
        result.find { it.referralId == endOfServiceReportSubmitted.id.toString() }
      assertThat(endOfServiceReportSubmittedSummary!!.endOfServiceReportId).isNotNull()
      assertThat(endOfServiceReportSubmittedSummary!!.endOfServiceReportSubmittedAt).isNotNull()
    }
  }

  @Nested
  @DisplayName("get sent referrals with person on probation search")
  inner class GetSentReferralsWithPopSearchTest {
    lateinit var user: AuthUser
    lateinit var pageRequest: PageRequest
    lateinit var referral: Referral
    lateinit var referral2: Referral
    lateinit var referral3: Referral
    lateinit var provider: ServiceProvider
    lateinit var intervention: Intervention

    @BeforeEach
    fun `setup referrals`() {
      user = userFactory.create("test_user", "auth")
      pageRequest = PageRequest.of(0, 20)
      provider = serviceProviderFactory.create("test")
      intervention = interventionFactory.create(contract = contractFactory.create(primeProvider = provider))

      val serviceUserData = serviceUserDataFactory.create(firstName = "john", lastName = "smith")
      referral = referralFactory.createSent(id = serviceUserData.draftReferral!!.id, intervention = intervention, createDraft = false)
      referral.serviceUserData = serviceUserData

      val serviceUserData2 = serviceUserDataFactory.create(firstName = "john", lastName = "smith")
      referral2 = referralFactory.createSent(id = serviceUserData2.draftReferral!!.id, intervention = intervention, createDraft = false)
      referral2.serviceUserData = serviceUserData2

      val serviceUserData3 = serviceUserDataFactory.create(firstName = "tim", lastName = "brown")
      referral3 = referralFactory.createSent(id = serviceUserData3.draftReferral!!.id, intervention = intervention, createDraft = false)
      referral3.serviceUserData = serviceUserData3

      whenever(serviceProviderAccessScopeMapper.fromUser(user))
        .thenReturn(ServiceProviderAccessScope(setOf(provider), setOf(intervention.dynamicFrameworkContract)))
    }

    @Test
    fun `only referrals that have a person on probation matching the search text should be returned`() {
      val result = referralService.getSentReferralSummaryForUser(user, null, null, null, null, pageRequest, "john smith")

      assertThat(result).hasSize(2)
        .extracting("id")
        .contains(referral.id, referral2.id)
    }

    @Test
    fun `no referrals should be returned if no people on probation matches the search term`() {
      val result = referralService.getSentReferralSummaryForUser(user, null, null, null, null, pageRequest, "hello there")
      assertThat(result).size().isEqualTo(0)
    }

    @Test
    fun `hyphenated names are returned in the search results `() {
      val serviceUserData2 = serviceUserDataFactory.create(firstName = "bob-blue", lastName = "green")
      val referralWithHyphenatedName = referralFactory.createSent(id = serviceUserData2.draftReferral!!.id, intervention = intervention, createDraft = false)
      referralWithHyphenatedName.serviceUserData = serviceUserData2

      val result = referralService.getSentReferralSummaryForUser(user, null, null, null, null, pageRequest, "bob-blue green")
      assertThat(result).size().isEqualTo(1)
      assertThat(result.first().id).isEqualTo(referralWithHyphenatedName.id)
    }

    @Test
    fun `a search for an empty string should return no results`() {
      val result = referralService.getSentReferralSummaryForUser(user, null, null, null, null, pageRequest, "")
      assertThat(result).size().isEqualTo(0)
    }

    @Test
    fun `a search for a space should return no results`() {
      val result = referralService.getSentReferralSummaryForUser(user, null, null, null, null, pageRequest, " ")
      assertThat(result).size().isEqualTo(0)
    }
  }

  @Nested
  @DisplayName("get sent referrals with filter options")
  inner class GetSentReferralsFilterOptionsTest {
    lateinit var user: AuthUser
    lateinit var ppUser: AuthUser
    lateinit var otherUser: AuthUser
    lateinit var provider: ServiceProvider
    lateinit var completedReferral: Referral
    lateinit var liveReferral: Referral
    lateinit var cancelledReferral: Referral
    lateinit var draftReferral: DraftReferral
    lateinit var selfAssignedReferral: Referral
    lateinit var otherAssignedReferral: Referral
    lateinit var completedSentReferralSummary: SentReferralSummary
    lateinit var liveSentReferralSummary: SentReferralSummary
    lateinit var cancelledSentReferralSummary: SentReferralSummary
    lateinit var selfAssignedSentReferralSummary: SentReferralSummary
    lateinit var otherAssignedSentReferralSummary: SentReferralSummary
    lateinit var pageRequest: PageRequest

    private val truncateSeconds: Comparator<OffsetDateTime> = Comparator { a, exp ->
      if (exp != null && a != null) {
        if (a
          .truncatedTo(ChronoUnit.SECONDS)
          .isEqual(exp.truncatedTo(ChronoUnit.SECONDS))
        ) 0 else 1
      } else { 0 }
    }

    private val recursiveComparisonConfiguration: RecursiveComparisonConfiguration = recursiveComparisonConfigurationBuilder
      .withComparatorForType(truncateSeconds, OffsetDateTime::class.java)
      .build()

    @BeforeEach
    fun `setup referrals`() {
      user = userFactory.create("test_user", "auth")
      otherUser = userFactory.create(id = "randomId1236798", userName = "otherUserName")
      ppUser = userFactory.createPP()
      provider = serviceProviderFactory.create("test")
      val intervention = interventionFactory.create(contract = contractFactory.create(primeProvider = provider))

      completedReferral = referralFactory.createEnded(
        intervention = intervention,
        endRequestedReason = cancellationReasonFactory.create("ANY"),
        endRequestedAt = OffsetDateTime.now(),
        concludedAt = OffsetDateTime.now(),
      ).also { referral ->
        referral.endOfServiceReport = endOfServiceReportFactory.create(referral = referral)
      }

      completedSentReferralSummary = sentReferralSummariesFactory.getReferralSummary(completedReferral)

      liveReferral = referralFactory.createSent(
        intervention = intervention
      )

      liveSentReferralSummary = sentReferralSummariesFactory.getReferralSummary(liveReferral)

      cancelledReferral = referralFactory.createEnded(
        intervention = intervention,
        endRequestedReason = cancellationReasonFactory.create("ANY"),
        endRequestedAt = OffsetDateTime.now(),
        concludedAt = OffsetDateTime.now(),
        endOfServiceReport = null,
        actionPlans = mutableListOf(actionPlanFactory.createSubmitted())
      )

      cancelledSentReferralSummary = sentReferralSummariesFactory.getReferralSummary(cancelledReferral)

      draftReferral = referralFactory.createDraft(intervention = intervention)

      selfAssignedReferral = referralFactory.createAssigned(
        intervention = intervention,
        assignments = listOf(
          ReferralAssignment(OffsetDateTime.now(), user, user)
        )
      )

      selfAssignedSentReferralSummary = sentReferralSummariesFactory.getReferralSummary(selfAssignedReferral)

      otherAssignedReferral = referralFactory.createAssigned(
        intervention = intervention,
        assignments = listOf(
          ReferralAssignment(OffsetDateTime.now(), otherUser, otherUser)
        )
      )

      otherAssignedSentReferralSummary = sentReferralSummariesFactory.getReferralSummary(otherAssignedReferral)

      whenever(serviceProviderAccessScopeMapper.fromUser(user))
        .thenReturn(ServiceProviderAccessScope(setOf(provider), setOf(intervention.dynamicFrameworkContract)))

      pageRequest = PageRequest.of(0, 20)
    }

    @Test
    fun `by default only sent referrals are returned`() {
      val result = referralService.getSentReferralSummaryForUser(user, null, null, null, null, pageRequest)
      assertThat(result)
        .usingRecursiveFieldByFieldElementComparator(recursiveComparisonConfiguration)
        .containsExactlyInAnyOrder(
          completedSentReferralSummary,
          liveSentReferralSummary,
          cancelledSentReferralSummary,
          selfAssignedSentReferralSummary,
          otherAssignedSentReferralSummary
        )
    }

    @Test
    fun `to check for cancelled referral were pop did not attend appointment should not return for SP User`() {
      val intervention = interventionFactory.create(contract = contractFactory.create(primeProvider = provider))
      val cancelledReferral = referralFactory.createEnded(
        intervention = intervention,
        endRequestedReason = cancellationReasonFactory.create("ANY"),
        endRequestedAt = OffsetDateTime.now(),
        concludedAt = OffsetDateTime.now(),
        endOfServiceReport = null,
        actionPlans = mutableListOf(actionPlanFactory.create(submittedAt = null))
      )
      val appointment =
        appointmentFactory.create(referral = cancelledReferral, attendanceSubmittedAt = null)
      val supplierAssessmentAppointment =
        supplierAssessmentFactory.create(appointment = appointment, referral = cancelledReferral)
      cancelledReferral.supplierAssessment = supplierAssessmentAppointment
      entityManager.refresh(cancelledReferral)
      val result = referralService.getSentReferralSummaryForUser(user, null, null, null, null, pageRequest)
      assertThat(result)
        .usingRecursiveFieldByFieldElementComparator(recursiveComparisonConfiguration)
        .containsExactlyInAnyOrder(
          completedSentReferralSummary,
          liveSentReferralSummary,
          cancelledSentReferralSummary,
          selfAssignedSentReferralSummary,
          otherAssignedSentReferralSummary
        )
    }

    @Test
    fun `to check if unattended cancelled referral should not return with referrals sorted for SP User`() {
      val intervention = interventionFactory.create(contract = contractFactory.create(primeProvider = provider))
      val cancelledReferral = referralFactory.createEnded(
        intervention = intervention,
        endRequestedReason = cancellationReasonFactory.create("ANY"),
        endRequestedAt = OffsetDateTime.now(),
        concludedAt = OffsetDateTime.now(),
        endOfServiceReport = null,
        actionPlans = mutableListOf(actionPlanFactory.create(submittedAt = null))
      )
      val appointment =
        appointmentFactory.create(referral = cancelledReferral, attendanceSubmittedAt = null)
      val supplierAssessmentAppointment =
        supplierAssessmentFactory.create(appointment = appointment, referral = cancelledReferral)
      cancelledReferral.supplierAssessment = supplierAssessmentAppointment
      entityManager.refresh(cancelledReferral)
      val result = referralService.getSentReferralSummaryForUser(user, null, null, true, null, PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "assignments.assignedTo.userName")))
      assertThat(result)
        .usingRecursiveFieldByFieldElementComparator(recursiveComparisonConfiguration)
        .containsExactlyInAnyOrder(
          completedSentReferralSummary,
          liveSentReferralSummary,
          cancelledSentReferralSummary
        )
    }

    @Test
    fun `setting concluded returns only concluded referrals`() {
      val result = referralService.getSentReferralSummaryForUser(user, true, null, null, null, pageRequest)
      assertThat(result)
        .usingRecursiveFieldByFieldElementComparator(recursiveComparisonConfiguration)
        .containsExactlyInAnyOrder(completedSentReferralSummary, cancelledSentReferralSummary)
      assertThat(result).doesNotContain(liveSentReferralSummary, selfAssignedSentReferralSummary, otherAssignedSentReferralSummary)
    }

    @Test
    fun `setting not concluded returns only non concluded referrals`() {
      val result = referralService.getSentReferralSummaryForUser(user, false, null, null, null, pageRequest)
      assertThat(result)
        .usingRecursiveFieldByFieldElementComparator(recursiveComparisonConfiguration)
        .containsExactlyInAnyOrder(liveSentReferralSummary, selfAssignedSentReferralSummary, otherAssignedSentReferralSummary)
      assertThat(result).doesNotContain(completedSentReferralSummary, cancelledSentReferralSummary)
    }

    @Test
    fun `setting cancelled returns only cancelled referrals`() {
      val result = referralService.getSentReferralSummaryForUser(user, null, true, null, null, pageRequest)
      assertThat(result)
        .usingRecursiveFieldByFieldElementComparator(recursiveComparisonConfiguration)
        .containsExactlyInAnyOrder(cancelledSentReferralSummary)
      assertThat(result).doesNotContain(
        completedSentReferralSummary,
        liveSentReferralSummary,
        selfAssignedSentReferralSummary,
        otherAssignedSentReferralSummary
      )
    }

    @Test
    fun `setting not cancelled returns only non cancelled referrals`() {
      val result = referralService.getSentReferralSummaryForUser(user, null, false, null, null, pageRequest)

      assertThat(result)
        .usingRecursiveFieldByFieldElementComparator(recursiveComparisonConfiguration)
        .containsExactlyInAnyOrder(
          completedSentReferralSummary,
          liveSentReferralSummary,
          selfAssignedSentReferralSummary,
          otherAssignedSentReferralSummary
        )

      assertThat(result).doesNotContain(cancelledSentReferralSummary)
    }

    @Test
    fun `setting unassigned returns only unassigned referrals`() {
      val result = referralService.getSentReferralSummaryForUser(user, null, null, true, null, pageRequest)
      assertThat(result)
        .usingRecursiveFieldByFieldElementComparator(recursiveComparisonConfiguration)
        .containsExactlyInAnyOrder(completedSentReferralSummary, liveSentReferralSummary, cancelledSentReferralSummary)
      assertThat(result).doesNotContain(selfAssignedSentReferralSummary, otherAssignedSentReferralSummary)
    }

    @Test
    fun `setting not unassigned returns only referrals with assignments`() {
      val result = referralService.getSentReferralSummaryForUser(user, null, null, false, null, pageRequest)
      assertThat(result)
        .usingRecursiveFieldByFieldElementComparator(recursiveComparisonConfiguration)
        .containsExactlyInAnyOrder(selfAssignedSentReferralSummary, otherAssignedSentReferralSummary)
      assertThat(result).doesNotContain(completedSentReferralSummary, liveSentReferralSummary, cancelledSentReferralSummary)
    }

    @Test
    fun `setting assigned to returns referrals with correct assignments`() {
      val result = referralService.getSentReferralSummaryForUser(user, null, null, false, otherUser.id, pageRequest)
      assertThat(result)
        .usingRecursiveFieldByFieldElementComparator(recursiveComparisonConfiguration)
        .containsExactlyInAnyOrder(otherAssignedSentReferralSummary)
      assertThat(result).doesNotContain(
        completedSentReferralSummary,
        liveSentReferralSummary,
        cancelledSentReferralSummary,
        selfAssignedSentReferralSummary
      )
    }

    @Test
    fun `setting assigned to with unknown user returns no referrals`() {
      val result = referralService.getSentReferralSummaryForUser(user, null, null, false, "unknown", pageRequest)
      assertThat(result).isEmpty()
      assertThat(result).doesNotContain(
        completedSentReferralSummary,
        liveSentReferralSummary,
        cancelledSentReferralSummary,
        otherAssignedSentReferralSummary,
        selfAssignedSentReferralSummary
      )
    }

    @Test
    fun `setting all filter options to true will return cancelled and unassigned referral`() {
      val result = referralService.getSentReferralSummaryForUser(user, true, true, true, null, pageRequest)
      assertThat(result)
        .usingRecursiveFieldByFieldElementComparator(recursiveComparisonConfiguration)
        .containsExactlyInAnyOrder(cancelledSentReferralSummary)
      assertThat(result).doesNotContain(
        completedSentReferralSummary,
        liveSentReferralSummary,
        selfAssignedSentReferralSummary,
        otherAssignedSentReferralSummary
      )
    }

    @Test
    fun `setting all filter options to false will return an assigned referral`() {
      val result = referralService.getSentReferralSummaryForUser(user, false, false, false, user.id, pageRequest)
      assertThat(result)
        .usingRecursiveFieldByFieldElementComparator(recursiveComparisonConfiguration)
        .containsExactlyInAnyOrder(selfAssignedSentReferralSummary)
      assertThat(result).doesNotContain(
        completedSentReferralSummary,
        liveSentReferralSummary,
        cancelledSentReferralSummary,
        otherAssignedSentReferralSummary
      )
    }
  }

  @Test
  fun `ensure that desired outcomes are actually removed via orphan removal`() {

    val serviceCategoryId1 = UUID.randomUUID()
    val serviceCategoryId2 = UUID.randomUUID()
    val desiredOutcome1 = DesiredOutcome(UUID.randomUUID(), "title", serviceCategoryId = serviceCategoryId1)
    val desiredOutcome2 = DesiredOutcome(UUID.randomUUID(), "title", serviceCategoryId = serviceCategoryId2)
    val serviceCategory1 =
      serviceCategoryFactory.create(id = serviceCategoryId1, desiredOutcomes = mutableListOf(desiredOutcome1))
    val serviceCategory2 =
      serviceCategoryFactory.create(id = serviceCategoryId2, desiredOutcomes = mutableListOf(desiredOutcome2))

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

    draftReferralRepository.flush()
    val updatedReferral = draftReferralRepository.findById(referral.id).get()
    assertThat(updatedReferral.selectedServiceCategories).hasSize(1)
    assertThat(updatedReferral.selectedServiceCategories!!.elementAt(0).id).isEqualTo(serviceCategoryId2)
    assertThat(updatedReferral.selectedDesiredOutcomes).hasSize(0)
  }

  @Test
  fun `ensure that service categories constraint is not thrown when service categories is reselected with an already selected desired outcome`() {
    val serviceCategoryId = UUID.randomUUID()
    val desiredOutcome = DesiredOutcome(UUID.randomUUID(), "title", serviceCategoryId = serviceCategoryId)
    val serviceCategory =
      serviceCategoryFactory.create(id = serviceCategoryId, desiredOutcomes = mutableListOf(desiredOutcome))

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

    draftReferralRepository.flush()
    val updatedReferral = draftReferralRepository.findById(referral.id).get()
    assertThat(updatedReferral.selectedServiceCategories).hasSize(1)
    assertThat(updatedReferral.selectedServiceCategories!!.elementAt(0).id).isEqualTo(serviceCategoryId)
    assertThat(updatedReferral.selectedDesiredOutcomes).hasSize(1)
  }

  @Test
  fun `check when user is not the same as the responsible officer`() {
    val responsibleProbationPractitioner = ResponsibleProbationPractitioner("abc", "abc@abc.com", null, AuthUser("123456", "delius", "TEST_INTERVENTIONS_SP_1"), "def")
    val authUser = AuthUser("123457", "delius", "bernard.beaks")

    val isUserTheResponsibleOfficer = referralService.isUserTheResponsibleOfficer(responsibleProbationPractitioner, authUser)

    assertFalse(isUserTheResponsibleOfficer)
  }

  @Test
  fun `check when user is the same as the responsible officer`() {
    val authUser = AuthUser("123457", "delius", "bernard.beaks")
    val responsibleProbationPractitioner = ResponsibleProbationPractitioner("abc", "abc@abc.com", null, authUser, "def")

    val isUserTheResponsibleOfficer = referralService.isUserTheResponsibleOfficer(responsibleProbationPractitioner, authUser)

    assertTrue(isUserTheResponsibleOfficer)
  }

  @Test
  fun `check when user is not the same as the responsible officer and has a valid deliusStaffId `() {
    val responsibleProbationPractitioner = ResponsibleProbationPractitioner("abc", "abc@abc.com", 768912, AuthUser("123456", "delius", "TEST_INTERVENTIONS_SP_1"), "def")
    val authUser = AuthUser("123457", "delius", "bernard.beaks")
    whenever(communityAPIOffenderService.getStaffIdentifier(any())).thenReturn(768912)

    val isUserTheResponsibleOfficer = referralService.isUserTheResponsibleOfficer(responsibleProbationPractitioner, authUser)

    assertTrue(isUserTheResponsibleOfficer)
  }

  @Test
  fun `check when user is not the same as the responsible officer and does not have a valid deliusStaffId `() {
    val responsibleProbationPractitioner = ResponsibleProbationPractitioner("abc", "abc@abc.com", 768912, AuthUser("123456", "delius", "TEST_INTERVENTIONS_SP_1"), "def")
    val authUser = AuthUser("123457", "delius", "bernard.beaks")
    whenever(communityAPIOffenderService.getStaffIdentifier(any())).thenReturn(768913)

    val isUserTheResponsibleOfficer = referralService.isUserTheResponsibleOfficer(responsibleProbationPractitioner, authUser)

    assertFalse(isUserTheResponsibleOfficer)
  }
}
