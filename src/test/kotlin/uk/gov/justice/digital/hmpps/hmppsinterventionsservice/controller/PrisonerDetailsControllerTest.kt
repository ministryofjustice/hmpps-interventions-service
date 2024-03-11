package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.controller

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
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
    val expectedReleaseDate = LocalDate.of(2050, 11, 1)
    val prisoner = mockPrisoner(personCustodyPrisonId, expectedReleaseDate)

    whenever(prisonerDetailsService.details(serviceUserCRN)).thenReturn(prisoner)

    assertThat(prisonerDetailsController.prisonerDetails(serviceUserCRN)).isEqualTo(prisoner)
  }

  @Test
  fun `when prison details is not available for the given crn`() {
    val serviceUserCRN = "X123456"
    val personCustodyPrisonId = "ABC"
    val expectedReleaseDate = LocalDate.of(2050, 11, 1)
    val prisoner = mockPrisoner(personCustodyPrisonId, expectedReleaseDate)

    whenever(prisonerDetailsService.details(serviceUserCRN)).thenReturn(null)

    assertNull(prisonerDetailsController.prisonerDetails(serviceUserCRN))
  }

  private fun mockPrisoner(prisonId: String, expectedReleaseDate: LocalDate) =
    Prisoner(
      prisonId,
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
