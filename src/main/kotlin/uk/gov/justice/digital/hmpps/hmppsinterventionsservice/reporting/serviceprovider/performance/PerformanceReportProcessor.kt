package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.reporting.serviceprovider.performance

import mu.KLogging
import net.logstash.logback.argument.StructuredArguments.kv
import org.springframework.batch.item.ItemProcessor
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.EndOfServiceReportRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.reporting.serviceprovider.performance.model.PerformanceReportReferral
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.ActionPlanService

@Component
class PerformanceReportProcessor(
  private val actionPlanService: ActionPlanService,
  private val endOfServiceReportRepository: EndOfServiceReportRepository,
) : ItemProcessor<PerformanceReportReferral, PerformanceReportData> {
  companion object : KLogging()

  override fun process(referral: PerformanceReportReferral): PerformanceReportData {
    logger.debug("processing referral {}", kv("referralId", referral.referralId))

    return PerformanceReportData(
      referralReference = referral.referralReference,
      referralId = referral.referralId,
      contractReference = referral.contractReference,
      organisationId = referral.organisationId,
      currentAssigneeEmail = referral.currentAssigneeEmail,
      serviceUserCRN = referral.serviceUserCRN,
      dateReferralReceived = referral.dateReferralReceived,
      dateSupplierAssessmentFirstArranged = referral.dateSupplierAssessmentFirstArranged,
      dateSupplierAssessmentFirstScheduledFor = referral.dateSupplierAssessmentFirstScheduledFor,
      dateSupplierAssessmentFirstNotAttended = referral.dateSupplierAssessmentFirstNotAttended,
      dateSupplierAssessmentFirstAttended = referral.dateSupplierAssessmentFirstAttended,
      supplierAssessmentAttendedOnTime = referral.supplierAssessmentAttendedOnTime,
      firstActionPlanSubmittedAt = referral.firstActionPlanSubmittedAt,
      firstActionPlanApprovedAt = referral.firstActionPlanApprovedAt,
      firstSessionAttendedAt = referral.approvedActionPlanId?.let { actionPlanService.getFirstAttendedAppointment(referral.referralId)?.appointmentTime },
      numberOfOutcomes = referral.numberOfOutcomes,
      achievementScore = referral.endOfServiceReportId?.let { eosr ->
        endOfServiceReportRepository.findById(eosr).map { it.achievementScore }
          .get()
      },
      numberOfSessions = referral.numberOfSessions,
      numberOfSessionsAttended = referral.approvedActionPlanId?.let { actionPlanService.getAllAttendedAppointments(referral.referralId).size },
      endRequestedAt = referral.endRequestedAt,
      endRequestedReason = referral.endRequestedReason,
      eosrSubmittedAt = referral.eosrSubmittedAt,
      concludedAt = referral.concludedAt,
      referralEndState = endState(referral),
    )
  }

  private fun endState(referral: PerformanceReportReferral): String? {
    if (referral.concludedAt == null) {
      return null
    }

    if (referral.endRequestedAt == null) {
      return "completed"
    }

    return if (referral.endOfServiceReportId == null) "cancelled" else "ended"
  }
}
