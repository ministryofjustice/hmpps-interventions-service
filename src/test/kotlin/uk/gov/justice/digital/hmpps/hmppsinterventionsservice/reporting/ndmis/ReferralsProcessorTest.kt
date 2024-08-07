package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.reporting.ndmis

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.reporting.ndmis.performance.NdmisDateTime
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.reporting.ndmis.performance.ReferralsProcessor
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.ActionPlanService
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.ReferralService
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ActionPlanFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.AppointmentFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.EndOfServiceReportFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ReferralFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.WithdrawReasonFactory
import java.time.OffsetDateTime

internal class ReferralsProcessorTest {
  private val actionPlanService = mock<ActionPlanService>()
  private val referralService = mock<ReferralService>()
  private val processor = ReferralsProcessor(actionPlanService, referralService)

  private val referralFactory = ReferralFactory()
  private val eosrFactory = EndOfServiceReportFactory()
  private val appointmentFactory = AppointmentFactory()
  private val actionPlanFactory = ActionPlanFactory()
  private val withdrawalReasonFactory = WithdrawReasonFactory()

  @Test
  fun `referral correctly returns a referralData object`() {
    val sentAt = OffsetDateTime.parse("2021-11-15T09:01:10.486887Z")
    val apSubmittedAt = OffsetDateTime.parse("2021-11-15T15:01:10.486887Z")
    val apApprovedAt = OffsetDateTime.parse("2021-11-15T17:11:10.486887Z")
    val endRequestedAt = OffsetDateTime.parse("2021-11-16T14:42:20.123456Z")
    val eosrSubmittedAt = OffsetDateTime.parse("2021-11-17T08:59:10.486887Z")
    val concludedAt = OffsetDateTime.parse("2021-11-17T09:01:10.486887Z")

    val actionPlanFirst = actionPlanFactory.createApproved(submittedAt = apSubmittedAt, approvedAt = apApprovedAt)
    val actionPlanSecond = actionPlanFactory.createApproved(submittedAt = apSubmittedAt.plusDays(1), approvedAt = apApprovedAt.plusDays(1))

    val referral = referralFactory.createEnded(
      sentAt = sentAt,
      concludedAt = concludedAt,
      endRequestedAt = endRequestedAt,
      endOfServiceReport = eosrFactory.create(submittedAt = eosrSubmittedAt),
      actionPlans = mutableListOf(actionPlanFirst, actionPlanSecond),
    )

    whenever(actionPlanService.getAllCompletedAppointments(anyOrNull())).thenReturn(listOf(appointmentFactory.create()))
    whenever(referralService.getWithdrawalReason(anyOrNull())).thenReturn(withdrawalReasonFactory.create())
    val result = processor.process(referral)!!

    assertThat(result.referralReference).isEqualTo(referral.referenceNumber)
    assertThat(result.referralId).isEqualTo(referral.id)
    assertThat(result.contractReference).isEqualTo(referral.intervention.dynamicFrameworkContract.contractReference)
    assertThat(result.contractType).isEqualTo(referral.intervention.dynamicFrameworkContract.contractType.name)
    assertThat(result.primeProvider).isEqualTo(referral.intervention.dynamicFrameworkContract.primeProvider.name)
    assertThat(result.relevantSentanceId).isEqualTo(123456L)
    assertThat(result.serviceUserCRN).isEqualTo("X123456")
    assertThat(result.dateReferralReceived).isEqualTo(NdmisDateTime(sentAt))
    assertThat(result.firstActionPlanSubmittedAt).isEqualTo(NdmisDateTime(apSubmittedAt))
    assertThat(result.firstActionPlanApprovedAt).isEqualTo(NdmisDateTime(apApprovedAt))
    assertThat(result.numberOfOutcomes).isEqualTo(referral.selectedDesiredOutcomes?.size)
    assertThat(result.achievementScore).isEqualTo(referral.endOfServiceReport?.achievementScore)
    assertThat(result.numberOfSessions).isEqualTo(referral.approvedActionPlan?.numberOfSessions)
    assertThat(result.numberOfSessionsAttended).isEqualTo(1)
    assertThat(result.endRequestedAt).isEqualTo(NdmisDateTime(endRequestedAt))
    assertThat(result.interventionEndReason).isEqualTo(referral.endState)
    assertThat(result.eosrSubmittedAt).isEqualTo(NdmisDateTime(eosrSubmittedAt))
    assertThat(result.endReasonCode).isEqualTo(referral.withdrawalReasonCode)
    assertThat(result.endReasonDescription).isEqualTo("Referral was made by mistake")
    assertThat(result.concludedAt).isEqualTo(NdmisDateTime(concludedAt))
  }
}
