package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.authorization.ReferralAccessChecker
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.authorization.ReferralAccessFilter
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
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.PersonCurrentLocationType
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Referral
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.SampleData
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ActionPlanRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.AuthUserRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.DeliverySessionRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.DraftReferralRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.EndOfServiceReportRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.InterventionRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ReferralDetailsRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ReferralLocationRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ReferralRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ServiceCategoryRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.AuthUserFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ContractTypeFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.DynamicFrameworkContractFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.InterventionFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ReferralFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.RepositoryTest
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ServiceCategoryFactory
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import kotlin.random.Random

@RepositoryTest
class DraftReferralServiceTest @Autowired constructor(
  val entityManager: TestEntityManager,
  val referralRepository: ReferralRepository,
  val draftReferralRepository: DraftReferralRepository,
  val authUserRepository: AuthUserRepository,
  val interventionRepository: InterventionRepository,
  val deliverySessionRepository: DeliverySessionRepository,
  val actionPlanRepository: ActionPlanRepository,
  val endOfServiceReportRepository: EndOfServiceReportRepository,
  val serviceCategoryRepository: ServiceCategoryRepository,
  val referralDetailsRepository: ReferralDetailsRepository,
  val referralLocationRepository: ReferralLocationRepository
) {
  private val userFactory = AuthUserFactory(entityManager)
  private val interventionFactory = InterventionFactory(entityManager)
  private val referralFactory = ReferralFactory(entityManager)
  private val serviceCategoryFactory = ServiceCategoryFactory(entityManager)
  private val contractTypeFactory = ContractTypeFactory(entityManager)
  private val dynamicFrameworkContractFactory = DynamicFrameworkContractFactory(entityManager)

  private val referralEventPublisher: ReferralEventPublisher = mock()
  private val referenceGenerator: ReferralReferenceGenerator = spy(ReferralReferenceGenerator())
  private val referralAccessChecker: ReferralAccessChecker = mock()
  private val userTypeChecker = UserTypeChecker()
  private val serviceProviderAccessScopeMapper: ServiceProviderAccessScopeMapper = mock()
  private val referralAccessFilter = ReferralAccessFilter(serviceProviderAccessScopeMapper)
  private val communityAPIReferralService: CommunityAPIReferralService = mock()
  private val serviceUserAccessChecker: ServiceUserAccessChecker = mock()
  private val assessRisksAndNeedsService: RisksAndNeedsService = mock()
  private val supplierAssessmentService: SupplierAssessmentService = mock()
  private val hmppsAuthService: HMPPSAuthService = mock()
  private val draftOasysRiskInformationService: DraftOasysRiskInformationService = mock()
  private var currentLocationEnabled: Boolean = true

  private val draftReferralService = DraftReferralService(
    referralRepository,
    draftReferralRepository,
    referralAccessFilter,
    referralAccessChecker,
    serviceUserAccessChecker,
    authUserRepository,
    interventionRepository,
    referralEventPublisher,
    referenceGenerator,
    deliverySessionRepository,
    serviceCategoryRepository,
    userTypeChecker,
    communityAPIReferralService,
    assessRisksAndNeedsService,
    supplierAssessmentService,
    hmppsAuthService,
    referralDetailsRepository,
    draftOasysRiskInformationService,
    referralLocationRepository,
    currentLocationEnabled
  )

  @AfterEach
  fun `clear referrals`() {
    entityManager.flush()
    interventionRepository.deleteAll()
    referralDetailsRepository.deleteAll()
    authUserRepository.deleteAll()
    draftReferralRepository.deleteAll()
    referralRepository.deleteAll()
    referralLocationRepository.deleteAll()
  }

  @Nested
  @DisplayName("create / find / update / send draft referrals")
  inner class CreateFindUpdateAndSendDraftReferrals {
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
    fun `update cannot overwrite identifier fields`() {
      val draftReferral = DraftReferralDTO(
        id = UUID.fromString("ce364949-7301-497b-894d-130f34a98bff"),
        createdAt = OffsetDateTime.of(LocalDate.of(2020, 12, 1), LocalTime.MIN, ZoneOffset.UTC)
      )

      val updated = draftReferralService.updateDraftReferral(sampleDraftReferral, draftReferral)
      assertThat(updated.id).isEqualTo(sampleDraftReferral.id)
      assertThat(updated.createdAt).isEqualTo(sampleDraftReferral.createdAt)
    }

    @Test
    fun `null fields in the update do not overwrite original fields`() {
      val sentenceId = Random.nextLong()
      sampleDraftReferral.relevantSentenceId = sentenceId
      entityManager.persistAndFlush(sampleDraftReferral)

      val draftReferral = DraftReferralDTO(relevantSentenceId = null)

      val updated = draftReferralService.updateDraftReferral(sampleDraftReferral, draftReferral)
      assertThat(updated.relevantSentenceId).isEqualTo(sentenceId)
    }

    @Test
    fun `non-null fields in the update overwrite original fields`() {
      val existingSentenceId = Random.nextLong()
      val newSentenceId = Random.nextLong()
      sampleDraftReferral.relevantSentenceId = existingSentenceId
      entityManager.persistAndFlush(sampleDraftReferral)

      val draftReferral = DraftReferralDTO(relevantSentenceId = newSentenceId)

      val updated = draftReferralService.updateDraftReferral(sampleDraftReferral, draftReferral)
      assertThat(updated.relevantSentenceId).isEqualTo(newSentenceId)
    }

    @Test
    fun `update mutates the original object`() {
      val existingSentenceId = Random.nextLong()
      val newSentenceId = Random.nextLong()
      sampleDraftReferral.relevantSentenceId = existingSentenceId
      entityManager.persistAndFlush(sampleDraftReferral)

      val draftReferral = DraftReferralDTO(relevantSentenceId = newSentenceId)

      val updated = draftReferralService.updateDraftReferral(sampleDraftReferral, draftReferral)
      assertThat(updated.relevantSentenceId).isEqualTo(newSentenceId)
    }

    @Test
    fun `update successfully persists the updated draft referral`() {
      val existingSentenceId = Random.nextLong()
      val newSentenceId = Random.nextLong()
      sampleDraftReferral.relevantSentenceId = existingSentenceId
      entityManager.persistAndFlush(sampleDraftReferral)

      val draftReferral = DraftReferralDTO(relevantSentenceId = newSentenceId)
      draftReferralService.updateDraftReferral(sampleDraftReferral, draftReferral)
      val savedDraftReferral = draftReferralService.getDraftReferralForUser(sampleDraftReferral.id, userFactory.create())
      assertThat(savedDraftReferral).isNotNull
      assertThat(savedDraftReferral?.id).isEqualTo(sampleDraftReferral.id)
      assertThat(savedDraftReferral?.createdAt).isEqualTo(sampleDraftReferral.createdAt)
      assertThat(savedDraftReferral?.relevantSentenceId).isEqualTo(draftReferral.relevantSentenceId)
    }

    @Test
    fun `create and persist non-cohort draft referral`() {
      val authUser = AuthUser("user_id", "delius", "user_name")
      val draftReferral = draftReferralService.createDraftReferral(authUser, "X123456", sampleIntervention.id)
      entityManager.flush()

      val savedDraftReferral = draftReferralService.getDraftReferralForUser(draftReferral.id, authUser)
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
      val draftReferral = draftReferralService.createDraftReferral(authUser, "X123456", sampleCohortIntervention.id)
      entityManager.flush()

      val savedDraftReferral = draftReferralService.getDraftReferralForUser(draftReferral.id, authUser)
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

      val savedDraftReferral = draftReferralService.getDraftReferralForUser(sampleDraftReferral.id, userFactory.create())
      assertThat(savedDraftReferral?.id).isEqualTo(sampleDraftReferral.id)
      assertThat(savedDraftReferral?.createdAt).isEqualTo(sampleDraftReferral.createdAt)
      assertThat(savedDraftReferral?.relevantSentenceId).isEqualTo(sampleDraftReferral.relevantSentenceId)
    }

    @Test
    fun `find by userID returns list of draft referrals`() {
      val user1 = AuthUser("123", "delius", "bernie.b")
      val user2 = AuthUser("456", "delius", "sheila.h")
      val user3 = AuthUser("789", "delius", "tom.myers")
      draftReferralService.createDraftReferral(user1, "X123456", sampleIntervention.id)
      draftReferralService.createDraftReferral(user1, "X123456", sampleIntervention.id)
      draftReferralService.createDraftReferral(user2, "X123456", sampleIntervention.id)
      entityManager.flush()

      val single = draftReferralService.getDraftReferralsForUser(user2)
      assertThat(single).hasSize(1)

      val multiple = draftReferralService.getDraftReferralsForUser(user1)
      assertThat(multiple).hasSize(2)

      val none = draftReferralService.getDraftReferralsForUser(user3)
      assertThat(none).hasSize(0)
    }

    @Test
    fun `completion date must be in the future`() {
      val update = DraftReferralDTO(completionDeadline = LocalDate.of(2020, 1, 1))
      val error = assertThrows<ValidationError> {
        draftReferralService.updateDraftReferral(sampleDraftReferral, update)
      }
      assertThat(error.errors.size).isEqualTo(1)
      assertThat(error.errors[0].field).isEqualTo("completionDeadline")
    }

    @Test
    fun `when needsInterpreter is true, interpreterLanguage must be set`() {
      // this is fine
      draftReferralService.updateDraftReferral(sampleDraftReferral, DraftReferralDTO(needsInterpreter = false))

      val error = assertThrows<ValidationError> {
        // this throws ValidationError
        draftReferralService.updateDraftReferral(sampleDraftReferral, DraftReferralDTO(needsInterpreter = true))
      }
      assertThat(error.errors.size).isEqualTo(1)
      assertThat(error.errors[0].field).isEqualTo("needsInterpreter")

      // this is also fine
      draftReferralService.updateDraftReferral(
        sampleDraftReferral,
        DraftReferralDTO(needsInterpreter = true, interpreterLanguage = "German")
      )
    }

    @Test
    fun `when hasAdditionalResponsibilities is true, whenUnavailable must be set`() {
      // this is fine
      draftReferralService.updateDraftReferral(sampleDraftReferral, DraftReferralDTO(hasAdditionalResponsibilities = false))

      val error = assertThrows<ValidationError> {
        // this throws ValidationError
        draftReferralService.updateDraftReferral(sampleDraftReferral, DraftReferralDTO(hasAdditionalResponsibilities = true))
      }
      assertThat(error.errors.size).isEqualTo(1)
      assertThat(error.errors[0].field).isEqualTo("hasAdditionalResponsibilities")

      // this is also fine
      draftReferralService.updateDraftReferral(
        sampleDraftReferral,
        DraftReferralDTO(hasAdditionalResponsibilities = true, whenUnavailable = "wednesdays")
      )
    }

    @Test
    fun `multiple errors at once`() {
      val update = DraftReferralDTO(completionDeadline = LocalDate.of(2020, 1, 1), needsInterpreter = true)
      val error = assertThrows<ValidationError> {
        draftReferralService.updateDraftReferral(sampleDraftReferral, update)
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
        draftReferralService.updateDraftReferral(sampleDraftReferral, update)
      }

      entityManager.flush()
      val draftReferral = draftReferralService.getDraftReferralForUser(
        sampleDraftReferral.id,
        userFactory.create()
      )
      assertThat(draftReferral?.additionalNeedsInformation).isNull()
    }

    @Test
    fun `once a draft referral is sent to referral it's id is still valid in draft referral`() {
      val user = AuthUser("user_id", "delius", "user_name")
      val draftReferral = draftReferralService.createDraftReferral(user, "X123456", sampleIntervention.id)
      draftReferral.additionalRiskInformation = "risk"
      draftReferral.additionalRiskInformationUpdatedAt = OffsetDateTime.now()
      draftReferral.personCurrentLocationType = PersonCurrentLocationType.CUSTODY
      draftReferral.personCustodyPrisonId = "ABC"

      assertThat(draftReferralService.getDraftReferralForUser(draftReferral.id, user)).isNotNull

      draftReferralService.sendDraftReferral(draftReferral, user)

      assertThat(draftReferralService.getDraftReferralForUser(draftReferral.id, user)).isNotNull
    }

    @Test
    fun `sending a draft referral generates a referral reference number`() {
      val user = AuthUser("user_id", "delius", "user_name")
      val draftReferral = draftReferralService.createDraftReferral(user, "X123456", sampleIntervention.id)
      draftReferral.additionalRiskInformation = "risk"
      draftReferral.additionalRiskInformationUpdatedAt = OffsetDateTime.now()
      draftReferral.personCurrentLocationType = PersonCurrentLocationType.CUSTODY
      draftReferral.personCustodyPrisonId = "ABC"

      val sentReferral = draftReferralService.sendDraftReferral(draftReferral, user)
      assertThat(sentReferral.referenceNumber).isNotNull
    }

    @Test
    fun `sending a draft referral creates a referral location`() {
      val user = AuthUser("user_id", "delius", "user_name")
      val draftReferral = draftReferralService.createDraftReferral(user, "X123456", sampleIntervention.id)
      draftReferral.additionalRiskInformation = "risk"
      draftReferral.additionalRiskInformationUpdatedAt = OffsetDateTime.now()

      val personCurrentLocationType = PersonCurrentLocationType.CUSTODY
      val personCustodyPrisonId = "ABC"
      draftReferral.personCurrentLocationType = personCurrentLocationType
      draftReferral.personCustodyPrisonId = personCustodyPrisonId

      val sentReferral = draftReferralService.sendDraftReferral(draftReferral, user)
      assertThat(referralLocationRepository.findByReferralId(sentReferral.id)?.referral).isEqualTo(sentReferral)
      assertThat(referralLocationRepository.findByReferralId(sentReferral.id)?.prisonId).isEqualTo(personCustodyPrisonId)
      assertThat(referralLocationRepository.findByReferralId(sentReferral.id)?.type).isEqualTo(personCurrentLocationType)
    }

    @Test
    fun `sending a draft referral creates a referral with referral location`() {
      val user = AuthUser("user_id", "delius", "user_name")
      val draftReferral = draftReferralService.createDraftReferral(user, "X123456", sampleIntervention.id)
      draftReferral.additionalRiskInformation = "risk"
      draftReferral.additionalRiskInformationUpdatedAt = OffsetDateTime.now()

      val personCurrentLocationType = PersonCurrentLocationType.CUSTODY
      val personCustodyPrisonId = "ABC"
      draftReferral.personCurrentLocationType = personCurrentLocationType
      draftReferral.personCustodyPrisonId = personCustodyPrisonId

      val sentReferral = draftReferralService.sendDraftReferral(draftReferral, user)
      assertThat(referralRepository.findById(sentReferral.id).get().referralLocation?.prisonId).isEqualTo(personCustodyPrisonId)
      assertThat(referralRepository.findById(sentReferral.id).get().referralLocation?.type).isEqualTo(personCurrentLocationType)
    }

    @Test
    fun `sending a draft referral generates a unique reference, even if the previous reference already exists`() {
      val user = AuthUser("user_id", "delius", "user_name")
      val draft1 = draftReferralService.createDraftReferral(user, "X123456", sampleIntervention.id)
      val draft2 = draftReferralService.createDraftReferral(user, "X123456", sampleIntervention.id)
      draft1.additionalRiskInformation = "risk"
      draft1.additionalRiskInformationUpdatedAt = OffsetDateTime.now()
      draft1.personCurrentLocationType = PersonCurrentLocationType.CUSTODY
      draft1.personCustodyPrisonId = "ABC"
      draft2.additionalRiskInformation = "risk"
      draft2.additionalRiskInformationUpdatedAt = OffsetDateTime.now()
      draft2.personCurrentLocationType = PersonCurrentLocationType.CUSTODY
      draft2.personCustodyPrisonId = "ABC"

      whenever(referenceGenerator.generate(sampleIntervention.dynamicFrameworkContract.contractType.name))
        .thenReturn("AA0000ZZ", "AA0000ZZ", "AA0000ZZ", "AA0000ZZ", "BB0000ZZ")

      val sent1 = draftReferralService.sendDraftReferral(draft1, user)
      assertThat(sent1.referenceNumber).isEqualTo("AA0000ZZ")

      val sent2 = draftReferralService.sendDraftReferral(draft2, user)
      assertThat(sent2.referenceNumber).isNotEqualTo("AA0000ZZ").isEqualTo("BB0000ZZ")
    }

    @Test
    fun `sending a draft referral triggers an event`() {
      val user = AuthUser("user_id", "delius", "user_name")
      val draftReferral = draftReferralService.createDraftReferral(user, "X123456", sampleIntervention.id)
      draftReferral.additionalRiskInformation = "risk"
      draftReferral.additionalRiskInformationUpdatedAt = OffsetDateTime.now()
      draftReferral.personCurrentLocationType = PersonCurrentLocationType.CUSTODY
      draftReferral.personCustodyPrisonId = "ABC"

      val referral = draftReferralService.sendDraftReferral(draftReferral, user)
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
        assertDoesNotThrow { draftReferralService.createDraftReferral(user, "X123456", sampleIntervention.id) }
      }
      assertThat(draftReferralService.getDraftReferralsForUser(user)).hasSize(3)
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
    draftReferralService.updateDraftReferral(referral, DraftReferralDTO(serviceCategoryIds = listOf(serviceCategoryId2)))

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
    draftReferralService.updateDraftReferral(referral, DraftReferralDTO(serviceCategoryIds = listOf(serviceCategoryId)))

    draftReferralRepository.flush()
    val updatedReferral = draftReferralRepository.findById(referral.id).get()
    assertThat(updatedReferral.selectedServiceCategories).hasSize(1)
    assertThat(updatedReferral.selectedServiceCategories!!.elementAt(0).id).isEqualTo(serviceCategoryId)
    assertThat(updatedReferral.selectedDesiredOutcomes).hasSize(1)
  }
}
