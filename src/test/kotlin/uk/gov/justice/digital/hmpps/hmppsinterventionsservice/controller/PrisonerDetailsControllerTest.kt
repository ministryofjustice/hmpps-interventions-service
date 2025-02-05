package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.controller

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.Prisoner
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.PrisonerDetailsService
import java.time.LocalDate

internal class PrisonerDetailsControllerTest {

  private val prisonerDetailsService: PrisonerDetailsService = mock()
  private val prisonerDetailsController = PrisonerDetailsController(prisonerDetailsService)

  @Test
  fun `extract the prison details for the given crn`() {
    val serviceUserCRN = "X123456"
    val personCustodyPrisonId = "ABC"
    val prisonerNumber = "A6838DA"
    val expectedReleaseDate = LocalDate.of(2050, 11, 1)
    val prisoner = mockPrisoner(personCustodyPrisonId, prisonerNumber, expectedReleaseDate)
    val jwtAuthenticationToken = JwtAuthenticationToken(mock())

    whenever(prisonerDetailsService.details(serviceUserCRN)).thenReturn(prisoner)

    assertThat(prisonerDetailsController.prisonerDetails(serviceUserCRN, jwtAuthenticationToken)).isEqualTo(prisoner)
  }

  @Test
  fun `when prison details is not available for the given crn`() {
    val serviceUserCRN = "X123456"
    val jwtAuthenticationToken = JwtAuthenticationToken(mock())

    whenever(prisonerDetailsService.details(serviceUserCRN)).thenReturn(null)

    assertNull(prisonerDetailsController.prisonerDetails(serviceUserCRN, jwtAuthenticationToken))
  }

  private fun mockPrisoner(prisonId: String, prisonerNumber: String, expectedReleaseDate: LocalDate) = Prisoner(
    prisonId,
    prisonerNumber,
    expectedReleaseDate,
    expectedReleaseDate,
    expectedReleaseDate,
    expectedReleaseDate,
    expectedReleaseDate,
    expectedReleaseDate,
    expectedReleaseDate,
    expectedReleaseDate,
  )
}
