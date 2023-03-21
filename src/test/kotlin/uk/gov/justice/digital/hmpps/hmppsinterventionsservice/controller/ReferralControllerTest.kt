package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.controller

import com.microsoft.applicationinsights.TelemetryClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus
import org.springframework.security.access.AccessDeniedException
import org.springframework.web.server.ResponseStatusException
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.authorization.ClientApiAccessChecker
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.authorization.UserMapper
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.controller.mappers.CancellationReasonMapper
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.AuthUserDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.EndReferralRequestDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.ReferralAssignmentDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AuthUser
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.CancellationReason
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.AuthUserRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.ActionPlanService
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.DraftOasysRiskInformationService
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.DraftReferralService
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.ReferralConcluder
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.ReferralService
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.ServiceCategoryService
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ActionPlanFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.AppointmentFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.AuthUserFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.JwtTokenFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ReferralFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.SupplierAssessmentFactory
import java.util.UUID

internal class ReferralControllerTest {
  private val draftReferralService = mock<DraftReferralService>()
  private val referralService = mock<ReferralService>()
  private val referralConcluder = mock<ReferralConcluder>()
  private val serviceCategoryService = mock<ServiceCategoryService>()
  private val authUserRepository = mock<AuthUserRepository>()
  private val userMapper = UserMapper(authUserRepository)
  private val clientApiAccessChecker = ClientApiAccessChecker()
  private val cancellationReasonMapper = mock<CancellationReasonMapper>()
  private val actionPlanService = mock<ActionPlanService>()
  private val draftOasysRiskInformationService = mock<DraftOasysRiskInformationService>()
  private val telemetryClient = mock<TelemetryClient>()
  private val referralController = ReferralController(
    referralService,
    referralConcluder,
    serviceCategoryService,
    userMapper,
    clientApiAccessChecker,
    cancellationReasonMapper,
    actionPlanService,
    telemetryClient,
  )
  private val tokenFactory = JwtTokenFactory()
  private val referralFactory = ReferralFactory()
  private val authUserFactory = AuthUserFactory()
  private val supplierAssessmentFactory = SupplierAssessmentFactory()
  private val appointmentsFactory = AppointmentFactory()
  private val actionPlanFactory = ActionPlanFactory()

  @Nested
  inner class GetSentReferralForUser {
    private val referral = referralFactory.createSent()
    private val user = authUserFactory.create()
    private val token = tokenFactory.create(userID = user.id, userName = user.userName, authSource = user.authSource)
    private val clientApiToken = tokenFactory.create("interventions-event-client", "ROLE_INTERVENTIONS_API_READ_ALL")

    @Test
    fun `getSentReferral returns not found if sent referral does not exist`() {
      whenever(referralService.getSentReferralForUser(eq(referral.id), any())).thenReturn(null)
      whenever(authUserRepository.save(any())).thenReturn(user)
      val e = assertThrows<ResponseStatusException> {
        referralController.getSentReferral(
          referral.id,
          token,
        )
      }
      assertThat(e.status).isEqualTo(HttpStatus.NOT_FOUND)
      assertThat(e.message).contains("\"sent referral not found")
    }

    @Test
    fun `getSentReferral returns a sent referral if it exists`() {
      whenever(referralService.getSentReferralForUser(eq(referral.id), any())).thenReturn(referral)
      whenever(authUserRepository.save(any())).thenReturn(user)
      val sentReferral = referralController.getSentReferral(
        referral.id,
        token,
      )
      assertThat(sentReferral.id).isEqualTo(referral.id)
    }
  }

  @Nested
  inner class GetSentReferralForClient {
    private val referral = referralFactory.createSent()

    @Test
    fun `getSentReferral returns a sent referral to a client api request`() {
      whenever(referralService.getSentReferral(eq(referral.id))).thenReturn(referral)
      val sentReferral = referralController.getSentReferral(
        referral.id,
        tokenFactory.create(
          clientId = "interventions-event-client",
          subject = "interventions-event-client",
          authorities = arrayOf("ROLE_INTERVENTIONS_API_READ_ALL")
        ),
      )
      assertThat(sentReferral.id).isEqualTo(referral.id)
    }

    @Test
    fun `getSentReferral does not return a sent referral to a client api request when client id and subject do not match`() {
      whenever(referralService.getSentReferral(eq(referral.id))).thenReturn(referral)
      val e = assertThrows<AccessDeniedException> {
        referralController.getSentReferral(
          referral.id,
          tokenFactory.create(
            clientId = "interventions-event-client",
            subject = "BERNARD.BERKS",
            authorities = arrayOf("ROLE_INTERVENTIONS_API_READ_ALL")
          ),
        )
      }
      assertThat(e.message).isEqualTo("could not map auth token to user: [no 'user_id' claim in token, no 'user_name' claim in token]")
    }

    @Test
    fun `getSentReferral does not return a sent referral to a client api request without the required authority`() {
      whenever(referralService.getSentReferral(eq(referral.id))).thenReturn(referral)
      val e = assertThrows<AccessDeniedException> {
        referralController.getSentReferral(
          referral.id,
          tokenFactory.create(
            clientId = "interventions-event-client",
            subject = "interventions-event-client",
            authorities = arrayOf("ROLE_INTEXXXXTIONS_API_READ_ALL")
          ),
        )
      }
      assertThat(e.message).isEqualTo("could not map auth token to user: [no 'user_id' claim in token, no 'user_name' claim in token]")
    }
  }

  @Nested
  inner class GetSentReferralSupplierAssessmentForClient {
    private val supplierAssessment = supplierAssessmentFactory.create()
    private val referral = supplierAssessment.referral

    @BeforeEach
    fun `associate supplier assessment to referral`() {
      referral.supplierAssessment = supplierAssessment
    }

    @Test
    fun `getSupplierAssessmentAppointment returns a supplier assessment to a client api request`() {
      whenever(referralService.getSentReferral(eq(referral.id))).thenReturn(referral)
      val supplierAssessmentDto = referralController.getSupplierAssessmentAppointment(
        referral.id,
        tokenFactory.create(
          clientId = "interventions-event-client",
          subject = "interventions-event-client",
          authorities = arrayOf("ROLE_INTERVENTIONS_API_READ_ALL")
        ),
      )
      assertThat(supplierAssessmentDto.id).isEqualTo(supplierAssessment.id)
    }

    @Test
    fun `getSupplierAssessmentAppointment returns a supplier assessment with only superseded appointments`() {
      val oldAppointment = appointmentsFactory.create(superseded = true)
      val newAppointment = appointmentsFactory.create()
      val supplierAssessment = supplierAssessmentFactory.createWithMultipleAppointments(appointments = mutableSetOf(oldAppointment, newAppointment))
      val referral = supplierAssessment.referral
      referral.supplierAssessment = supplierAssessment

      whenever(referralService.getSentReferral(eq(referral.id))).thenReturn(referral)
      val supplierAssessmentDto = referralController.getSupplierAssessmentAppointment(
        referral.id,
        tokenFactory.create(
          clientId = "interventions-event-client",
          subject = "interventions-event-client",
          authorities = arrayOf("ROLE_INTERVENTIONS_API_READ_ALL")
        ),
      )
      assertThat(supplierAssessmentDto.id).isEqualTo(supplierAssessment.id)
      assertThat(supplierAssessmentDto.appointments)
        .hasSize(1)
        .extracting("id")
        .contains(newAppointment.id)
    }

    @Test
    fun `getSupplierAssessmentAppointment does not return a supplier assessment to a client api request when client id and subject do not match`() {
      whenever(referralService.getSentReferral(eq(referral.id))).thenReturn(referral)
      val e = assertThrows<AccessDeniedException> {
        referralController.getSupplierAssessmentAppointment(
          referral.id,
          tokenFactory.create(
            clientId = "interventions-event-client",
            subject = "BERNARD.BERKS",
            authorities = arrayOf("ROLE_INTERVENTIONS_API_READ_ALL")
          ),
        )
      }
      assertThat(e.message).isEqualTo("could not map auth token to user: [no 'user_id' claim in token, no 'user_name' claim in token]")
    }

    @Test
    fun `getSupplierAssessmentAppointment does not return a supplier assessment to a client api request without the required authority`() {
      whenever(referralService.getSentReferral(eq(referral.id))).thenReturn(referral)
      val e = assertThrows<AccessDeniedException> {
        referralController.getSupplierAssessmentAppointment(
          referral.id,
          tokenFactory.create(
            clientId = "interventions-event-client",
            subject = "interventions-event-client",
            authorities = arrayOf("ROLE_INTEXXXXTIONS_API_READ_ALL")
          ),
        )
      }
      assertThat(e.message).isEqualTo("could not map auth token to user: [no 'user_id' claim in token, no 'user_name' claim in token]")
    }
  }

  @Nested
  inner class GetServiceProviderSentReferralsSummary {
    private val token = tokenFactory.create()

    @Test
    fun `can accept all defined dashboard types`() {
      whenever(referralService.getServiceProviderSummaries(any(), any())).thenReturn(emptyList())
      whenever(authUserRepository.save(any())).thenReturn(authUserFactory.create())
      assertThat(
        referralController.getServiceProviderSentReferralsSummary(
          token, "myCases"
        )
      ).isNotNull

      assertThat(
        referralController.getServiceProviderSentReferralsSummary(
          token, "openCases"
        )
      ).isNotNull

      assertThat(
        referralController.getServiceProviderSentReferralsSummary(
          token, "unassignedCases"
        )
      ).isNotNull

      assertThat(
        referralController.getServiceProviderSentReferralsSummary(
          token, "completedCases"
        )
      ).isNotNull
    }

    @Test
    fun `returns default list when no dashboardType provided`() {
      whenever(referralService.getServiceProviderSummaries(any(), any())).thenReturn(emptyList())
      whenever(authUserRepository.save(any())).thenReturn(authUserFactory.create())
      val result = referralController.getServiceProviderSentReferralsSummary(
        token, null
      )
      assertThat(result).isNotNull
    }

    @Test
    fun `thros error when invalid dashboardType provided`() {
      whenever(referralService.getServiceProviderSummaries(any(), any())).thenReturn(emptyList())
      whenever(authUserRepository.save(any())).thenReturn(authUserFactory.create())
      assertThrows<IllegalArgumentException> {
        referralController.getServiceProviderSentReferralsSummary(
          token,
          "invalidDashBoardType"
        )
      }
    }
  }

  @Test
  fun `assignSentReferral returns 404 if referral does not exist`() {
    whenever(referralService.getSentReferralForUser(any(), any())).thenReturn(null)
    whenever(authUserRepository.save(any())).thenReturn(authUserFactory.create())
    val e = assertThrows<ResponseStatusException> {
      referralController.assignSentReferral(
        UUID.randomUUID(),
        ReferralAssignmentDTO(AuthUserDTO("username", "authSource", "userId")),
        tokenFactory.create()
      )
    }
    assertThat(e.status).isEqualTo(HttpStatus.NOT_FOUND)
  }

  @Test
  fun `assignSentReferral uses incoming jwt for 'assignedBy' argument, and request body for 'assignedTo'`() {
    val referral = referralFactory.createSent()
    val assignedToUser = authUserFactory.create(id = "to")
    whenever(referralService.getSentReferralForUser(any(), any())).thenReturn(referral)
    whenever(referralService.assignSentReferral(any(), any(), any())).thenReturn(referral)
    whenever(authUserRepository.save(any())).thenReturn(authUserFactory.create(id = "by"))
    referralController.assignSentReferral(
      UUID.randomUUID(),
      ReferralAssignmentDTO(AuthUserDTO.from(assignedToUser)),
      tokenFactory.create(userID = "by")
    )

    val toCaptor = argumentCaptor<AuthUser>()
    val byCaptor = argumentCaptor<AuthUser>()
    verify(referralService).assignSentReferral(eq(referral), byCaptor.capture(), toCaptor.capture())
    assertThat(toCaptor.firstValue.id).isEqualTo("to")
    assertThat(byCaptor.firstValue.id).isEqualTo("by")
  }

  @Test
  fun `successfully call end referral endpoint`() {
    val referral = referralFactory.createSent()
    val endReferralDTO = EndReferralRequestDTO("AAA", "comment")
    val cancellationReason = CancellationReason("AAA", "description")

    whenever(cancellationReasonMapper.mapCancellationReasonIdToCancellationReason(any())).thenReturn(cancellationReason)
    whenever(referralService.getSentReferralForUser(any(), any())).thenReturn(referral)

    val user = AuthUser("CRN123", "auth", "user")
    val token = tokenFactory.create(user.id, user.authSource, user.userName)
    val endedReferral = referralFactory.createEnded(endRequestedComments = "comment")
    whenever(referralService.requestReferralEnd(any(), any(), any(), any())).thenReturn(endedReferral)
    whenever(referralConcluder.requiresEndOfServiceReportCreation(endedReferral)).thenReturn(true)
    whenever(authUserRepository.save(any())).thenReturn(user)

    referralController.endSentReferral(referral.id, endReferralDTO, token)
    verify(referralService).requestReferralEnd(referral, user, cancellationReason, "comment")
  }

  @Test
  fun `end referral endpoint does not find referral`() {
    val endReferralDTO = EndReferralRequestDTO("AAA", "comment")
    val cancellationReason = CancellationReason("AAA", "description")

    whenever(cancellationReasonMapper.mapCancellationReasonIdToCancellationReason(any())).thenReturn(cancellationReason)
    whenever(referralService.getSentReferralForUser(any(), any())).thenReturn(null)
    whenever(authUserRepository.save(any())).thenReturn(authUserFactory.create())

    val token = tokenFactory.create()
    val e = assertThrows<ResponseStatusException> {
      referralController.endSentReferral(UUID.randomUUID(), endReferralDTO, token)
    }
    assertThat(e.status).isEqualTo(HttpStatus.NOT_FOUND)
  }

  @Test
  fun `get all cancellation reasons`() {
    val cancellationReasons = listOf(
      CancellationReason(code = "aaa", description = "reason 1"),
      CancellationReason(code = "bbb", description = "reason 2")
    )
    whenever(referralService.getCancellationReasons()).thenReturn(cancellationReasons)
    val response = referralController.getCancellationReasons()
    assertThat(response).isEqualTo(cancellationReasons)
  }

  @Test
  fun `get supplier assessment appointment`() {
    val referral = referralFactory.createSent()
    referral.supplierAssessment = supplierAssessmentFactory.create()
    val token = tokenFactory.create()

    whenever(referralService.getSentReferralForUser(any(), any())).thenReturn(referral)
    whenever(authUserRepository.save(any())).thenReturn(authUserFactory.create())

    val response = referralController.getSupplierAssessmentAppointment(referral.id, token)

    assertThat(response).isNotNull
  }

  @Test
  fun `no sent referral found for get supplier assessment appointment `() {
    val referralId = UUID.randomUUID()
    val token = tokenFactory.create()

    whenever(referralService.getSentReferralForUser(any(), any())).thenReturn(null)
    whenever(authUserRepository.save(any())).thenReturn(authUserFactory.create())

    val e = assertThrows<ResponseStatusException> {
      referralController.getSupplierAssessmentAppointment(referralId, token)
    }
    assertThat(e.status).isEqualTo(HttpStatus.NOT_FOUND)
    assertThat(e.message).contains("sent referral not found [id=$referralId]")
  }

  @Nested
  inner class SetsEndOfServiceReportRequired {
    private val user = authUserFactory.create()
    private val token = tokenFactory.create(userID = user.id, userName = user.userName, authSource = user.authSource)

    @Test
    fun `is set when getting a set referral`() {
      val referral = referralFactory.createSent()
      whenever(referralService.getSentReferralForUser(eq(referral.id), any())).thenReturn(referral)
      whenever(referralConcluder.requiresEndOfServiceReportCreation(referral)).thenReturn(false)
      whenever(authUserRepository.save(any())).thenReturn(user)

      val sentReferral = referralController.getSentReferral(
        referral.id,
        token,
      )
      assertThat(sentReferral.id).isEqualTo(referral.id)
      assertThat(sentReferral.endOfServiceReportCreationRequired).isFalse
    }

    @Test
    fun `is set after assigning a referral`() {
      val referral = referralFactory.createSent()
      val assignedToUser = authUserFactory.create(id = "to")
      whenever(referralService.getSentReferralForUser(any(), any())).thenReturn(referral)
      whenever(referralService.assignSentReferral(any(), any(), any())).thenReturn(referral)
      whenever(referralConcluder.requiresEndOfServiceReportCreation(referral)).thenReturn(false)
      whenever(authUserRepository.save(any())).thenReturn(user)

      val assignedReferral = referralController.assignSentReferral(
        UUID.randomUUID(),
        ReferralAssignmentDTO(AuthUserDTO.from(assignedToUser)),
        tokenFactory.create(userID = "by")
      )
      assertThat(assignedReferral.id).isEqualTo(referral.id)
      assertThat(assignedReferral.endOfServiceReportCreationRequired).isFalse
    }

    @Test
    fun `is set after ending a referral`() {
      val referral = referralFactory.createSent()
      val endReferralDTO = EndReferralRequestDTO("AAA", "comment")
      val cancellationReason = CancellationReason("AAA", "description")

      whenever(cancellationReasonMapper.mapCancellationReasonIdToCancellationReason(any())).thenReturn(cancellationReason)
      whenever(referralService.getSentReferralForUser(any(), any())).thenReturn(referral)
      whenever(authUserRepository.save(any())).thenReturn(user)

      val user = AuthUser("CRN123", "auth", "user")
      val token = tokenFactory.create(user.id, user.authSource, user.userName)
      val endedReferral = referralFactory.createEnded(endRequestedComments = "comment")
      whenever(referralService.requestReferralEnd(any(), any(), any(), any())).thenReturn(endedReferral)
      whenever(referralConcluder.requiresEndOfServiceReportCreation(referral)).thenReturn(true)

      val response = referralController.endSentReferral(referral.id, endReferralDTO, token)
      assertThat(response.endOfServiceReportCreationRequired).isTrue
    }
  }

  @Test
  fun `get action plans for referral returns only approved action plans`() {
    val referralId = UUID.randomUUID()
    val approvedActionPlan = actionPlanFactory.createApproved()
    val nonApprovedActionPlan = actionPlanFactory.create()

    whenever(actionPlanService.getApprovedActionPlansByReferral(referralId)).thenReturn(listOf(approvedActionPlan, nonApprovedActionPlan))
    val response = referralController.getReferralApprovedActionPlans(referralId)

    assertThat(response.size).isEqualTo(2)
  }
}
