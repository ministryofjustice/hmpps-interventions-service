package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.controller

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.web.server.ResponseStatusException
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.AuthUserDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.SASReferralDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.SASReferralDetailsDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.SASReferralStatus
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.ServiceProviderDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.SASReferralService
import java.time.OffsetDateTime

internal class SASReferralControllerTest {

  private val sasReferralService: SASReferralService = mock()
  private val sasReferralController = SASReferralController(sasReferralService)

  private val crn = "X123456"

  @Test
  fun `returns referrals for a valid CRN`() {
    val sentAt = OffsetDateTime.parse("2026-04-30T12:43:15Z")
    val createdAt = OffsetDateTime.parse("2026-04-29T10:00:00Z")
    val referrals = listOf(
      SASReferralDTO(
        status = SASReferralStatus.LIVE,
        sentAt = sentAt,
        sentBy = AuthUserDTO(username = "john.smith", authSource = "delius", userId = "user-001"),
        referral = SASReferralDetailsDTO(
          createdAt = createdAt,
          serviceProviders = listOf(ServiceProviderDTO(name = "TestProvider", id = "SP001")),
        ),
      ),
    )
    whenever(sasReferralService.getReferralsByCrn(crn)).thenReturn(referrals)

    val result = sasReferralController.getReferralDetails(crn)

    assertThat(result).hasSize(1)
    assertThat(result[0].status).isEqualTo(SASReferralStatus.LIVE)
    assertThat(result[0].sentAt).isEqualTo(sentAt)
    assertThat(result[0].sentBy?.username).isEqualTo("john.smith")
    assertThat(result[0].referral.createdAt).isEqualTo(createdAt)
    assertThat(result[0].referral.serviceProviders).hasSize(1)
    assertThat(result[0].referral.serviceProviders[0].name).isEqualTo("TestProvider")
  }

  @Test
  fun `returns DRAFT referral when sentAt and sentBy are null`() {
    val createdAt = OffsetDateTime.now()
    val referrals = listOf(
      SASReferralDTO(
        status = SASReferralStatus.DRAFT,
        sentAt = null,
        sentBy = null,
        referral = SASReferralDetailsDTO(
          createdAt = createdAt,
          serviceProviders = listOf(ServiceProviderDTO(name = "Provider", id = "SP001")),
        ),
      ),
    )
    whenever(sasReferralService.getReferralsByCrn(crn)).thenReturn(referrals)

    val result = sasReferralController.getReferralDetails(crn)

    assertThat(result).hasSize(1)
    assertThat(result[0].status).isEqualTo(SASReferralStatus.DRAFT)
    assertThat(result[0].sentAt).isNull()
    assertThat(result[0].sentBy).isNull()
  }

  @Test
  fun `returns COMPLETED referral`() {
    val referrals = listOf(
      SASReferralDTO(
        status = SASReferralStatus.COMPLETED,
        sentAt = OffsetDateTime.now(),
        sentBy = null,
        referral = SASReferralDetailsDTO(
          createdAt = OffsetDateTime.now(),
          serviceProviders = emptyList(),
        ),
      ),
    )
    whenever(sasReferralService.getReferralsByCrn(crn)).thenReturn(referrals)

    val result = sasReferralController.getReferralDetails(crn)

    assertThat(result[0].status).isEqualTo(SASReferralStatus.COMPLETED)
  }

  @Test
  fun `returns WITHDRAWN referral`() {
    val referrals = listOf(
      SASReferralDTO(
        status = SASReferralStatus.WITHDRAWN,
        sentAt = OffsetDateTime.now(),
        sentBy = null,
        referral = SASReferralDetailsDTO(
          createdAt = OffsetDateTime.now(),
          serviceProviders = emptyList(),
        ),
      ),
    )
    whenever(sasReferralService.getReferralsByCrn(crn)).thenReturn(referrals)

    val result = sasReferralController.getReferralDetails(crn)

    assertThat(result[0].status).isEqualTo(SASReferralStatus.WITHDRAWN)
  }

  @Test
  fun `returns multiple referrals for a CRN`() {
    val referrals = listOf(
      SASReferralDTO(
        status = SASReferralStatus.LIVE,
        sentAt = OffsetDateTime.now(),
        sentBy = null,
        referral = SASReferralDetailsDTO(createdAt = OffsetDateTime.now(), serviceProviders = emptyList()),
      ),
      SASReferralDTO(
        status = SASReferralStatus.COMPLETED,
        sentAt = OffsetDateTime.now(),
        sentBy = null,
        referral = SASReferralDetailsDTO(createdAt = OffsetDateTime.now(), serviceProviders = emptyList()),
      ),
      SASReferralDTO(
        status = SASReferralStatus.DRAFT,
        sentAt = null,
        sentBy = null,
        referral = SASReferralDetailsDTO(createdAt = OffsetDateTime.now(), serviceProviders = emptyList()),
      ),
    )
    whenever(sasReferralService.getReferralsByCrn(crn)).thenReturn(referrals)

    val result = sasReferralController.getReferralDetails(crn)

    assertThat(result).hasSize(3)
    assertThat(result.map { it.status }).containsExactlyInAnyOrder(
      SASReferralStatus.LIVE,
      SASReferralStatus.COMPLETED,
      SASReferralStatus.DRAFT,
    )
  }

  @Test
  fun `returns 404 when no referrals found for CRN`() {
    whenever(sasReferralService.getReferralsByCrn("NOTFOUND")).thenReturn(emptyList())

    val exception = assertThrows<ResponseStatusException> {
      sasReferralController.getReferralDetails("NOTFOUND")
    }

    assertThat(exception.statusCode.value()).isEqualTo(404)
    assertThat(exception.reason).contains("NOTFOUND")
  }
}
