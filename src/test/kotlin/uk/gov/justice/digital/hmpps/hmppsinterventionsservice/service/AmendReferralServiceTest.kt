package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.authorization.UserMapper
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.AmendDesiredOutcomesDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.AmendNeedsAndRequirementsDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.ReferralEventPublisher
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Changelog
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.DesiredOutcome
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ActionPlanRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.AuthUserRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ChangelogRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.DeliverySessionRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.EndOfServiceReportRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.InterventionRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ReferralRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ServiceCategoryRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.AuthUserFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ReferralFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.RepositoryTest
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ServiceCategoryFactory
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
  val changelogRepository: ChangelogRepository
) {

  private val referralEventPublisher: ReferralEventPublisher = mock()
  private val userFactory = AuthUserFactory(entityManager)
  private val referralFactory = ReferralFactory(entityManager)
  private val serviceCategoryFactory = ServiceCategoryFactory(entityManager)
  private val referralService: ReferralService = mock()

  private val userMapper: UserMapper = mock()
  private val jwtAuthenticationToken = JwtAuthenticationToken(mock())
  private val amendReferralService = AmendReferralService(
    referralEventPublisher,
    changelogRepository,
    referralRepository,
    serviceCategoryRepository,
    userMapper,
    referralService
  )

  @AfterEach
  fun `clear referrals`() {
    referralRepository.deleteAll()
    changelogRepository.deleteAll()
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

    amendReferralService.updateAmendCaringOrEmploymentResponsibilitiesDTO(
      referral.id,
      AmendNeedsAndRequirementsDTO(true, "9-12AM", "needs changing"),
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
}
