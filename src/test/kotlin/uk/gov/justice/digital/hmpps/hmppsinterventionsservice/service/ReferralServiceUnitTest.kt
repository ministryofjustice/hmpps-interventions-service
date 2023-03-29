package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service

import io.netty.handler.timeout.ReadTimeoutException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.AdditionalAnswers
import org.mockito.ArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.firstValue
import org.mockito.kotlin.mock
import org.mockito.kotlin.secondValue
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.authorization.ReferralAccessChecker
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.authorization.ReferralAccessFilter
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.authorization.ServiceProviderAccessScopeMapper
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.authorization.UserTypeChecker
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.ReferralAmendmentDetails
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.UpdateReferralDetailsDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.ReferralEventPublisher
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AuthUser
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.CancellationReason
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Referral
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ReferralDetails
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.AuthUserRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.CancellationReasonRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ChangelogRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.DeliverySessionRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.DraftReferralRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.InterventionRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ReferralDetailsRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ReferralRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.SentReferralSummariesRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ServiceCategoryRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.AuthUserFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.CancellationReasonFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ChangeLogFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ReferralFactory
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

class ReferralServiceUnitTest {
  private val authUserRepository: AuthUserRepository = mock()
  private val referralRepository: ReferralRepository = mock()
  private val draftReferralRepository: DraftReferralRepository = mock()
  private val sentReferralSummariesRepository: SentReferralSummariesRepository = mock()
  private val interventionRepository: InterventionRepository = mock()
  private val referralEventPublisher: ReferralEventPublisher = mock()
  private val referralConcluder: ReferralConcluder = mock()
  private val cancellationReasonRepository: CancellationReasonRepository = mock()
  private val deliverySessionRepository: DeliverySessionRepository = mock()
  private val serviceCategoryRepository: ServiceCategoryRepository = mock()
  private val referralAccessChecker: ReferralAccessChecker = mock()
  private val serviceProviderAccessScopeMapper: ServiceProviderAccessScopeMapper = mock()
  private val referralAccessFilter: ReferralAccessFilter = mock()
  private val userTypeChecker: UserTypeChecker = mock()
  private val communityAPIOffenderService: CommunityAPIOffenderService = mock()
  private val amendReferralService: AmendReferralService = mock()
  private val hmppsAuthService: HMPPSAuthService = mock()
  private val telemetryService: TelemetryService = mock()
  private val referralDetailsRepository: ReferralDetailsRepository = mock()
  private val changeLogRepository: ChangelogRepository = mock()

  private val referralFactory = ReferralFactory()
  private val authUserFactory = AuthUserFactory()
  private val cancellationReasonFactory = CancellationReasonFactory()
  private val changeLogFactory = ChangeLogFactory()

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
    amendReferralService,
    hmppsAuthService,
    telemetryService,
    referralDetailsRepository,
  )

  @Test
  fun `set ended fields on a sent referral`() {
    val referral = referralFactory.createSent()
    val authUser = authUserFactory.create()
    val cancellationReason = cancellationReasonFactory.create()
    val cancellationComments = "comment"

    whenever(authUserRepository.save(authUser)).thenReturn(authUser)
    whenever(referralRepository.save(any())).thenReturn(referralFactory.createEnded(endRequestedComments = cancellationComments))

    val endedReferral = referralService.requestReferralEnd(referral, authUser, cancellationReason, cancellationComments)
    assertThat(endedReferral.endRequestedAt).isNotNull
    assertThat(endedReferral.endRequestedBy).isEqualTo(authUser)
    assertThat(endedReferral.endRequestedReason).isEqualTo(cancellationReason)
    assertThat(endedReferral.endRequestedComments).isEqualTo(cancellationComments)
  }

  @Test
  fun `referral is concluded if eligible on a sent referral`() {
    val referral = referralFactory.createSent()
    val authUser = authUserFactory.create()
    val cancellationReason = cancellationReasonFactory.create()
    val cancellationComments = "comment"

    whenever(authUserRepository.save(any())).thenAnswer(AdditionalAnswers.returnsFirstArg<AuthUser>())
    whenever(referralRepository.save(any())).thenAnswer(AdditionalAnswers.returnsFirstArg<Referral>())

    referralService.requestReferralEnd(referral, authUser, cancellationReason, cancellationComments)

    verify(referralConcluder).concludeIfEligible(referral)
  }

  @Test
  fun `get all cancellation reasons`() {
    val cancellationReasons = listOf(
      CancellationReason(code = "aaa", description = "reason 1"),
      CancellationReason(code = "bbb", description = "reason 2"),
    )
    whenever(cancellationReasonRepository.findAll()).thenReturn(cancellationReasons)
    val result = referralService.getCancellationReasons()
    assertThat(result).isNotNull
  }

  @Test
  fun `getResponsibleProbationPractitioner uses responsible officer`() {
    whenever(communityAPIOffenderService.getResponsibleOfficer(any())).thenReturn(ResponsibleOfficer("tom", "tom@tom.tom", 123, "jones"))
    val pp = referralService.getResponsibleProbationPractitioner(referralFactory.createSent())
    assertThat(pp.firstName).isEqualTo("tom")
    assertThat(pp.email).isEqualTo("tom@tom.tom")
  }

  @Test
  fun `getResponsibleProbationPractitioner uses sender if there is an unexpected error`() {
    val sender = authUserFactory.create("sender")
    whenever(communityAPIOffenderService.getResponsibleOfficer(any())).thenThrow(ReadTimeoutException.INSTANCE)
    whenever(hmppsAuthService.getUserDetail(sender)).thenReturn(UserDetail("andrew", "andrew@tom.tom", "marr"))
    val pp = referralService.getResponsibleProbationPractitioner(referralFactory.createSent(sentBy = sender))
    assertThat(pp.firstName).isEqualTo("andrew")
    assertThat(pp.email).isEqualTo("andrew@tom.tom")
  }

  @Test
  fun `getResponsibleProbationPractitioner uses sender if there is no responsible officer email address`() {
    val sender = authUserFactory.create("sender")
    whenever(communityAPIOffenderService.getResponsibleOfficer(any())).thenReturn(ResponsibleOfficer("tom", null, 123, "jones"))
    whenever(hmppsAuthService.getUserDetail(sender)).thenReturn(UserDetail("andrew", "andrew@tom.tom", "marr"))
    val pp = referralService.getResponsibleProbationPractitioner(referralFactory.createSent(sentBy = sender))
    assertThat(pp.firstName).isEqualTo("andrew")
    assertThat(pp.email).isEqualTo("andrew@tom.tom")
  }

  @Test
  fun `getResponsibleProbationPractitioner uses creator if there is no responsible officer email address`() {
    val creator = authUserFactory.create("creator")
    whenever(communityAPIOffenderService.getResponsibleOfficer(any())).thenReturn(ResponsibleOfficer("tom", null, 123, "jones"))
    whenever(hmppsAuthService.getUserDetail(creator)).thenReturn(UserDetail("dan", "dan@tom.tom", "walker"))
    val pp = referralService.getResponsibleProbationPractitioner(referralFactory.createSent(createdBy = creator))
    assertThat(pp.firstName).isEqualTo("dan")
    assertThat(pp.email).isEqualTo("dan@tom.tom")
  }

  @Test
  fun `gets sent referral`() {
    val sentReferral = referralFactory.createSent()
    whenever(referralRepository.findByIdAndSentAtIsNotNull(sentReferral.id)).thenReturn(sentReferral)

    val response = referralService.getSentReferral(sentReferral.id)

    assertThat(response).isSameAs(sentReferral)
    verify(referralRepository).findByIdAndSentAtIsNotNull(sentReferral.id)
  }

  @Nested inner class UpdateSentReferralDetails {
    @Test
    fun `a new version is not created if the update contains no data`() {
      val referral = referralFactory.createSent()
      val update = UpdateReferralDetailsDTO(null, null, null, null, null, reasonForChange = "blah blah")
      val returnedValue = referralService.updateReferralDetails(referral, update, referral.createdBy)

      verify(referralDetailsRepository, times(0)).save(any())
      assertThat(returnedValue).isNull()
    }

    @Test
    fun `a new version is created with the current timestamp and updated fields`() {
      val referral = referralFactory.createSent()
      val existingDetails = ReferralDetails(
        UUID.randomUUID(),
        null,
        referral.id,
        referral.createdAt,
        referral.createdBy.id,
        "initial version",
        LocalDate.of(2022, 3, 28),
        "old information",
        10,
      )

      whenever(referralDetailsRepository.findLatestByReferralId(referral.id)).thenReturn(existingDetails)
      whenever(authUserRepository.save(referral.createdBy)).thenReturn(referral.createdBy)

      val returnedValue = referralService.updateReferralDetails(
        referral,
        UpdateReferralDetailsDTO(20, null, "new information", null, null, "we decided 10 days wasn't enough"),
        referral.createdBy,
      )

      val captor = ArgumentCaptor.forClass(ReferralDetails::class.java)
      verify(referralDetailsRepository, times(2)).save(captor.capture())

      val oldReferralValue = ReferralAmendmentDetails(listOf(existingDetails.completionDeadline.toString()))
      val newValue = ReferralAmendmentDetails(listOf(returnedValue?.completionDeadline.toString()))
      val changelogId = UUID.randomUUID()
      val changlogReturned = changeLogFactory.create(
        changelogId,
        AmendTopic.COMPLETION_DATETIME,
        referral.id,
        oldReferralValue,
        newValue,
        "completion deadline change",
        OffsetDateTime.of(2022, 8, 3, 0, 0, 0, 0, ZoneOffset.UTC),
      )

      whenever(changeLogRepository.save(changlogReturned)).thenReturn(changlogReturned)

      assertThat(changlogReturned).isNotNull
      assertThat(changlogReturned.id).isEqualTo(changelogId)
      assertThat(changlogReturned.referralId).isEqualTo(referral.id)
      assertThat(captor.firstValue).isEqualTo(returnedValue)
      assertThat(returnedValue!!.createdAt).isNotEqualTo(existingDetails.createdAt)
      assertThat(returnedValue.createdAt).isNotEqualTo(referral.createdAt)
      assertThat(returnedValue.reasonForChange).isEqualTo("we decided 10 days wasn't enough")
      assertThat(returnedValue.completionDeadline).isEqualTo(LocalDate.of(2022, 3, 28))
      assertThat(returnedValue.furtherInformation).isEqualTo("new information")
      assertThat(returnedValue.maximumEnforceableDays).isEqualTo(20)

      assertThat(captor.secondValue).isEqualTo(existingDetails)
      assertThat(existingDetails.supersededById).isEqualTo(returnedValue.id)
    }
  }
}
