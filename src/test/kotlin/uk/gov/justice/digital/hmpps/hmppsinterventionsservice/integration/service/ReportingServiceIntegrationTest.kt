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

class ReportingServiceIntegrationTest() : IntegrationTestBase() {
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
    val contract = setupAssistant.createDynamicFrameworkContract(contractReference = "PRJ_123", primeProviderId = "HARMONY_LIVING")
    setupAssistant.createDraftReferral(intervention = setupAssistant.createIntervention(dynamicFrameworkContract = contract))

    whenever(serviceProviderAccessScopeMapper.fromUser(spUser)).thenReturn(ServiceProviderAccessScope(setOf(contract.primeProvider), setOf(contract)))
  }

  @Test
  fun `sanity`() {
    val report = reportingService.getReportData(OffsetDateTime.now().plusDays(1), OffsetDateTime.now().minusDays(1), spUser)
    assertThat(report.size).isEqualTo(1)
  }
}
