package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.authorization.ServiceProviderAccessScope
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.authorization.ServiceProviderAccessScopeMapper
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.CancellationReason
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ReferralReportData
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ReferralRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.AuthUserFactory
import java.time.Instant
import java.time.OffsetDateTime
import java.util.UUID


class ReportingServiceTest {
  private val referralRepository: ReferralRepository = mock()
  private val serviceProviderAccessScopeMapper: ServiceProviderAccessScopeMapper = mock()
  private val authUserFactory = AuthUserFactory()

  private val reportingService = ReportingService(
    referralRepository, serviceProviderAccessScopeMapper, "http://abc", "/service-provider/referrals/{id}/details"
  )

  @Test
  fun `getReportData builds successful response`(){
    val from = OffsetDateTime.parse("2020-12-04T00:00:00+00:00")
    val to = OffsetDateTime.parse("2020-12-04T00:00:00+00:00")
    val user = authUserFactory.create()
    val referralId = UUID.randomUUID()
    val serviceProviderAccessScope = ServiceProviderAccessScope(setOf(), setOf())

    val reportData: ReferralReportData =
      ReportDataTest(
      referralLink = null,
      referralReference = "abc123",
      referralId = referralId,
      contractId = "aaa",
      organisationId = "bbb",
      referringOfficerEmail = null,
      caseworkerId = null,
      serviceUserCRN = "ccc",
      dateReferralReceived = Instant.now(),
      dateSAABooked = null,
      dateSAAAttended = null,
      dateFirstActionPlanSubmitted = null,
      dateOfFirstActionPlanApproval = null,
      dateOfFirstAttendedSession = null,
      outcomesToBeAchievedCount = null,
      outcomesAchieved = null,
      countOfSessionsExpected = null,
      countOfSessionsAttended = null,
      endRequestedByPPAt = null,
      endRequestedByPPReason = null,
      dateEOSRSubmitted = null,
      concludedAt = null,
      )

    whenever(serviceProviderAccessScopeMapper.fromUser(user)).thenReturn(serviceProviderAccessScope)
    whenever(referralRepository.reportingData(from, to, setOf())).thenReturn(listOf(reportData))

    val result = reportingService.getReportData(from, to, user)
    assertThat(result.size).isEqualTo(1)
    assertThat(result[0].referralLink).isEqualTo("http://abc/service-provider/referrals/$referralId/details")
  }
}

data class ReportDataTest(
  override val referralLink: String?,
  override val referralReference: String,
  override var referralId: UUID,
  override val contractId: String,
  override val organisationId: String,
  override val referringOfficerEmail: String?,
  override val caseworkerId: UUID?,
  override val serviceUserCRN: String,
  override val dateReferralReceived: Instant,
  override val dateSAABooked: Instant?,
  override val dateSAAAttended: Instant?,
  override val dateFirstActionPlanSubmitted: Instant?,
  override val dateOfFirstActionPlanApproval: Instant?,
  override val dateOfFirstAttendedSession: Instant?,
  override val outcomesToBeAchievedCount: Int?,
  override val outcomesAchieved: Double?,
  override val countOfSessionsExpected: Int?,
  override val countOfSessionsAttended: Int?,
  override val endRequestedByPPAt: Instant?,
  override val endRequestedByPPReason: CancellationReason?,
  override val dateEOSRSubmitted: Instant?,
  override val concludedAt: Instant?,
): ReferralReportData