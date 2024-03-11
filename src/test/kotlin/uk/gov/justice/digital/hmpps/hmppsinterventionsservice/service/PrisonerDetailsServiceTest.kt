package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import reactor.core.publisher.Mono
import java.time.LocalDate

internal class PrisonerDetailsServiceTest {

  private val communityAPIOffenderService: CommunityAPIOffenderService = mock()
  private val prisonerOffenderSearchService: PrisonerOffenderSearchService = mock()

  private val prisonerDetailsService = PrisonerDetailsService(communityAPIOffenderService, prisonerOffenderSearchService)

  @Test
  fun `extract prisoner details for the given crn`() {
    val serviceUserCRN = "X123456"
    val personCustodyPrisonId = "ABC"
    val expectedReleaseDate = LocalDate.of(2050, 11, 1)
    val nomsNumber = "N123"

    whenever(communityAPIOffenderService.getOffenderIdentifiers(serviceUserCRN))
      .thenReturn(Mono.just(OffenderIdentifiersResponse(PrimaryIdentifiersResponse(nomsNumber))))
    whenever(prisonerOffenderSearchService.getPrisonerById(nomsNumber))
      .thenReturn(Mono.just(mockPrisoner(personCustodyPrisonId, expectedReleaseDate)))

    val prisonerDetails = this.prisonerDetailsService.details(serviceUserCRN)

    assertThat(prisonerDetails?.prisonId).isEqualTo(personCustodyPrisonId)
    assertThat(prisonerDetails?.releaseDate).isEqualTo(expectedReleaseDate)
  }

  @Test
  fun `when prisoner details is not available for the given crn`() {
    val serviceUserCRN = "X123456"
    val nomsNumber = "N123"

    whenever(communityAPIOffenderService.getOffenderIdentifiers(serviceUserCRN))
      .thenReturn(Mono.just(OffenderIdentifiersResponse(PrimaryIdentifiersResponse(nomsNumber))))
    whenever(prisonerOffenderSearchService.getPrisonerById(nomsNumber))
      .thenReturn(Mono.empty())

    val prisonerDetails = this.prisonerDetailsService.details(serviceUserCRN)

    assertNull(prisonerDetails)
  }

  @Test
  fun `when nomis number is not available for the given crn`() {
    val serviceUserCRN = "X123456"

    whenever(communityAPIOffenderService.getOffenderIdentifiers(serviceUserCRN))
      .thenReturn(Mono.empty())
    val prisonerDetails = this.prisonerDetailsService.details(serviceUserCRN)

    assertNull(prisonerDetails)
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
