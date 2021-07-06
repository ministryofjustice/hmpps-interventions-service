package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.integration.service

import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.mock.mockito.MockBean
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.authorization.ServiceProviderAccessScope
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.authorization.ServiceProviderAccessScopeMapper
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AuthUser
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

  @Test
  fun `sanity`() {
    val contract = setupAssistant.createDynamicFrameworkContract(contractReference = "PRJ_123", primeProviderId = "HARMONY_LIVING")
    whenever(serviceProviderAccessScopeMapper.fromUser(spUser)).thenReturn(ServiceProviderAccessScope(setOf(contract.primeProvider), setOf(contract)))

    val intervention = setupAssistant.createIntervention(dynamicFrameworkContract = contract)

    (1..10).forEach {
      setupAssistant.createDraftReferral(intervention = intervention)
      setupAssistant.createSentReferral(intervention = intervention)
    }

    val report = reportingService.getReportData(OffsetDateTime.now().minusDays(1), OffsetDateTime.now().plusDays(1), spUser)
    assertThat(report.size).isEqualTo(20)
  }

  @Test
  fun `referrals with multiple action plans only show up once in the report`() {
    val contract = setupAssistant.createDynamicFrameworkContract(contractReference = "PRJ_123", primeProviderId = "HARMONY_LIVING")
    whenever(serviceProviderAccessScopeMapper.fromUser(spUser)).thenReturn(ServiceProviderAccessScope(setOf(contract.primeProvider), setOf(contract)))

    val referral = setupAssistant.createSentReferral(intervention = setupAssistant.createIntervention(dynamicFrameworkContract = contract))

    val now = OffsetDateTime.now()
    val firstDraftActionPlan = setupAssistant.createActionPlan(referral = referral, createdAt = now.minusHours(3))
    val firstSubmittedActionPlan = setupAssistant.createActionPlan(referral = referral, submittedAt = now.minusHours(2))
    val firstApprovedActionPlan = setupAssistant.createActionPlan(referral = referral, submittedAt = now.minusHours(1), approvedAt = now)

    val report = reportingService.getReportData(OffsetDateTime.now().minusDays(1), OffsetDateTime.now().plusDays(1), spUser)
    assertThat(report.size).isEqualTo(1)
    assertThat(report[0].dateFirstActionPlanSubmitted).isEqualTo(firstSubmittedActionPlan.submittedAt)
    assertThat(report[0].dateOfFirstActionPlanApproval).isEqualTo(firstApprovedActionPlan.approvedAt)
  }
}
