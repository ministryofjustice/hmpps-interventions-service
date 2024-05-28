package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.reporting.serviceprovider.performance

import mu.KLogging
import net.logstash.logback.argument.StructuredArguments.kv
import org.springframework.batch.item.ItemProcessor
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ReferralPerformanceReportRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.reporting.serviceprovider.performance.model.ReferralPerformanceReport

@Component
class PerformanceReportProcessor(
  private val referralPerformanceReportRepository: ReferralPerformanceReportRepository,
) : ItemProcessor<ReferralPerformanceReport, PerformanceReportData> {
  companion object : KLogging()

  override fun process(referral: ReferralPerformanceReport): PerformanceReportData {
    logger.debug("processing referral {}", kv("referralId", referral.referralId))

    return PerformanceReportData(
      referralReference = referral.referralReference,
      referralId = referral.referralId,
      contractReference = referral.contractReference,
      organisationId = referral.organisationId,
      currentAssigneeEmail = referral.currentAssigneeEmail,
      serviceUserCRN = referral.crn,
      dateReferralReceived = referral.dateReferralReceived,
      dateSupplierAssessmentFirstArranged = referral.dateSupplierAssessmentFirstArranged,
      dateSupplierAssessmentFirstScheduledFor = referral.dateSupplierAssessmentFirstScheduledFor,
      dateSupplierAssessmentFirstNotAttended = referral.dateSupplierAssessmentFirstNotAttended,
      dateSupplierAssessmentFirstAttended = referral.dateSupplierAssessmentFirstAttended,
      dateSupplierAssessmentFirstCompleted = referral.dateSupplierAssessmentFirstCompleted,
      supplierAssessmentAttendedOnTime = referral.supplierAssessmentAttendedOnTime,
      firstActionPlanSubmittedAt = referral.firstActionPlanSubmittedAt,
      firstActionPlanApprovedAt = referral.firstActionPlanApprovedAt,
      firstSessionAttendedAt = referral.approvedActionPlanId?.let { referral.referralId?.let { it1 -> referralPerformanceReportRepository.firstAttendanceDate(it1) } },
      numberOfOutcomes = referral.numberOfOutcomes?.toInt(),
      achievementScore = referral.endOfServiceReportId?.let { eosr ->
        referralPerformanceReportRepository.eosrAchievementScore(eosr).toFloat()
      },
      numberOfSessions = referral.numberOfSessions,
      numberOfSessionsAttended = referral.approvedActionPlanId?.let { referral.referralId?.let { it1 -> referralPerformanceReportRepository.attendanceCount(it1).toInt() } },
      endRequestedAt = referral.endRequestedAt,
      endRequestedReason = referral.endRequestedReason,
      eosrSubmittedAt = referral.eosrSubmittedAt,
      concludedAt = referral.concludedAt,
      referralEndState = endState(referral),
      dateInterventionToBeCompletedBy = referral.completionDeadline,
    )
  }

  private fun endState(referral: ReferralPerformanceReport): String? {
    if (referral.concludedAt == null) {
      return null
    }

    if (referral.endRequestedAt == null) {
      return "completed"
    }

    return if (referral.endOfServiceReportId == null) "cancelled" else "ended"
  }
}
