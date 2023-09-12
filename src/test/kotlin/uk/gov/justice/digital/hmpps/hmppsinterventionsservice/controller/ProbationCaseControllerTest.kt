package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.controller

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.component.LocationMapper
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.ProbationCaseReferralDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.ProbationCaseService
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.ReferralService
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.ResponsibleProbationPractitioner
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.UserDetail
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ReferralFactory
import java.net.URI

internal class ProbationCaseControllerTest {
  private val probationCaseService = mock<ProbationCaseService>()
  private val referralService = mock<ReferralService>()
  private val locationMapper = mock<LocationMapper>()
  private val referralFactory = ReferralFactory()

  private val probationCaseController = ProbationCaseController(
    probationCaseService,
    referralService,
    locationMapper,
  )

  @Test
  fun `saves draft action plan`() {
    val jwtAuthenticationToken = JwtAuthenticationToken(mock())
    val referral = referralFactory.createSent()

    val responsibleOfficer = ResponsibleProbationPractitioner(firstName = "responsible", lastName = "practitioner", email = "aa", deliusStaffCode = "aa", authUser = null)
    val referringOfficerDetails = UserDetail(firstName = "referring", lastName = "officer", email = "aaa")
    val serviceProviderUser = UserDetail(firstName = "service", lastName = "provider", email = "aaa")
    val uri = URI.create("http://localhost/1234")

    val result = ProbationCaseReferralDTO.from(referral, responsibleOfficer, serviceProviderUser, referringOfficerDetails)

    whenever(probationCaseService.getProbationCaseDetails("X123456")).thenReturn(listOf(result))
    whenever(locationMapper.expandPathToCurrentRequestUrl("/probation-case/{crn}/referral", "X123456")).thenReturn(uri)

    val response = probationCaseController.getReferralByCrn("X123456", jwtAuthenticationToken)

    assertThat(response.let { it.body }).isEqualTo(listOf(result))
    assertThat(response.let { it.headers["location"] }).isEqualTo(listOf(uri.toString()))
  }
}
