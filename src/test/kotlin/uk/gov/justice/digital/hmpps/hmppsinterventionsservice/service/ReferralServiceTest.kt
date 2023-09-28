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
import org.junit.jupiter.params.provider.Arguments
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.reactive.function.client.WebClientResponseException
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.authorization.ReferralAccessChecker
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.authorization.ReferralAccessFilter
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.authorization.ServiceProviderAccessScope
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.authorization.ServiceProviderAccessScopeMapper
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.authorization.UserMapper
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.authorization.UserTypeChecker
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.UpdateReferralDetailsDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.ReferralEventPublisher
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AuthUser
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.DraftReferral
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Intervention
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Referral
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ReferralAssignment
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.SentReferralSummary
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ServiceProvider
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ActionPlanRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.AuthUserRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.CancellationReasonRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ChangelogRepository
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
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.DynamicFrameworkContractFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.EndOfServiceReportFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.InterventionFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ReferralDetailsFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ReferralFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ReferralServiceUserFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.RepositoryTest
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.SentReferralSummariesFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ServiceProviderFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ServiceUserFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.SupplierAssessmentFactory
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

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
  val referralDetailsRepository: ReferralDetailsRepository,
  val changelogRepository: ChangelogRepository,
) {
  private val userFactory = AuthUserFactory(entityManager)
  private val interventionFactory = InterventionFactory(entityManager)
  private val contractFactory = DynamicFrameworkContractFactory(entityManager)
  private val serviceProviderFactory = ServiceProviderFactory(entityManager)
  private val referralFactory = ReferralFactory(entityManager)
  private val sentReferralSummariesFactory = SentReferralSummariesFactory(entityManager)
  private val cancellationReasonFactory = CancellationReasonFactory(entityManager)
  private val endOfServiceReportFactory = EndOfServiceReportFactory(entityManager)
  private val actionPlanFactory = ActionPlanFactory(entityManager)
  private val appointmentFactory = AppointmentFactory(entityManager)
  private val supplierAssessmentFactory = SupplierAssessmentFactory(entityManager)
  private val serviceUserDataFactory = ServiceUserFactory(entityManager)
  private val referralServiceUserDataFactory = ReferralServiceUserFactory(entityManager)
  private val referralDetailsFactory = ReferralDetailsFactory(entityManager)

  private val referralEventPublisher: ReferralEventPublisher = mock()
  private val referralConcluder: ReferralConcluder = mock()
  private val referralAccessChecker: ReferralAccessChecker = mock()
  private val userTypeChecker = UserTypeChecker()
  private val serviceProviderAccessScopeMapper: ServiceProviderAccessScopeMapper = mock()
  private val referralAccessFilter = ReferralAccessFilter(serviceProviderAccessScopeMapper)
  private val communityAPIOffenderService: CommunityAPIOffenderService = mock()
  private val ramDeliusAPIOffenderService: RamDeliusAPIOffenderService = mock()
  private val amendReferralService: AmendReferralService = mock()
  private val hmppsAuthService: HMPPSAuthService = mock()
  private val telemetryService: TelemetryService = mock()

  private val recursiveComparisonConfigurationBuilder = RecursiveComparisonConfiguration.builder()
  private val userMapper: UserMapper = mock()
  private val jwtAuthenticationToken: JwtAuthenticationToken = mock()
  private val withDrawnReferrals = true

  private val referralService = ReferralService(
    referralRepository,
    sentReferralSummariesRepository,
    authUserRepository,
    interventionRepository,
    referralConcluder,
    referralEventPublisher,
    cancellationReasonRepository,
    deliverySessionRepository,
    serviceCategoryRepository,
    referralAccessChecker,
    userTypeChecker,
    serviceProviderAccessScopeMapper,
    referralAccessFilter,
    communityAPIOffenderService,
    ramDeliusAPIOffenderService,
    amendReferralService,
    hmppsAuthService,
    telemetryService,
    referralDetailsRepository,
  )

  companion object {
    @JvmStatic
    fun names1234(): List<Arguments> {
      return listOf(
        Arguments.of("bob-smith", "smith", "bob-smith smith"),
        Arguments.of("john", "blue-red", "john blue-red"),
      )
    }
  }

  @AfterEach
  fun `clear referrals`() {
    entityManager.flush()
    interventionRepository.deleteAll()
    referralDetailsRepository.deleteAll()
    authUserRepository.deleteAll()
    changelogRepository.deleteAll()
    draftReferralRepository.deleteAll()
    referralRepository.deleteAll()
  }

  @Nested
  @DisplayName("get sent referrals with a probation practitioner user")
  inner class GetSentReferralsPPUser {
    private val truncateSeconds: Comparator<OffsetDateTime> = Comparator { a, exp ->
      if (exp != null && a != null) {
        if (a
          .truncatedTo(ChronoUnit.SECONDS)
          .isEqual(exp.truncatedTo(ChronoUnit.SECONDS))
        ) {
          0
        } else {
          1
        }
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
    fun `returns referrals started by the user in the sorted order`() {
      val user = userFactory.create("pp_user_1", "delius")
      val startDraftReferral1 = referralFactory.createDraft(createdBy = user)
      val startedReferral1 = referralFactory.createSent(id = startDraftReferral1.id, createdBy = user, createDraft = false)
      val serviceUserData1 = referralServiceUserDataFactory.create("Zack", "Synder", startedReferral1)
      startedReferral1.serviceUserData = serviceUserData1
      entityManager.refresh(startedReferral1)
      val startDraftReferral2 = referralFactory.createDraft(createdBy = user)
      val startedReferral2 = referralFactory.createSent(id = startDraftReferral2.id, createdBy = user, createDraft = false)
      val serviceUserData2 = referralServiceUserDataFactory.create("Dom", "Barnett", startedReferral2)
      startedReferral2.serviceUserData = serviceUserData2
      entityManager.refresh(startedReferral2)
      val startDraftReferral3 = referralFactory.createDraft(createdBy = user)
      val startedReferral3 = referralFactory.createSent(id = startDraftReferral3.id, createdBy = user)
      val serviceUserData3 = referralServiceUserDataFactory.create("Alice", "Wonderland", startedReferral3)
      startedReferral3.serviceUserData = serviceUserData3
      entityManager.refresh(startedReferral3)

      val result = referralService.getSentReferralSummaryForUser(user, null, null, null, null, PageRequest.of(0, 5, Sort.by("serviceUserData.lastName").descending()))
      assertThat(result.elementAt(0).id).isEqualTo(startedReferral3.id)
      assertThat(result.elementAt(1).id).isEqualTo(startedReferral1.id)
      assertThat(result.elementAt(2).id).isEqualTo(startedReferral2.id)
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
        actionPlans = mutableListOf(actionPlanFactory.create(submittedAt = null)),
      )
      val appointment =
        appointmentFactory.create(referral = cancelledReferral, attendanceSubmittedAt = null)
      val superSededAppointment =
        appointmentFactory.create(referral = cancelledReferral, attendanceSubmittedAt = null, superseded = true, supersededById = appointment.id)
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

    @Test
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
        Pageable.ofSize(pageSize),
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
      val pageable = Pageable.ofSize(pageSize)
      val page = referralService.getSentReferralSummaryForUser(
        user,
        null,
        null,
        null,
        null,
        pageable,
      ) as Page<SentReferralSummary>
      assertThat(page.content.size).isEqualTo(pageSize)
      assertThat(page.totalElements).isEqualTo(8)

      val secondPage = referralService.getSentReferralSummaryForUser(
        user,
        null,
        null,
        null,
        null,
        pageable.next(),
      ) as Page<SentReferralSummary>

      assertThat(secondPage.content.size).isEqualTo(3)
      assertThat(secondPage.totalElements).isEqualTo(8)
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
      val userProviders = listOf("test_org_1", "test_org_2", "test_org_3", "test_org_4", "test_org_5").map { id -> serviceProviderFactory.create(id = id, name = id) }
      val contractWithUserProviderAsPrime = contractFactory.create(primeProvider = userProviders[0])
      val contractWithUserProviderAsSub1 = contractFactory.create(subcontractorProviders = mutableSetOf(userProviders[1]))
      val contractWithUserProviderAsSub2 = contractFactory.create(subcontractorProviders = mutableSetOf(userProviders[2]))
      val contractWithUserProviderAsBothPrimeAndSub = contractFactory.create(primeProvider = userProviders[3], subcontractorProviders = mutableSetOf(userProviders[4]))
      val contractWithNoUserProviders = contractFactory.create()

      val primeRef = referralFactory.createSent(intervention = interventionFactory.create(contract = contractWithUserProviderAsPrime))
      val primeAndSubRef = referralFactory.createSent(intervention = interventionFactory.create(contract = contractWithUserProviderAsBothPrimeAndSub))
      val refWithAllProvidersBeingSubs = referralFactory.createSent(intervention = interventionFactory.create(contract = contractWithUserProviderAsSub1))
      val subRef = referralFactory.createSent(intervention = interventionFactory.create(contract = contractWithUserProviderAsSub2))
      val noAccess = referralFactory.createSent(intervention = interventionFactory.create(contract = contractWithNoUserProviders))

      val user = userFactory.create("test_user", "auth")
      whenever(serviceProviderAccessScopeMapper.fromUser(user))
        .thenReturn(ServiceProviderAccessScope(userProviders.toSet(), setOf(contractWithUserProviderAsBothPrimeAndSub, contractWithUserProviderAsPrime, contractWithUserProviderAsSub1, contractWithUserProviderAsSub2)))

      val result = referralService.getServiceProviderSummaries(user)
      assertThat(result.size).isEqualTo(4)
      val referralIds = result.map { summary -> UUID.fromString(summary.referralId) }
      assertThat(referralIds).doesNotContain(noAccess.id)
      assertThat(referralIds).containsAll(
        listOf(
          primeRef.id,
          primeAndSubRef.id,
          refWithAllProvidersBeingSubs.id,
          subRef.id,
        ),
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
        concludedAt = null,
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
        intervention = intervention,
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
      // referral.serviceUserData = serviceUserData

      val serviceUserData2 = serviceUserDataFactory.create(firstName = "john", lastName = "smith")
      referral2 = referralFactory.createSent(id = serviceUserData2.draftReferral!!.id, intervention = intervention, createDraft = false)
      // referral2.serviceUserData = serviceUserData2

      val serviceUserData3 = serviceUserDataFactory.create(firstName = "tim", lastName = "brown")
      referral3 = referralFactory.createSent(id = serviceUserData3.draftReferral!!.id, intervention = intervention, createDraft = false)
      // referral3.serviceUserData = serviceUserData3

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
      val referralServiceUserData2 = referralServiceUserDataFactory.create(firstName = "bob-blue", lastName = "green")
      val referralWithHyphenatedName = referralFactory.createSent(id = serviceUserData2.draftReferral!!.id, intervention = intervention, createDraft = false)
      referralWithHyphenatedName.serviceUserData = referralServiceUserData2

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
  @DisplayName("get sent referrals with reference number search")
  inner class GetSentReferralsWithReferenceNumberSearchTest {
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
      referral = referralFactory.createSent(id = serviceUserData.draftReferral!!.id, referenceNumber = "AJ1827DR", intervention = intervention, createDraft = false)
      // referral.serviceUserData = serviceUserData

      val serviceUserData2 = serviceUserDataFactory.create(firstName = "john", lastName = "smith")
      referral2 = referralFactory.createSent(id = serviceUserData2.draftReferral!!.id, referenceNumber = "EJ3892AC", intervention = intervention, createDraft = false)
      // referral2.serviceUserData = serviceUserData2

      whenever(serviceProviderAccessScopeMapper.fromUser(user))
        .thenReturn(ServiceProviderAccessScope(setOf(provider), setOf(intervention.dynamicFrameworkContract)))
    }

    @Test
    fun `only referrals that have a referenceNumber matching the search text should be returned`() {
      val result = referralService.getSentReferralSummaryForUser(user, null, null, null, null, pageRequest, "AJ1827DR")

      assertThat(result).hasSize(1)
        .extracting("id")
        .contains(referral.id)
    }

    @Test
    fun `no referrals should be returned if no reference Number matches the search term`() {
      val result = referralService.getSentReferralSummaryForUser(user, null, null, null, null, pageRequest, "GH3927AC")
      assertThat(result).size().isEqualTo(0)
    }

    @Test
    fun `no referrals should be returned if no reference Number matches the pattern for referenceNumber`() {
      val result = referralService.getSentReferralSummaryForUser(user, null, null, null, null, pageRequest, "AABBCCDD")
      assertThat(result).size().isEqualTo(0)
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
        ) {
          0
        } else {
          1
        }
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
        intervention = intervention,
      )

      liveSentReferralSummary = sentReferralSummariesFactory.getReferralSummary(liveReferral)

      cancelledReferral = referralFactory.createEnded(
        intervention = intervention,
        endRequestedReason = cancellationReasonFactory.create("ANY"),
        endRequestedAt = OffsetDateTime.now(),
        concludedAt = OffsetDateTime.now(),
        endOfServiceReport = null,
        actionPlans = mutableListOf(actionPlanFactory.createSubmitted()),
      )

      cancelledSentReferralSummary = sentReferralSummariesFactory.getReferralSummary(cancelledReferral)

      draftReferral = referralFactory.createDraft(intervention = intervention)

      selfAssignedReferral = referralFactory.createAssigned(
        intervention = intervention,
        assignments = listOf(
          ReferralAssignment(OffsetDateTime.now(), user, user),
        ),
      )

      selfAssignedSentReferralSummary = sentReferralSummariesFactory.getReferralSummary(selfAssignedReferral)

      otherAssignedReferral = referralFactory.createAssigned(
        intervention = intervention,
        assignments = listOf(
          ReferralAssignment(OffsetDateTime.now(), otherUser, otherUser),
        ),
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
          otherAssignedSentReferralSummary,
        )
    }

    // TODO- come back once we solve the production issue
    @Test
    fun `to check for cancelled referral were pop did not attend appointment should not return for SP User`() {
      val intervention = interventionFactory.create(contract = contractFactory.create(primeProvider = provider))
      val cancelledReferral = referralFactory.createEnded(
        intervention = intervention,
        endRequestedReason = cancellationReasonFactory.create("ANY"),
        endRequestedAt = OffsetDateTime.now(),
        concludedAt = OffsetDateTime.now(),
        endOfServiceReport = null,
        createdBy = ppUser,
        actionPlans = mutableListOf(actionPlanFactory.create(submittedAt = null)),
      )
      val appointment =
        appointmentFactory.create(referral = cancelledReferral, attendanceSubmittedAt = null)
      val supplierAssessmentAppointment =
        supplierAssessmentFactory.create(appointment = appointment, referral = cancelledReferral)
      cancelledReferral.supplierAssessment = supplierAssessmentAppointment
      val id = entityManager.persistAndGetId(cancelledReferral)
      val ppDashboard = referralService.getSentReferralSummaryForUser(ppUser, null, null, null, null, pageRequest)
      assertThat(ppDashboard.count()).isEqualTo(1)
      val result = referralService.getSentReferralSummaryForUser(user, null, null, null, null, pageRequest)
      assertThat(result)
        .usingRecursiveFieldByFieldElementComparator(recursiveComparisonConfiguration)
        .containsExactlyInAnyOrder(
          completedSentReferralSummary,
          liveSentReferralSummary,
          cancelledSentReferralSummary,
          selfAssignedSentReferralSummary,
          otherAssignedSentReferralSummary,
        )
    }

    @Test
    fun `setting concluded returns only concluded referrals`() {
      val appointment =
        appointmentFactory.create(referral = completedReferral, attendanceSubmittedAt = OffsetDateTime.now())
      val superSededAppointment =
        appointmentFactory.create(referral = completedReferral, attendanceSubmittedAt = OffsetDateTime.now(), superseded = true, supersededById = appointment.id)
      val supplierAssessmentAppointment =
        supplierAssessmentFactory.createWithMultipleAppointments(appointments = mutableSetOf(appointment, superSededAppointment), referral = completedReferral)
      completedReferral.supplierAssessment = supplierAssessmentAppointment
      entityManager.refresh(completedReferral)
      completedSentReferralSummary = sentReferralSummariesFactory.getReferralSummary(completedReferral)
      val result = referralService.getSentReferralSummaryForUser(user, true, null, null, null, pageRequest)
      assertThat(result)
        .usingRecursiveFieldByFieldElementComparator(recursiveComparisonConfiguration)
        .containsExactlyInAnyOrder(completedSentReferralSummary)
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
        otherAssignedSentReferralSummary,
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
          otherAssignedSentReferralSummary,
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
        selfAssignedSentReferralSummary,
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
        selfAssignedSentReferralSummary,
      )
    }

    @Test
    fun `setting all filter options to true will not return any results`() {
      val appointment =
        appointmentFactory.create(referral = completedReferral, attendanceSubmittedAt = OffsetDateTime.now())
      val superSededAppointment =
        appointmentFactory.create(referral = completedReferral, attendanceSubmittedAt = OffsetDateTime.now(), superseded = true, supersededById = appointment.id)
      val supplierAssessmentAppointment =
        supplierAssessmentFactory.createWithMultipleAppointments(appointments = mutableSetOf(appointment, superSededAppointment), referral = completedReferral)
      completedReferral.supplierAssessment = supplierAssessmentAppointment
      entityManager.refresh(completedReferral)
      completedSentReferralSummary = sentReferralSummariesFactory.getReferralSummary(completedReferral)
      val result = referralService.getSentReferralSummaryForUser(user, true, true, true, null, pageRequest)
      assertThat(result).isEmpty()
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
        otherAssignedSentReferralSummary,
      )
    }
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
    val responsibleProbationPractitioner = ResponsibleProbationPractitioner("abc", "abc@abc.com", "N01UTAA", AuthUser("123456", "delius", "TEST_INTERVENTIONS_SP_1"), "def")
    val authUser = AuthUser("123457", "delius", "TEST_INTERVENTIONS_SP_1")

    val isUserTheResponsibleOfficer = referralService.isUserTheResponsibleOfficer(responsibleProbationPractitioner, authUser)

    assertTrue(isUserTheResponsibleOfficer)
  }

  @Test
  fun `check when user is not the same as the responsible officer and does not have a valid deliusStaffId `() {
    val responsibleProbationPractitioner = ResponsibleProbationPractitioner("abc", "abc@abc.com", "N01UTAA", AuthUser("123456", "delius", "TEST_INTERVENTIONS_SP_1"), "def")
    val authUser = AuthUser("123457", "delius", "bernard.beaks")

    val isUserTheResponsibleOfficer = referralService.isUserTheResponsibleOfficer(responsibleProbationPractitioner, authUser)

    assertFalse(isUserTheResponsibleOfficer)
  }

  @Test
  fun `updateReferralDetails updates completion deadline`() {
    val authUser = AuthUser("123457", "delius", "bernard.beaks")
    val user = userFactory.create("pp_user_1", "delius")
    val description = ""
    val id = UUID.randomUUID()
    val existingCompletionDate = LocalDate.of(2022, 10, 21)
    val completionDateToChange = LocalDate.of(2022, 10, 30)
    val expectedreleaseDate = LocalDate.of(2023, 12, 31)
    val intervention = interventionFactory.create(description = description)
    val referral = referralFactory.createSent(
      createdAt = OffsetDateTime.now(),
      createdBy = user,
      id = id,
      sentAt = OffsetDateTime.now(),
      serviceUserCRN = "crn",
      intervention = intervention,
    )
    val referralDetails = referralDetailsFactory.create(
      referralId = id,
      createdAt = OffsetDateTime.now(),
      createdBy = authUser,
      id = UUID.randomUUID(),
      completionDeadline = existingCompletionDate,
      saved = true,
    )
    whenever(userMapper.fromToken(jwtAuthenticationToken)).thenReturn(authUser)

    val referralToUpdate = UpdateReferralDetailsDTO(20, completionDateToChange, "new information", expectedreleaseDate, null, "we decided 10 days wasn't enough")
    val referralDetailsReturned = referralService.updateReferralDetails(referral, referralToUpdate, user)
    val referralDetailsValue = referralService.getReferralDetailsById(referralDetailsReturned?.id)

    assertThat(referralDetailsValue?.referralId).isEqualTo(referral.id)
    assertThat(referralToUpdate.furtherInformation).isEqualTo(referralDetailsValue?.furtherInformation)
    assertThat(referralToUpdate.completionDeadline).isEqualTo(referralDetailsValue?.completionDeadline)
  }

  @Test
  fun `updateReferralDetails updates maximum enforceable days`() {
    val authUser = AuthUser("123457", "delius", "bernard.beaks")
    val user = userFactory.create("pp_user_1", "delius")
    val description = ""
    val id = UUID.randomUUID()
    val intervention = interventionFactory.create(description = description)
    val referral = referralFactory.createSent(
      createdAt = OffsetDateTime.now(),
      createdBy = user,
      id = id,
      sentAt = OffsetDateTime.now(),
      serviceUserCRN = "crn",
      intervention = intervention,
    )
    referralDetailsFactory.create(
      referralId = id,
      createdAt = OffsetDateTime.now(),
      createdBy = authUser,
      id = UUID.randomUUID(),
      maximumNumberOfEnforceableDays = 15,
      saved = true,
    )

    whenever(userMapper.fromToken(jwtAuthenticationToken)).thenReturn(authUser)

    val referralToUpdate = UpdateReferralDetailsDTO(20, null, "new information", null, null, "we decided 10 days wasn't enough")
    val referralDetailsReturned = referralService.updateReferralDetails(referral, referralToUpdate, user)
    val referralDetailsValue = referralService.getReferralDetailsById(referralDetailsReturned?.id)

    assertThat(referralDetailsValue?.referralId).isEqualTo(referral.id)
    assertThat(referralToUpdate.furtherInformation).isEqualTo(referralDetailsValue?.furtherInformation)
    assertThat(referralToUpdate.maximumEnforceableDays).isEqualTo(referralDetailsValue?.maximumEnforceableDays)
  }
}
