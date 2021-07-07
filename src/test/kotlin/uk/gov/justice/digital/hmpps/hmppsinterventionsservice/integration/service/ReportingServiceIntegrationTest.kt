package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.integration.service

import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.mock.mockito.MockBean
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.authorization.ServiceProviderAccessScope
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.authorization.ServiceProviderAccessScopeMapper
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Attended
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AuthUser
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Intervention
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.ReportingService
import java.time.OffsetDateTime

class ReportingServiceIntegrationTest : IntegrationTestBase() {
  @MockBean lateinit var serviceProviderAccessScopeMapper: ServiceProviderAccessScopeMapper

  private lateinit var reportingService: ReportingService

  private lateinit var ppUser: AuthUser
  private lateinit var spUser: AuthUser

  @BeforeEach
  fun beforeEach() {
    reportingService = ReportingService(
      referralRepository,
      serviceProviderAccessScopeMapper,
      "http://interventions-ui.com",
      "/referral/{id}"
    )

    ppUser = setupAssistant.createPPUser()
    spUser = setupAssistant.createSPUser()
  }

  private fun setupInterventionForUser(user: AuthUser): Intervention {
    val contract = setupAssistant.createDynamicFrameworkContract(contractReference = "PRJ_123", primeProviderId = "HARMONY_LIVING")
    whenever(serviceProviderAccessScopeMapper.fromUser(user)).thenReturn(ServiceProviderAccessScope(setOf(contract.primeProvider), setOf(contract)))

    return setupAssistant.createIntervention(dynamicFrameworkContract = contract)
  }

  @Test
  fun `sanity`() {
    val intervention = setupInterventionForUser(spUser)

    (1..10).forEach {
      setupAssistant.createDraftReferral(intervention = intervention)
      setupAssistant.createSentReferral(intervention = intervention)
    }

    val report = reportingService.getReportData(OffsetDateTime.now().minusDays(1), OffsetDateTime.now().plusDays(1), spUser)
    assertThat(report.size).isEqualTo(10)
  }

  @Test
  fun `referrals with multiple action plans only show up once in the report`() {
    val intervention = setupInterventionForUser(spUser)
    val referral = setupAssistant.createSentReferral(intervention = intervention)

    val now = OffsetDateTime.now()
    val firstDraftActionPlan = setupAssistant.createActionPlan(referral = referral, createdAt = now.minusHours(3))
    val firstSubmittedActionPlan = setupAssistant.createActionPlan(referral = referral, submittedAt = now.minusHours(2))
    val firstApprovedActionPlan = setupAssistant.createActionPlan(referral = referral, submittedAt = now.minusHours(1), approvedAt = now)

    val report = reportingService.getReportData(OffsetDateTime.now().minusDays(1), OffsetDateTime.now().plusDays(1), spUser)
    assertThat(report.size).isEqualTo(1)
    assertThat(report[0].dateFirstActionPlanSubmitted).isEqualTo(firstSubmittedActionPlan.submittedAt)
    assertThat(report[0].dateOfFirstActionPlanApproval).isEqualTo(firstApprovedActionPlan.approvedAt)
  }

  @Test
  fun `first attended session is taken from most recent action plan`() {
    val intervention = setupInterventionForUser(spUser)
    val referral = setupAssistant.createSentReferral(intervention = intervention)

    val now = OffsetDateTime.now()
    val oldActionPlan = setupAssistant.createActionPlan(referral = referral, createdAt = now.minusHours(3))
    setupAssistant.createActionPlanSession(oldActionPlan, 1, 60, now.minusHours(2), Attended.YES)

    val newerActionPlan = setupAssistant.createActionPlan(referral = referral, createdAt =  now.minusHours(1))
    var report = reportingService.getReportData(OffsetDateTime.now().minusDays(1), OffsetDateTime.now().plusDays(1), spUser)
    assertThat(report.size).isEqualTo(1)
    assertThat(report[0].dateOfFirstAttendedSession).isNull()

    setupAssistant.createActionPlanSession(newerActionPlan, 1, 60, now, Attended.LATE)
    report = reportingService.getReportData(OffsetDateTime.now().minusDays(1), OffsetDateTime.now().plusDays(1), spUser)
    assertThat(report.size).isEqualTo(1)
    assertThat(report[0].dateOfFirstAttendedSession).isEqualTo(now)
  }
}
