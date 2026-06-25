package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.SASReferralStatus
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ReferralRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.SASReferralNativeData
import java.time.Instant
import java.util.UUID

internal class SASReferralServiceTest {

  private val referralRepository: ReferralRepository = mock()
  private val sasReferralService = SASReferralService(referralRepository)

  private val crn = "X123456"
  private val primeProviderId = "SP_001"
  private val primeProviderName = "TestProvider"

  private fun sentRow(
    referralId: String = UUID.randomUUID().toString(),
    sentAt: Instant = Instant.now(),
    concludedAt: Instant? = null,
    withdrawalReasonCode: String? = null,
    withdrawalComments: String? = null,
    sentByUserId: String? = null,
    sentByAuthSource: String? = null,
    sentByUserName: String? = null,
    endOfServiceReportId: String? = null,
    primeProviderId: String = this.primeProviderId,
    primeProviderName: String = this.primeProviderName,
    subProviderId: String? = null,
    subProviderName: String? = null,
  ) = SASReferralNativeData(
    referralId = referralId,
    sentAt = sentAt,
    concludedAt = concludedAt,
    withdrawalReasonCode = withdrawalReasonCode,
    withdrawalComments = withdrawalComments,
    createdAt = Instant.now(),
    sentByUserId = sentByUserId,
    sentByAuthSource = sentByAuthSource,
    sentByUserName = sentByUserName,
    endOfServiceReportId = endOfServiceReportId,
    primeProviderId = primeProviderId,
    primeProviderName = primeProviderName,
    subProviderId = subProviderId,
    subProviderName = subProviderName,
  )

  @Test
  fun `returns LIVE status for a sent referral`() {
    whenever(referralRepository.getSASReferralsByCrn(crn)).thenReturn(listOf(sentRow()))

    val result = sasReferralService.getReferralsByCrn(crn)

    assertThat(result).hasSize(1)
    assertThat(result[0].status).isEqualTo(SASReferralStatus.LIVE)
    assertThat(result[0].sentAt).isNotNull()
    assertThat(result[0].sentBy).isNull()
  }

  @Test
  fun `returns empty list when no referrals found`() {
    whenever(referralRepository.getSASReferralsByCrn(crn)).thenReturn(emptyList())

    val result = sasReferralService.getReferralsByCrn(crn)

    assertThat(result).isEmpty()
  }

  @Test
  fun `returns COMPLETED status when concludedAt set and end-of-service report present`() {
    val row = sentRow(concludedAt = Instant.now(), endOfServiceReportId = UUID.randomUUID().toString())
    whenever(referralRepository.getSASReferralsByCrn(crn)).thenReturn(listOf(row))

    val result = sasReferralService.getReferralsByCrn(crn)

    assertThat(result).hasSize(1)
    assertThat(result[0].status).isEqualTo(SASReferralStatus.COMPLETED)
  }

  @Test
  fun `returns WITHDRAWN status when withdrawalReasonCode and withdrawalComments are present`() {
    val row = sentRow(withdrawalReasonCode = "MIS", withdrawalComments = "No longer required")
    whenever(referralRepository.getSASReferralsByCrn(crn)).thenReturn(listOf(row))

    val result = sasReferralService.getReferralsByCrn(crn)

    assertThat(result).hasSize(1)
    assertThat(result[0].status).isEqualTo(SASReferralStatus.WITHDRAWN)
  }

  @Test
  fun `returns LIVE not COMPLETED when concludedAt set but no end-of-service report`() {
    val row = sentRow(concludedAt = Instant.now(), endOfServiceReportId = null)
    whenever(referralRepository.getSASReferralsByCrn(crn)).thenReturn(listOf(row))

    val result = sasReferralService.getReferralsByCrn(crn)

    assertThat(result).hasSize(1)
    assertThat(result[0].status).isEqualTo(SASReferralStatus.LIVE)
  }

  @Test
  fun `returns referral with sentBy details when present`() {
    val sentAt = Instant.parse("2026-04-30T12:43:15Z")
    val row = sentRow(
      sentAt = sentAt,
      sentByUserId = "user-id-001",
      sentByAuthSource = "delius",
      sentByUserName = "john.smith",
    )
    whenever(referralRepository.getSASReferralsByCrn(crn)).thenReturn(listOf(row))

    val result = sasReferralService.getReferralsByCrn(crn)

    assertThat(result).hasSize(1)
    assertThat(result[0].sentBy!!.username).isEqualTo("john.smith")
    assertThat(result[0].sentBy!!.authSource).isEqualTo("delius")
    assertThat(result[0].sentBy!!.userId).isEqualTo("user-id-001")
  }

  @Test
  fun `returns service providers including prime provider`() {
    val row = sentRow(primeProviderId = "SP_PRIME", primeProviderName = "PrimeProvider")
    whenever(referralRepository.getSASReferralsByCrn(crn)).thenReturn(listOf(row))

    val result = sasReferralService.getReferralsByCrn(crn)

    assertThat(result).hasSize(1)
    assertThat(result[0].referral.serviceProviders.map { it.name }).contains("PrimeProvider")
  }

  @Test
  fun `aggregates subcontractor providers alongside prime provider for the same referral`() {
    val id = UUID.randomUUID().toString()
    val row1 = sentRow(referralId = id, primeProviderId = "SP_PRIME", primeProviderName = "PrimeProvider")
    val row2 = row1.copy(subProviderId = "SP_SUB", subProviderName = "SubProvider")
    whenever(referralRepository.getSASReferralsByCrn(crn)).thenReturn(listOf(row1, row2))

    val result = sasReferralService.getReferralsByCrn(crn)

    assertThat(result).hasSize(1)
    assertThat(result[0].referral.serviceProviders).hasSize(2)
    assertThat(result[0].referral.serviceProviders.map { it.name }).containsExactlyInAnyOrder("PrimeProvider", "SubProvider")
  }

  @Test
  fun `returns multiple referrals for the same CRN`() {
    val row1 = sentRow()
    val row2 = sentRow(concludedAt = Instant.now(), endOfServiceReportId = UUID.randomUUID().toString())
    whenever(referralRepository.getSASReferralsByCrn(crn)).thenReturn(listOf(row1, row2))

    val result = sasReferralService.getReferralsByCrn(crn)

    assertThat(result).hasSize(2)
    assertThat(result.map { it.status }).containsExactlyInAnyOrder(SASReferralStatus.LIVE, SASReferralStatus.COMPLETED)
  }
}
