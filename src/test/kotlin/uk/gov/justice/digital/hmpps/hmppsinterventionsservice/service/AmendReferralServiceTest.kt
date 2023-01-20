package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.server.ResponseStatusException
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.authorization.UserMapper
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.AmendDesiredOutcomesDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.AmendNeedsAndRequirementsDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.ReferralAmendmentDetails
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.UpdateReferralDetailsDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.ReferralEventPublisher
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Changelog
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ComplexityLevel
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.DesiredOutcome
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ActionPlanRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.AuthUserRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ChangelogRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ComplexityLevelRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.DeliverySessionRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.DesiredOutcomeRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.EndOfServiceReportRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.InterventionRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ReferralRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ServiceCategoryRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.AuthUserFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ChangeLogFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.InterventionFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ReferralDetailsFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ReferralFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.RepositoryTest
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ServiceCategoryFactory
import java.time.OffsetDateTime
import java.util.Optional
import java.util.UUID

@RepositoryTest
class AmendReferralServiceTest @Autowired constructor(
  val entityManager: TestEntityManager,
  val referralRepository: ReferralRepository,
  val authUserRepository: AuthUserRepository,
  val interventionRepository: InterventionRepository,
  val deliverySessionRepository: DeliverySessionRepository,
  val actionPlanRepository: ActionPlanRepository,
  val endOfServiceReportRepository: EndOfServiceReportRepository,
  val serviceCategoryRepository: ServiceCategoryRepository,
  val desiredOutcomeRepository: DesiredOutcomeRepository,
  val changelogRepository: ChangelogRepository
) {

  private val referralEventPublisher: ReferralEventPublisher = mock()
  private val userFactory = AuthUserFactory(entityManager)
  private val referralFactory = ReferralFactory(entityManager)
  private val interventionFactory = InterventionFactory(entityManager)
  private val referralDetailsFactory = ReferralDetailsFactory(entityManager)
  private val serviceCategoryFactory = ServiceCategoryFactory(entityManager)
  private val changeLogFactory = ChangeLogFactory(entityManager)
  private val referralService: ReferralService = mock()
  private val complexityLevelRepository: ComplexityLevelRepository = mock()

  private val userMapper: UserMapper = mock()
  private val jwtAuthenticationToken = JwtAuthenticationToken(mock())
  private val amendReferralService = AmendReferralService(
    referralEventPublisher,
    changelogRepository,
    referralRepository,
    serviceCategoryRepository,
    complexityLevelRepository,
    desiredOutcomeRepository,
    userMapper,
    referralService
  )

  @AfterEach
  fun `clear referrals`() {
    referralRepository.deleteAll()
    changelogRepository.deleteAll()
    complexityLevelRepository.deleteAll()
    desiredOutcomeRepository.deleteAll()
  }

  @Test
  fun `amend desired outcomes of a service category for a referral`() {
    val someoneElse = userFactory.create("helper_pp_user", "delius")
    val user = userFactory.create("pp_user_1", "delius")
    val serviceCategoryId = UUID.randomUUID()
    val desiredOutcome1 = DesiredOutcome(UUID.randomUUID(), "title", serviceCategoryId = serviceCategoryId)
    val desiredOutcome2 = DesiredOutcome(UUID.randomUUID(), "title", serviceCategoryId = serviceCategoryId)

    val serviceCategory = serviceCategoryFactory.create(
      id = serviceCategoryId,
      desiredOutcomes = listOf(desiredOutcome1, desiredOutcome2)
    )
    val referral = referralFactory.createSent(
      selectedServiceCategories = mutableSetOf(serviceCategory),
      desiredOutcomes = mutableListOf(desiredOutcome1),
      createdBy = someoneElse
    )
    whenever(userMapper.fromToken(jwtAuthenticationToken)).thenReturn(user)
    whenever(referralService.getSentReferralForUser(any(), any())).thenReturn(referral)

    amendReferralService.updateReferralDesiredOutcomes(
      referral.id,
      AmendDesiredOutcomesDTO(listOf(desiredOutcome2.id), "needs changing"),
      jwtAuthenticationToken,
      serviceCategory.id
    )
    val changelog = entityManager.entityManager.createQuery("FROM Changelog u WHERE u.referralId = :referralId")
      .setParameter("referralId", referral.id)
      .singleResult as Changelog

    assertThat(changelog.newVal.values.size).isEqualTo(1)
    assertThat(changelog.newVal.values).containsExactly(desiredOutcome2.id.toString())
    assertThat(changelog.reasonForChange).isEqualTo("needs changing")

    val newReferral = referralRepository.findById(referral.id).get()

    assertThat(newReferral.selectedDesiredOutcomes!!.size).isEqualTo(1)
    assertThat(newReferral.selectedDesiredOutcomes!![0].serviceCategoryId).isEqualTo(serviceCategory.id)
    assertThat(newReferral.selectedDesiredOutcomes!![0].desiredOutcomeId).isEqualTo(desiredOutcome2.id)
  }

  @Test
  fun `amend needs and requirements for employment responsibilities `() {
    val someoneElse = userFactory.create("helper_pp_user", "delius")
    val user = userFactory.create("pp_user_1", "delius")

    val referral = referralFactory.createSent(
      hasAdditionalResponsibilities = false,
      createdBy = someoneElse
    )
    whenever(userMapper.fromToken(jwtAuthenticationToken)).thenReturn(user)
    whenever(referralService.getSentReferralForUser(any(), any())).thenReturn(referral)

    amendReferralService.amendCaringOrEmploymentResponsibilities(
      referral.id,
      AmendNeedsAndRequirementsDTO(hasAdditionalResponsibilities = true, whenUnavailable = "9-12AM", reasonForChange = "needs changing"),
      jwtAuthenticationToken
    )
    val changelog = entityManager.entityManager.createQuery("FROM Changelog u WHERE u.referralId = :referralId")
      .setParameter("referralId", referral.id)
      .singleResult as Changelog

    assertThat(changelog.newVal.values.size).isEqualTo(2)
    assertThat(changelog.newVal.values).contains("true", "9-12AM")
    assertThat(changelog.oldVal.values).contains("false")

    assertThat(changelog.reasonForChange).isEqualTo("needs changing")

    val newReferral = referralRepository.findById(referral.id).get()

    assertThat(newReferral.hasAdditionalResponsibilities).isTrue
    assertThat(newReferral.whenUnavailable).isEqualTo("9-12AM")
  }
  @Test
  fun `amend needs and requirements for accessibility needs `() {
    val someoneElse = userFactory.create("helper_pp_user", "delius")
    val user = userFactory.create("pp_user_1", "delius")

    val referral = referralFactory.createSent(
      accessibilityNeeds = "school",
      createdBy = someoneElse
    )
    whenever(userMapper.fromToken(jwtAuthenticationToken)).thenReturn(user)
    whenever(referralService.getSentReferralForUser(any(), any())).thenReturn(referral)

    amendReferralService.amendAccessibilityNeeds(
      referral.id,
      AmendNeedsAndRequirementsDTO(accessibilityNeeds = "Home", reasonForChange = "needs changing"),
      jwtAuthenticationToken
    )
    val changelog = entityManager.entityManager.createQuery("FROM Changelog u WHERE u.referralId = :referralId")
      .setParameter("referralId", referral.id)
      .singleResult as Changelog

    assertThat(changelog.newVal.values.size).isEqualTo(1)
    assertThat(changelog.newVal.values).contains("Home")
    assertThat(changelog.reasonForChange).isEqualTo("needs changing")
    val newReferral = referralRepository.findById(referral.id).get()
    assertThat(newReferral.accessibilityNeeds).isEqualTo("Home")
  }

  @Test
  fun `amend needs and requirements for identify needs `() {
    val someoneElse = userFactory.create("helper_pp_user", "delius")
    val user = userFactory.create("pp_user_1", "delius")

    val referral = referralFactory.createSent(
      additionalNeedsInformation = "school",
      createdBy = someoneElse
    )
    whenever(userMapper.fromToken(jwtAuthenticationToken)).thenReturn(user)
    whenever(referralService.getSentReferralForUser(any(), any())).thenReturn(referral)

    amendReferralService.amendIdentifyNeeds(
      referral.id,
      AmendNeedsAndRequirementsDTO(additionalNeedsInformation = "Home", reasonForChange = "needs changing"),
      jwtAuthenticationToken
    )
    val changelog = entityManager.entityManager.createQuery("FROM Changelog u WHERE u.referralId = :referralId")
      .setParameter("referralId", referral.id)
      .singleResult as Changelog

    assertThat(changelog.newVal.values.size).isEqualTo(1)
    assertThat(changelog.newVal.values).contains("Home")
    assertThat(changelog.reasonForChange).isEqualTo("needs changing")
    val newReferral = referralRepository.findById(referral.id).get()
    assertThat(newReferral.additionalNeedsInformation).isEqualTo("Home")
  }

  @Test
  fun `amend needs and requirements for interpreter required `() {
    val someoneElse = userFactory.create("helper_pp_user", "delius")
    val user = userFactory.create("pp_user_1", "delius")

    val referral = referralFactory.createSent(
      needsInterpreter = false,
      createdBy = someoneElse
    )
    whenever(userMapper.fromToken(jwtAuthenticationToken)).thenReturn(user)
    whenever(referralService.getSentReferralForUser(any(), any())).thenReturn(referral)

    amendReferralService.amendInterpreterRequired(
      referral.id,
      AmendNeedsAndRequirementsDTO(needsInterpreter = true, interpreterLanguage = "Yoruba", reasonForChange = "needs changing"),
      jwtAuthenticationToken
    )
    val changelog = entityManager.entityManager.createQuery("FROM Changelog u WHERE u.referralId = :referralId")
      .setParameter("referralId", referral.id)
      .singleResult as Changelog

    assertThat(changelog.newVal.values.size).isEqualTo(2)
    assertThat(changelog.newVal.values).contains("true", "Yoruba")
    assertThat(changelog.oldVal.values).contains("false")

    assertThat(changelog.reasonForChange).isEqualTo("needs changing")

    val newReferral = referralRepository.findById(referral.id).get()

    assertThat(newReferral.needsInterpreter).isTrue
    assertThat(newReferral.interpreterLanguage).isEqualTo("Yoruba")
  }

  @Test
  fun `find changelog by changelogId`() {

    val someoneElse = userFactory.create("helper_pp_user", "delius")
    val user = userFactory.create("pp_user_1", "delius")

    val complexityLevel1 = ComplexityLevel(UUID.randomUUID(), "medium complexity", "some description")
    val complexityLevel2 = ComplexityLevel(UUID.randomUUID(), "low complexity", "some description")

    val referral = referralFactory.createSent(
      needsInterpreter = false,
      createdBy = someoneElse
    )

    whenever(userMapper.fromToken(jwtAuthenticationToken)).thenReturn(user)
    whenever(complexityLevelRepository.findById(eq(complexityLevel1.id))).thenReturn(Optional.of(complexityLevel1))
    whenever(complexityLevelRepository.findById(eq(complexityLevel2.id))).thenReturn(Optional.of(complexityLevel2))

    val uuid1 = UUID.randomUUID()
    val uuid2 = UUID.randomUUID()
    val uuid3 = UUID.randomUUID()

    val changelog1 = changeLogFactory.create(
      id = uuid1,
      referralId = referral.id,
      topic = AmendTopic.NEEDS_AND_REQUIREMENTS_ACCESSIBILITY_NEEDS,
      oldVal = ReferralAmendmentDetails(listOf("wheelchair")),
      newVal = ReferralAmendmentDetails(listOf("hearing aid")),
      changedBy = someoneElse
    )
    val changelog2 = changeLogFactory.create(
      id = uuid2,
      referralId = referral.id,
      topic = AmendTopic.COMPLEXITY_LEVEL,
      oldVal = ReferralAmendmentDetails(listOf(complexityLevel1.id.toString())),
      newVal = ReferralAmendmentDetails(listOf(complexityLevel2.id.toString())),
      changedBy = user
    )
    changeLogFactory.create(
      id = uuid3,
      referralId = referral.id,
      topic = AmendTopic.DESIRED_OUTCOMES,
      oldVal = ReferralAmendmentDetails(listOf("301ead30-30a4-4c7c-8296-2768abfb59b5")),
      newVal = ReferralAmendmentDetails(listOf("65924ac6-9724-455b-ad30-906936291421")),
      changedBy = user
    )

    val changelog = amendReferralService.getChangeLogById(changelog2.id, jwtAuthenticationToken)

    assertThat(changelog.changelog).isEqualTo(changelog2)
    assertThat(changelog.oldValue).isEqualTo(complexityLevel1.title)
    assertThat(changelog.newValue).isEqualTo(complexityLevel2.title)
  }

  @Test
  fun `find changelog by changelogId for desired outcomes`() {

    val someoneElse = userFactory.create("helper_pp_user", "delius")
    val user = userFactory.create("pp_user_1", "delius")
    val serviceCategoryId = UUID.randomUUID()
    val desiredOutcome1 = DesiredOutcome(UUID.randomUUID(), "desiredOutcome1", serviceCategoryId = serviceCategoryId)
    val desiredOutcome2 = DesiredOutcome(UUID.randomUUID(), "desiredOutcome2", serviceCategoryId = serviceCategoryId)

    val serviceCategory = serviceCategoryFactory.create(
      id = serviceCategoryId,
      desiredOutcomes = listOf(desiredOutcome1, desiredOutcome2)
    )
    val referral = referralFactory.createSent(
      selectedServiceCategories = mutableSetOf(serviceCategory),
      desiredOutcomes = mutableListOf(desiredOutcome1, desiredOutcome2),
      createdBy = someoneElse
    )

    whenever(userMapper.fromToken(jwtAuthenticationToken)).thenReturn(user)

    val uuid1 = UUID.randomUUID()
    val uuid3 = UUID.randomUUID()

    val changelog1 = changeLogFactory.create(
      id = uuid1,
      referralId = referral.id,
      topic = AmendTopic.NEEDS_AND_REQUIREMENTS_ACCESSIBILITY_NEEDS,
      oldVal = ReferralAmendmentDetails(listOf("018e8ef3-9645-4712-898e-c913e2bb5bd9")),
      newVal = ReferralAmendmentDetails(listOf("hearing aid")),
      changedBy = someoneElse
    )
    val changelog2 = changeLogFactory.create(
      id = uuid3,
      referralId = referral.id,
      topic = AmendTopic.DESIRED_OUTCOMES,
      oldVal = ReferralAmendmentDetails(listOf(desiredOutcome1.id.toString())),
      newVal = ReferralAmendmentDetails(listOf(desiredOutcome2.id.toString())),
      changedBy = user
    )

    val changelog = amendReferralService.getChangeLogById(changelog2.id, jwtAuthenticationToken)

    assertThat(changelog.changelog).isEqualTo(changelog2)
    assertThat(changelog.oldValues).contains("desiredOutcome1")
    assertThat(changelog.newValues).contains("desiredOutcome2")
  }

  @Test
  fun `find changelog by changelogId for interpreter required`() {

    val someoneElse = userFactory.create("helper_pp_user", "delius")
    val user = userFactory.create("pp_user_1", "delius")

    val referral = referralFactory.createSent(
      needsInterpreter = true,
      interpreterLanguage = "french",
      createdBy = someoneElse
    )

    whenever(userMapper.fromToken(jwtAuthenticationToken)).thenReturn(user)

    val uuid1 = UUID.randomUUID()

    val changelog1 = changeLogFactory.create(
      id = uuid1,
      referralId = referral.id,
      topic = AmendTopic.NEEDS_AND_REQUIREMENTS_INTERPRETER_REQUIRED,
      oldVal = ReferralAmendmentDetails(listOf("false")),
      newVal = ReferralAmendmentDetails(listOf("true", "French")),
      changedBy = someoneElse
    )

    val changelog = amendReferralService.getChangeLogById(changelog1.id, jwtAuthenticationToken)

    assertThat(changelog.changelog).isEqualTo(changelog1)
    assertThat(changelog.oldValue).contains("No")
    assertThat(changelog.newValue).contains("Yes-French")
  }

  @Test
  fun `find changelog by changelogId for employment responsibilities`() {

    val someoneElse = userFactory.create("helper_pp_user", "delius")
    val user = userFactory.create("pp_user_1", "delius")

    val referral = referralFactory.createSent(
      needsInterpreter = true,
      interpreterLanguage = "french",
      createdBy = someoneElse
    )

    whenever(userMapper.fromToken(jwtAuthenticationToken)).thenReturn(user)

    val uuid1 = UUID.randomUUID()

    val changelog1 = changeLogFactory.create(
      id = uuid1,
      referralId = referral.id,
      topic = AmendTopic.NEEDS_AND_REQUIREMENTS_HAS_ADDITIONAL_RESPONSIBILITIES,
      oldVal = ReferralAmendmentDetails(listOf("false")),
      newVal = ReferralAmendmentDetails(listOf("true", "will not available wednesday mornings")),
      changedBy = someoneElse
    )

    val changelog = amendReferralService.getChangeLogById(changelog1.id, jwtAuthenticationToken)

    assertThat(changelog.changelog).isEqualTo(changelog1)
    assertThat(changelog.oldValue).contains("No")
    assertThat(changelog.newValue).contains("Yes-will not available wednesday mornings")
  }

  @Test
  fun `find changelog by changelogId for completion date`() {

    val someoneElse = userFactory.create("helper_pp_user", "delius")
    val user = userFactory.create("pp_user_1", "delius")

    val referral = referralFactory.createSent(
      needsInterpreter = true,
      interpreterLanguage = "french",
      createdBy = someoneElse
    )

    whenever(userMapper.fromToken(jwtAuthenticationToken)).thenReturn(user)

    val uuid1 = UUID.randomUUID()

    val changelog1 = changeLogFactory.create(
      id = uuid1,
      referralId = referral.id,
      topic = AmendTopic.COMPLETION_DATETIME,
      oldVal = ReferralAmendmentDetails(listOf("2023-02-14")),
      newVal = ReferralAmendmentDetails(listOf("2023-03-14")),
      changedBy = someoneElse
    )

    val changelog = amendReferralService.getChangeLogById(changelog1.id, jwtAuthenticationToken)

    assertThat(changelog.changelog).isEqualTo(changelog1)
    assertThat(changelog.oldValue).contains("14 Feb 2023")
    assertThat(changelog.newValue).contains("14 Mar 2023")
  }

  @Test
  fun `find changelog throws 404 when changelog is not found`() {

    val someoneElse = userFactory.create("helper_pp_user", "delius")
    val user = userFactory.create("pp_user_1", "delius")

    val referral = referralFactory.createSent(
      needsInterpreter = false,
      createdBy = someoneElse
    )

    whenever(userMapper.fromToken(jwtAuthenticationToken)).thenReturn(user)
    whenever(referralService.getSentReferralForUser(any(), any())).thenReturn(referral)

    val uuid1 = UUID.randomUUID()
    val exception = assertThrows<ResponseStatusException> {
      amendReferralService.getChangeLogById(uuid1, jwtAuthenticationToken)
    }
    assertThat(exception.message).contains("Change log not found for [id=$uuid1]")
  }
  @Test
  fun `log change writes changelog data`() {
    val user = userFactory.create("pp_user_1", "delius")
    val description = "15 days is never enough"
    val id = UUID.randomUUID()
    val intervention = interventionFactory.create(description = description)

    val referral = referralFactory.createSent(
      createdAt = OffsetDateTime.now(),
      createdBy = user,
      id = id,
      sentAt = OffsetDateTime.now(),
      serviceUserCRN = "crn",
      intervention = intervention
    )
    val referralDetails = referralDetailsFactory.create(
      referralId = id,
      createdAt = OffsetDateTime.now(),
      createdBy = user,
      id = UUID.randomUUID(),
      maximumNumberOfEnforceableDays = 15,
      saved = true
    )

    val referralToUpdate = UpdateReferralDetailsDTO(20, null, "new information", "we decided 10 days wasn't enough")

    whenever(userMapper.fromToken(jwtAuthenticationToken)).thenReturn(user)
    whenever(referralService.getSentReferralForUser(any(), any())).thenReturn(referral)

    val uuid1 = UUID.randomUUID()

    amendReferralService.logChanges(referralDetails, referralToUpdate, user)
    val changeLogReturned = changelogRepository.findAll().filter { x -> x.referralId == id }
    assertThat(changeLogReturned.size).isEqualTo(1)

    assertThat(changeLogReturned.firstOrNull()?.referralId).isEqualTo(referral.id)
    assertThat(changeLogReturned.firstOrNull()?.newVal).isNotNull
    assertThat(changeLogReturned.firstOrNull()?.newVal?.values).isNotEmpty
    assertThat(changeLogReturned.firstOrNull()?.newVal?.values?.get(0)).isEqualTo("20")
    assertThat(changeLogReturned.firstOrNull()?.oldVal).isNotNull
    assertThat(changeLogReturned.firstOrNull()?.oldVal?.values).isNotEmpty
    assertThat(changeLogReturned.firstOrNull()?.oldVal?.values?.get(0)).isEqualTo(referralDetails.maximumEnforceableDays.toString())
    assertThat(changeLogReturned.firstOrNull()?.reasonForChange).isNotBlank
    assertThat(changeLogReturned.firstOrNull()?.topic).isEqualTo(AmendTopic.MAXIMUM_ENFORCEABLE_DAYS)
  }
}
