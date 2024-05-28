package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.reporting.serviceprovider.performance.model

import jakarta.persistence.Id
import java.time.OffsetDateTime
import java.util.UUID

data class ReferralPerformanceReport(
  @Id val referralId: UUID?,
  val referralReference: String,
  val contractReference: String,
  val organisationId: String,
  val currentAssigneeEmail: String?,
  val crn: String,
  val dateReferralReceived: OffsetDateTime,
  val dateSupplierAssessmentFirstArranged: OffsetDateTime?,
  val dateSupplierAssessmentFirstScheduledFor: OffsetDateTime?,
  val dateSupplierAssessmentFirstNotAttended: OffsetDateTime?,
  val dateSupplierAssessmentFirstAttended: OffsetDateTime?,
  val dateSupplierAssessmentFirstCompleted: OffsetDateTime?,
  val supplierAssessmentAttendedOnTime: Boolean?,
  val firstActionPlanSubmittedAt: OffsetDateTime?,
  val firstActionPlanApprovedAt: OffsetDateTime?,
  val approvedActionPlanId: UUID?,
  val numberOfOutcomes: Long?,
  val endOfServiceReportId: UUID?,
  val numberOfSessions: Int?,
  val endRequestedAt: OffsetDateTime?,
  val endRequestedReason: String?,
  val eosrSubmittedAt: OffsetDateTime?,
  val concludedAt: OffsetDateTime?,
)
