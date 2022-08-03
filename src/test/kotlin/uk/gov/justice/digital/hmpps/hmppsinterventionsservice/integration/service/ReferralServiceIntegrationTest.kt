package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.integration.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.authorization.ReferralAccessChecker
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.authorization.ReferralAccessFilter
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.authorization.ServiceProviderAccessScopeMapper
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.authorization.ServiceUserAccessChecker
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.authorization.UserMapper
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.authorization.UserTypeChecker
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.UpdateReferralDetailsDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.ReferralEventPublisher
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AuthUser
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ActionPlanRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.AuthUserRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.CancellationReasonRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ChangelogRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.DeliverySessionRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.EndOfServiceReportRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.InterventionRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ReferralDetailsRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ReferralRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.SentReferralSummariesRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ServiceCategoryRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.AmendTopic
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.CommunityAPIOffenderService
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.CommunityAPIReferralService
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.DraftOasysRiskInformationService
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.HMPPSAuthService
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.ReferralConcluder
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.ReferralReferenceGenerator
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.ReferralService
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.RisksAndNeedsService
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.SupplierAssessmentService
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.TelemetryService
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.AuthUserFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.InterventionFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ReferralDetailsFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ReferralFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.RepositoryTest
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@RepositoryTest
class ReferralServiceIntegrationTest @Autowired constructor(
  val entityManager: TestEntityManager,
  val referralRepository: ReferralRepository,
  val referralDetailsRepository: ReferralDetailsRepository,
  val sentReferralSummariesRepository: SentReferralSummariesRepository,
  val authUserRepository: AuthUserRepository,
  val interventionRepository: InterventionRepository,
  val cancellationReasonRepository: CancellationReasonRepository,
  val deliverySessionRepository: DeliverySessionRepository,
  val actionPlanRepository: ActionPlanRepository,
  val endOfServiceReportRepository: EndOfServiceReportRepository,
  val serviceCategoryRepository: ServiceCategoryRepository,
  val changelogRepository: ChangelogRepository
) {

  private val userFactory = AuthUserFactory(entityManager)
  private val interventionFactory = InterventionFactory(entityManager)
  private val referralFactory = ReferralFactory(entityManager)
  private val referralDetailsFactory = ReferralDetailsFactory(entityManager)
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
  private val userMapper: UserMapper = mock()
  private val jwtAuthentionToken: JwtAuthenticationToken = mock()
  private val referralService = ReferralService(
    referralRepository,
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
    changelogRepository
  )

  fun setup() {
    entityManager.flush()
    referralRepository.deleteAll()
    interventionRepository.deleteAll()
    referralDetailsRepository.deleteAll()
    authUserRepository.deleteAll()
    changelogRepository.deleteAll()
  }
  @Test
  fun `updateReferralDetails updates futher information and completion deadline update when referral details exists`() {
    val authUser = AuthUser("123457", "delius", "bernard.beaks")
    val user = userFactory.create("pp_user_1", "delius")
    val description = ""
    val id = UUID.randomUUID()
    val completionDateToChange = LocalDate.of(2022, 7, 1)
    val intervention = interventionFactory.create(description = description)
    val referral = referralFactory.createSent(
      createdAt = OffsetDateTime.now(),
      createdBy = user,
      id = id,
      sentAt = null,
      serviceUserCRN = "crn",
      intervention = intervention,
      completionDeadline = LocalDate.of(2022, 2, 2)
    )
    referralDetailsFactory.create(referralId = id, createdAt = OffsetDateTime.now(), createdBy = authUser, id = UUID.randomUUID(), completionDeadline = LocalDate.of(2022, 8, 2), saved = true)
    whenever(userMapper.fromToken(jwtAuthentionToken)).thenReturn(authUser)

    val referralToUpdate = UpdateReferralDetailsDTO(20, completionDateToChange, "new information", "we decided 10 days wasn't enough")
    var referralDetailsReturned = referralService.updateReferralDetails(referral, referralToUpdate, user)
    val referralDetailsValue = referralService.getReferralDetailsById(referralDetailsReturned?.id)
    val changeLogReturned = changelogRepository.findAll().singleOrNull { x -> x.referralId == id }

    assertThat(referralDetailsValue?.referralId).isEqualTo(referral.id)
    assertThat(referralToUpdate.furtherInformation).isEqualTo(referralDetailsValue?.furtherInformation)
    assertThat(referralToUpdate.completionDeadline).isEqualTo(referralDetailsValue?.completionDeadline)
    assertThat(changeLogReturned?.referralId).isEqualTo(referral.id)
    assertThat(changeLogReturned?.newVal).isNotNull
    assertThat(changeLogReturned?.newVal?.values).isNotEmpty
    assertThat(changeLogReturned?.newVal?.values?.get(0)).isEqualTo(referralDetailsValue?.completionDeadline.toString())
    assertThat(changeLogReturned?.oldVal).isNotNull
    assertThat(changeLogReturned?.oldVal?.values).isNotEmpty
    assertThat(changeLogReturned?.oldVal?.values?.get(0)).isEqualTo(referral.completionDeadline.toString())
    assertThat(changeLogReturned?.reasonForChange).isNotBlank
    assertThat(changeLogReturned?.topic).isEqualTo(AmendTopic.COMPLETION_DATETIME)
  }

  @Test
  fun `updateReferralDetails updates futher information and completion deadline update when referral details doesnt exists`() {
    val authUser = AuthUser("123457", "delius", "bernard.beaks")
    val user = userFactory.create("pp_user_1", "delius")
    val description = ""
    val id = UUID.randomUUID()
    val completionDate = LocalDate.of(2022, 3, 28)
    val completionDateToChange = LocalDate.of(2022, 7, 1)
    val intervention = interventionFactory.create(description = description)

    val referral = referralFactory.createSent(
      createdAt = OffsetDateTime.now(),
      createdBy = user,
      id = id,
      sentAt = OffsetDateTime.now(),
      serviceUserCRN = "crn",
      intervention = intervention,
      completionDeadline = completionDate
    )
    whenever(userMapper.fromToken(jwtAuthentionToken)).thenReturn(authUser)

    val referralToUpdate = UpdateReferralDetailsDTO(20, completionDateToChange, "new information", "we decided 10 days wasn't enough")
    var referralDetailsReturned = referralService.updateReferralDetails(referral, referralToUpdate, user)
    val referralDetailsValue = referralService.getReferralDetailsById(referralDetailsReturned?.id)

    assertThat(referralDetailsValue?.referralId).isEqualTo(referral.id)
    assertThat(referralToUpdate.completionDeadline).isEqualTo(referralDetailsValue?.completionDeadline)
    assertThat(referralToUpdate.reasonForChange).isEqualTo(referralDetailsValue?.reasonForChange)
    assertThat(referralToUpdate.furtherInformation).isEqualTo(referralDetailsValue?.furtherInformation)
  }
}
