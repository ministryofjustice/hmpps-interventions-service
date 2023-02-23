package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.listeners

import com.microsoft.applicationinsights.TelemetryClient
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.CustodyLocationLookupEvent
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.PersonCurrentLocationType
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ReferralLocation
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ReferralLocationRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.CommunityAPIOffenderService
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.OffenderIdentifiersResponse
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.PrimaryIdentifiersResponse
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.Prisoner
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.PrisonerOffenderSearchService
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.RepositoryTest
import java.time.LocalDate
import java.util.UUID

@RepositoryTest
internal class CustodyLocationLookupListenerTest {
  private val communityAPIOffenderService: CommunityAPIOffenderService = mock()
  private val prisonerOffenderSearchService: PrisonerOffenderSearchService = mock()
  private val telemetryClient = mock<TelemetryClient>()
  private val referralLocationRepository: ReferralLocationRepository = mock()
  private val custodyLocationLookupListener = CustodyLocationLookupListener(
    communityAPIOffenderService,
    prisonerOffenderSearchService,
    referralLocationRepository,
    telemetryClient
  )

  @Test
  fun `sends tracking event with matching comparison results`() {
    val referralId = UUID.randomUUID()
    val serviceUserCRN = "X123456"
    val personCustodyPrisonId = "ABC"
    val expectedReleaseDate = LocalDate.of(2050, 11, 1)
    val nomsNumber = "N123"

    whenever(communityAPIOffenderService.getOffenderIdentifiers(serviceUserCRN))
      .thenReturn(Mono.just(OffenderIdentifiersResponse(PrimaryIdentifiersResponse(nomsNumber))))
    whenever(prisonerOffenderSearchService.getPrisonerById(nomsNumber))
      .thenReturn(Mono.just(mockPrisoner(personCustodyPrisonId, expectedReleaseDate)))
    whenever(referralLocationRepository.findByReferralId(referralId))
      .thenReturn(mockReferralLocation(personCustodyPrisonId, expectedReleaseDate))

    custodyLocationLookupListener.onApplicationEvent(custodyLocationLookupEvent(referralId, serviceUserCRN))

    verify(telemetryClient).trackEvent(
      "CustodyLocationLookup",
      mapOf(
        "result" to "match",
        "crn" to serviceUserCRN
      ),
      null
    )
  }

  @Test
  fun `sends track event with non matching comparison results`() {
    val referralId = UUID.randomUUID()
    val serviceUserCRN = "X123456"
    val personCustodyPrisonId = "ABC"
    val expectedReleaseDate = LocalDate.of(2050, 11, 1)
    val differentPrisonId = "AAA"
    val nomsNumber = "N123"

    whenever(communityAPIOffenderService.getOffenderIdentifiers(serviceUserCRN))
      .thenReturn(Mono.just(OffenderIdentifiersResponse(PrimaryIdentifiersResponse(nomsNumber))))
    whenever(prisonerOffenderSearchService.getPrisonerById(nomsNumber))
      .thenReturn(Mono.just(mockPrisoner(personCustodyPrisonId, expectedReleaseDate)))
    whenever(referralLocationRepository.findByReferralId(referralId))
      .thenReturn(mockReferralLocation(differentPrisonId, expectedReleaseDate))

    custodyLocationLookupListener.onApplicationEvent(custodyLocationLookupEvent(referralId, serviceUserCRN))

    verify(telemetryClient).trackEvent(
      "CustodyLocationLookup",
      mapOf(
        "result" to "no match",
        "crn" to serviceUserCRN
      ),
      null
    )
  }

  @Test
  fun `sends tracking events with error status and reason`() {
    val referralId = UUID.randomUUID()
    val serviceUserCRN = "X123456"
    val nomsNumber = "N123"
    val e = WebClientResponseException(404, "", null, null, null, null)

    whenever(communityAPIOffenderService.getOffenderIdentifiers(serviceUserCRN))
      .thenReturn(Mono.just(OffenderIdentifiersResponse(PrimaryIdentifiersResponse(nomsNumber))))
    whenever(prisonerOffenderSearchService.getPrisonerById(nomsNumber))
      .thenReturn(Mono.error(e))

    custodyLocationLookupListener.onApplicationEvent(custodyLocationLookupEvent(referralId, serviceUserCRN))

    verify(telemetryClient).trackEvent(
      "CustodyLocationLookup",
      mapOf(
        "result" to "prisoner search failed",
        "exception" to e.toString(),
        "crn" to serviceUserCRN
      ),
      null
    )
    verify(telemetryClient, never()).trackEvent(
      "CustodyLocationLookup",
      mapOf(
        "result" to "prisoner not found",
        "noms" to nomsNumber,
        "crn" to serviceUserCRN
      ),
      null
    )
  }

  @Test
  fun `sends tracking events with not found result`() {
    val referralId = UUID.randomUUID()
    val serviceUserCRN = "X123456"

    whenever(communityAPIOffenderService.getOffenderIdentifiers(any())).thenReturn(Mono.empty())

    custodyLocationLookupListener.onApplicationEvent(custodyLocationLookupEvent(referralId, serviceUserCRN))
    verify(telemetryClient).trackEvent(
      "CustodyLocationLookup",
      mapOf(
        "result" to "noms not found",
        "crn" to serviceUserCRN
      ),
      null
    )
  }

  private fun custodyLocationLookupEvent(referralId: UUID, serviceUserCRN: String): CustodyLocationLookupEvent =
    CustodyLocationLookupEvent(
      this,
      referralId,
      serviceUserCRN,
      "testUrl"
    )

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
      expectedReleaseDate
    )

  private fun mockReferralLocation(prisonId: String, expectedReleaseDate: LocalDate) =
    ReferralLocation(
      UUID.randomUUID(),
      referral = mock(),
      type = PersonCurrentLocationType.CUSTODY,
      prisonId = prisonId,
      expectedReleaseDate = expectedReleaseDate,
      expectedReleaseDateMissingReason = null
    )
}
