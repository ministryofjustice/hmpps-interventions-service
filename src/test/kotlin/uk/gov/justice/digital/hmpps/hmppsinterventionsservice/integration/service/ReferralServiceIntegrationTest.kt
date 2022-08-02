package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.integration.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.authorization.*
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.ReferralDetailsDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.UpdateReferralDetailsDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.ReferralEventPublisher
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AuthUser
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.*
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.*
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.*
import java.rmi.server.UID
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.*

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
  private val changelogRepository: ChangelogRepository = mock()
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
  }
  @Test
  fun `updateReferralDetails updates futher information and completion deadline update when referral details exists`() {
    val authUser = AuthUser("123457", "delius", "bernard.beaks")
    val user = userFactory.create("pp_user_1", "delius")
    val description = ""
    val id = UUID.randomUUID()
    val completionDate=  LocalDate.of(2022, 3, 28)
    val completionDateToChange=  LocalDate.of(2022, 7, 1)
    val intervention = interventionFactory.create(description = description)
    val referral = referralFactory.createSent(
      createdAt = OffsetDateTime.now(),
      createdBy = user,
      id = id,
      sentAt = null,
      serviceUserCRN = "crn",
      intervention = intervention,
    )
    referralDetailsFactory.create(referralId = id, createdAt = OffsetDateTime.now(), createdBy = authUser, id=UUID.randomUUID(), completionDeadline = LocalDate.of(2022,8,2),saved=true)
    whenever(userMapper.fromToken(jwtAuthentionToken)).thenReturn(authUser)

    val referralToUpdate =  UpdateReferralDetailsDTO(20, completionDateToChange, "new information", "we decided 10 days wasn't enough")
    var referralDetailsReturned = referralService.updateReferralDetails(referral,referralToUpdate,authUser)
    val referralDetailsValue = referralService.getReferralDetailsById(referralDetailsReturned?.id)

    assertThat(referralDetailsValue?.referralId).isEqualTo(referral?.id)
    assertThat(referralToUpdate?.furtherInformation).isEqualTo(referralDetailsValue?.furtherInformation)
    assertThat(referralToUpdate?.completionDeadline).isEqualTo(referralDetailsValue?.completionDeadline)
  }

  @Test
  fun `updateReferralDetails updates futher information and completion deadline update when referral details doesnt exists`() {
    val authUser = AuthUser("123457", "delius", "bernard.beaks")
    val user = userFactory.create("pp_user_1", "delius")
    val description = ""
    val id = UUID.randomUUID()
    val completionDate=  LocalDate.of(2022, 3, 28)
    val completionDateToChange=  LocalDate.of(2022, 7, 1)
    val intervention = interventionFactory.create(description = description)
    val referral = referralFactory.createSent(
      createdAt = OffsetDateTime.now(),
      createdBy = user,
      id = id,
      sentAt = OffsetDateTime.now(),
      serviceUserCRN = "crn",
      intervention = intervention,
    )
    whenever(userMapper.fromToken(jwtAuthentionToken)).thenReturn(authUser)

    val referralToUpdate =  UpdateReferralDetailsDTO(20, completionDateToChange, "new information", "we decided 10 days wasn't enough")
    var referralDetailsReturned = referralService.updateReferralDetails(referral,referralToUpdate,authUser)
    val referralDetailsValue = referralService.getReferralDetailsById(referralDetailsReturned?.id)

    assertThat(referralDetailsValue?.referralId).isEqualTo(referral?.id)
    assertThat(referralToUpdate?.furtherInformation).isEqualTo(referralDetailsValue?.furtherInformation)
    assertThat(referralToUpdate?.completionDeadline).isEqualTo(referralDetailsValue?.completionDeadline)
    assertThat(referralToUpdate?.reasonForChange).isEqualTo(referralDetailsValue?.reasonForChange)
  }
}