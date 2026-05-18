package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.SASReferralStatus
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.SampleData
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.DraftReferralRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ReferralRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.EndOfServiceReportFactory
import java.time.OffsetDateTime
import java.util.UUID

internal class SASReferralServiceTest {

  private val referralRepository: ReferralRepository = mock()
  private val draftReferralRepository: DraftReferralRepository = mock()
  private val endOfServiceReportFactory = EndOfServiceReportFactory()

  private val sasReferralService = SASReferralService(referralRepository, draftReferralRepository)

  private val crn = "X123456"

  // sampleReferral defaults to Accommodation service category (via sampleContractType → sampleServiceCategory)
  private fun accommodationReferral(
    id: UUID = UUID.randomUUID(),
    sentAt: OffsetDateTime? = OffsetDateTime.now(),
    concludedAt: OffsetDateTime? = null,
    endOfServiceReport: uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.EndOfServiceReport? = null,
    sentBy: uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AuthUser? = null,
  ) = SampleData.sampleReferral(
    crn = crn,
    serviceProviderName = "TestProvider",
    id = id,
    sentAt = sentAt,
    concludedAt = concludedAt,
    endOfServiceReport = endOfServiceReport,
    sentBy = sentBy,
  )

  // referral with a non-Accommodation service category
  private fun nonAccommodationReferral(id: UUID = UUID.randomUUID()) = SampleData.sampleReferral(
    crn = crn,
    serviceProviderName = "TestProvider",
    id = id,
    sentAt = OffsetDateTime.now(),
    intervention = SampleData.sampleIntervention(
      dynamicFrameworkContract = SampleData.sampleContract(
        primeProvider = SampleData.sampleServiceProvider(),
        contractType = SampleData.sampleContractType(
          name = "Education, Training and Employment",
          code = "ETE",
          serviceCategories = setOf(
            SampleData.sampleServiceCategory(name = "Education, Training and Employment"),
          ),
        ),
      ),
    ),
  )

  @Test
  fun `returns LIVE status for a sent referral with Accommodation service category`() {
    val referral = accommodationReferral()
    whenever(referralRepository.findByServiceUserCRN(crn)).thenReturn(listOf(referral))
    whenever(draftReferralRepository.findDraftOnlyByServiceUserCRN(crn)).thenReturn(emptyList())

    val result = sasReferralService.getReferralsByCrn(crn)

    assertThat(result).hasSize(1)
    assertThat(result[0].status).isEqualTo(SASReferralStatus.LIVE)
    assertThat(result[0].sentAt).isNotNull()
    assertThat(result[0].sentBy).isNull()
  }

  @Test
  fun `excludes sent referrals that do not have Accommodation service category`() {
    val referral = nonAccommodationReferral()
    whenever(referralRepository.findByServiceUserCRN(crn)).thenReturn(listOf(referral))
    whenever(draftReferralRepository.findDraftOnlyByServiceUserCRN(crn)).thenReturn(emptyList())

    val result = sasReferralService.getReferralsByCrn(crn)

    assertThat(result).isEmpty()
  }

  @Test
  fun `excludes sent referrals with null selectedServiceCategories`() {
    val referral = accommodationReferral()
    referral.selectedServiceCategories = null
    whenever(referralRepository.findByServiceUserCRN(crn)).thenReturn(listOf(referral))
    whenever(draftReferralRepository.findDraftOnlyByServiceUserCRN(crn)).thenReturn(emptyList())

    val result = sasReferralService.getReferralsByCrn(crn)

    assertThat(result).isEmpty()
  }

  @Test
  fun `excludes draft referrals without Accommodation service category`() {
    val draftReferral = SampleData.sampleDraftReferral(
      crn = crn,
      serviceProviderName = "TestProvider",
      intervention = SampleData.sampleIntervention(
        dynamicFrameworkContract = SampleData.sampleContract(
          primeProvider = SampleData.sampleServiceProvider(),
          contractType = SampleData.sampleContractType(
            name = "Education, Training and Employment",
            code = "ETE",
            serviceCategories = setOf(
              SampleData.sampleServiceCategory(name = "Education, Training and Employment"),
            ),
          ),
        ),
      ),
    )
    whenever(referralRepository.findByServiceUserCRN(crn)).thenReturn(emptyList())
    whenever(draftReferralRepository.findDraftOnlyByServiceUserCRN(crn)).thenReturn(listOf(draftReferral))

    val result = sasReferralService.getReferralsByCrn(crn)

    assertThat(result).isEmpty()
  }

  @Test
  fun `includes draft referrals with Accommodation service category`() {
    val draftReferral = SampleData.sampleDraftReferral(crn = crn, serviceProviderName = "TestProvider")
    whenever(referralRepository.findByServiceUserCRN(crn)).thenReturn(emptyList())
    whenever(draftReferralRepository.findDraftOnlyByServiceUserCRN(crn)).thenReturn(listOf(draftReferral))

    val result = sasReferralService.getReferralsByCrn(crn)

    assertThat(result).hasSize(1)
    assertThat(result[0].status).isEqualTo(SASReferralStatus.DRAFT)
  }

  @Test
  fun `filters mixed referrals keeping only Accommodation ones`() {
    val accommodationRef = accommodationReferral(id = UUID.randomUUID())
    val nonAccommodationRef = nonAccommodationReferral(id = UUID.randomUUID())
    whenever(referralRepository.findByServiceUserCRN(crn)).thenReturn(listOf(accommodationRef, nonAccommodationRef))
    whenever(draftReferralRepository.findDraftOnlyByServiceUserCRN(crn)).thenReturn(emptyList())

    val result = sasReferralService.getReferralsByCrn(crn)

    assertThat(result).hasSize(1)
    assertThat(result[0].referral.serviceProviders.map { it.name }).contains("TestProvider")
  }

  @Test
  fun `returns COMPLETED status for referral with concludedAt and end-of-service report`() {
    val referral = accommodationReferral(
      concludedAt = OffsetDateTime.now(),
      endOfServiceReport = endOfServiceReportFactory.create(),
    )
    whenever(referralRepository.findByServiceUserCRN(crn)).thenReturn(listOf(referral))
    whenever(draftReferralRepository.findDraftOnlyByServiceUserCRN(crn)).thenReturn(emptyList())

    val result = sasReferralService.getReferralsByCrn(crn)

    assertThat(result).hasSize(1)
    assertThat(result[0].status).isEqualTo(SASReferralStatus.COMPLETED)
  }

  @Test
  fun `returns WITHDRAWN status for referral with withdrawalReasonCode and withdrawalComments`() {
    val referral = accommodationReferral()
    referral.withdrawalReasonCode = "MIS"
    referral.withdrawalComments = "No longer required"
    whenever(referralRepository.findByServiceUserCRN(crn)).thenReturn(listOf(referral))
    whenever(draftReferralRepository.findDraftOnlyByServiceUserCRN(crn)).thenReturn(emptyList())

    val result = sasReferralService.getReferralsByCrn(crn)

    assertThat(result).hasSize(1)
    assertThat(result[0].status).isEqualTo(SASReferralStatus.WITHDRAWN)
  }

  @Test
  fun `returns LIVE not COMPLETED when concludedAt set but no end-of-service report`() {
    val referral = accommodationReferral(concludedAt = OffsetDateTime.now(), endOfServiceReport = null)
    whenever(referralRepository.findByServiceUserCRN(crn)).thenReturn(listOf(referral))
    whenever(draftReferralRepository.findDraftOnlyByServiceUserCRN(crn)).thenReturn(emptyList())

    val result = sasReferralService.getReferralsByCrn(crn)

    assertThat(result).hasSize(1)
    assertThat(result[0].status).isEqualTo(SASReferralStatus.LIVE)
  }

  @Test
  fun `returns referral with sentBy details when present`() {
    val sentAt = OffsetDateTime.parse("2026-04-30T12:43:15Z")
    val referral = accommodationReferral(
      sentAt = sentAt,
      sentBy = uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AuthUser(
        id = "user-id-001",
        authSource = "delius",
        userName = "john.smith",
      ),
    )
    whenever(referralRepository.findByServiceUserCRN(crn)).thenReturn(listOf(referral))
    whenever(draftReferralRepository.findDraftOnlyByServiceUserCRN(crn)).thenReturn(emptyList())

    val result = sasReferralService.getReferralsByCrn(crn)

    assertThat(result).hasSize(1)
    assertThat(result[0].sentAt).isEqualTo(sentAt)
    assertThat(result[0].sentBy!!.username).isEqualTo("john.smith")
    assertThat(result[0].sentBy!!.authSource).isEqualTo("delius")
    assertThat(result[0].sentBy!!.userId).isEqualTo("user-id-001")
  }

  @Test
  fun `returns service providers including prime provider`() {
    val referral = SampleData.sampleReferral(
      crn = crn,
      serviceProviderName = "PrimeProvider",
      id = UUID.randomUUID(),
      sentAt = OffsetDateTime.now(),
    )
    whenever(referralRepository.findByServiceUserCRN(crn)).thenReturn(listOf(referral))
    whenever(draftReferralRepository.findDraftOnlyByServiceUserCRN(crn)).thenReturn(emptyList())

    val result = sasReferralService.getReferralsByCrn(crn)

    assertThat(result).hasSize(1)
    assertThat(result[0].referral.serviceProviders.map { it.name }).contains("PrimeProvider")
  }

  @Test
  fun `returns empty list when no referrals found`() {
    whenever(referralRepository.findByServiceUserCRN(crn)).thenReturn(emptyList())
    whenever(draftReferralRepository.findDraftOnlyByServiceUserCRN(crn)).thenReturn(emptyList())

    val result = sasReferralService.getReferralsByCrn(crn)

    assertThat(result).isEmpty()
  }

  @Test
  fun `returns multiple Accommodation referrals for the same CRN`() {
    val referral1 = accommodationReferral(id = UUID.randomUUID())
    val referral2 = accommodationReferral(
      id = UUID.randomUUID(),
      concludedAt = OffsetDateTime.now(),
      endOfServiceReport = endOfServiceReportFactory.create(),
    )
    whenever(referralRepository.findByServiceUserCRN(crn)).thenReturn(listOf(referral1, referral2))
    whenever(draftReferralRepository.findDraftOnlyByServiceUserCRN(crn)).thenReturn(emptyList())

    val result = sasReferralService.getReferralsByCrn(crn)

    assertThat(result).hasSize(2)
    assertThat(result.map { it.status }).containsExactlyInAnyOrder(SASReferralStatus.LIVE, SASReferralStatus.COMPLETED)
  }
}
