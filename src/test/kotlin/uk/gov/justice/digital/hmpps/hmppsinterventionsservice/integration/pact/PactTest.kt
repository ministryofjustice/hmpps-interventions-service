package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.integration.pact

import au.com.dius.pact.provider.junit5.PactVerificationContext
import au.com.dius.pact.provider.junitsupport.Provider
import au.com.dius.pact.provider.junitsupport.State
import au.com.dius.pact.provider.junitsupport.loader.PactBroker
import au.com.dius.pact.provider.spring.junit5.PactVerificationSpringProvider
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestTemplate
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.whenever
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.test.context.bean.override.mockito.MockitoBean
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.authorization.ServiceProviderAccessScope
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.authorization.ServiceProviderAccessScopeMapper
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.AuthUserDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AuthUser
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.CommunityAPIOffenderService
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.Conviction
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.ConvictionDetails
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.HMPPSAuthService
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.Offence
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.RamDeliusAPIConvictionService
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.RamDeliusReferralService
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.RisksAndNeedsService
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.Sentence
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.ServiceUserAccessResult
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.UserDetail
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.DynamicFrameworkContractFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ServiceProviderFactory
import java.time.Clock
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@PactBroker
@Provider("Interventions Service")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@Import(PactTest.FixedClockConfig::class)
class PactTest : IntegrationTestBase() {
  @MockitoBean private lateinit var communityAPIOffenderService: CommunityAPIOffenderService

  @MockitoBean private lateinit var ramDeliusAPIConvictionService: RamDeliusAPIConvictionService

  @MockitoBean private lateinit var ramDeliusReferralService: RamDeliusReferralService

  @MockitoBean private lateinit var risksAndNeedsService: RisksAndNeedsService

  @MockitoBean private lateinit var hmppsAuthService: HMPPSAuthService

  @MockitoBean private lateinit var serviceProviderAccessScopeMapper: ServiceProviderAccessScopeMapper

  private val serviceProviderFactory = ServiceProviderFactory()
  private val dynamicFrameworkContractFactory = DynamicFrameworkContractFactory()

  @TestTemplate
  @ExtendWith(PactVerificationSpringProvider::class)
  fun pactVerificationTestTemplate(context: PactVerificationContext) {
    context.verifyInteraction()
  }

  @BeforeEach
  fun `before each`(context: PactVerificationContext) {
    // required for 'sendDraftReferral' to return a valid DTO
    whenever(risksAndNeedsService.createSupplementaryRisk(any(), any(), any(), anyOrNull(), any(), anyOrNull())).thenReturn(UUID.randomUUID())

    whenever(communityAPIOffenderService.checkIfAuthenticatedDeliusUserHasAccessToServiceUser(any(), any()))
      .thenReturn(ServiceUserAccessResult(true, emptyList()))
    whenever(ramDeliusAPIConvictionService.getConvictionDetails(any(), any())).thenReturn(
      ConvictionDetails(
        Conviction(
          123456L,
          LocalDate.parse("2020-01-01"),
          Sentence("custodial", LocalDate.parse("2020-01-02")),
          Offence("", ""),
          false,
        ),
      ),
    )
    // required for SP users
    whenever(hmppsAuthService.getUserDetail(any<AuthUserDTO>())).thenReturn(UserDetail("tom", "tom@tom.tom", "jones"))
    whenever(hmppsAuthService.getUserDetail(any<AuthUser>())).thenReturn(UserDetail("tom", "tom@tom.tom", "jones"))
    whenever(serviceProviderAccessScopeMapper.fromUser(any())).thenReturn(
      ServiceProviderAccessScope(
        setOf(serviceProviderFactory.create()),
        setOf(
          setupAssistant.createDynamicFrameworkContract(
            contractType = setupAssistant.contractTypes["WOS"]!!,
            primeProviderId = "HARMONY_LIVING",
          ),
        ),
      ),
    )

    context.addStateChangeHandlers(
      ActionPlanContracts(setupAssistant),
      InterventionContracts(setupAssistant, serviceProviderAccessScopeMapper),
      ReferralContracts(setupAssistant),
      ServiceCategoryContracts(setupAssistant),
      EndOfServiceReportContracts(setupAssistant),
      SupplierAssessmentContracts(setupAssistant),
      ReportingContracts(setupAssistant),
      CaseNoteContracts(setupAssistant),
    )
  }

  @State("nothing")
  fun `noop`() {}

  @TestConfiguration
  open class FixedClockConfig {
    @Bean
    @Primary
    fun pactTestClock(): Clock = Clock.fixed(OffsetDateTime.parse("2025-12-31T12:00:00Z").toInstant(), OffsetDateTime.parse("2025-12-31T12:00:00Z").offset)
  }
}
