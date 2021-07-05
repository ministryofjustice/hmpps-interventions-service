package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto

import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.CancellationReason
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ReferralReportData
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

data class ReferralReportDataDTO(
  val referralLink: String?,
  val referralRef: String?,
  val referralId: UUID,
  val contractId: String,
  val organisationId: String,
  val referringOfficerEmail: String?,
  val caseworkerId: UUID?,
  val serviceUserCRN: String,
  val dateReferralReceived: OffsetDateTime,
  val dateSAABooked: OffsetDateTime?,
  val dateSAAAttended: OffsetDateTime?,
  val dateFirstActionPlanSubmitted: OffsetDateTime?,
  val dateOfFirstActionPlanApproval: OffsetDateTime?,
  val dateOfFirstAttendedSession: OffsetDateTime?,
  val outcomesToBeAchievedCount: Int?,
  val outcomesAchieved: Double?,
  val countOfSessionsExpected: Int?,
  val countOfSessionsAttended: Int?,
  val endRequestedByPPAt: OffsetDateTime?,
  val endRequestedByPPReason: CancellationReason?,
  val dateEOSRSubmitted: OffsetDateTime?,
  val concludedAt: OffsetDateTime?,
){
  companion object {
    fun from(referralReportData: ReferralReportData, referralLink: String): ReferralReportDataDTO {
      return ReferralReportDataDTO(
        referralLink = referralLink,
        referralRef = referralReportData.referralReference,
        referralId = referralReportData.referralId,
        contractId = referralReportData.contractId,
        organisationId = referralReportData.organisationId,
        referringOfficerEmail = referralReportData.referringOfficerEmail,
        caseworkerId = referralReportData.caseworkerId,
        serviceUserCRN = referralReportData.serviceUserCRN,
        dateReferralReceived = OffsetDateTime.ofInstant(referralReportData.dateReferralReceived, ZoneOffset.UTC),
        dateSAABooked = referralReportData.dateSAABooked ?.let {OffsetDateTime.ofInstant(referralReportData.dateSAABooked, ZoneOffset.UTC)},
        dateSAAAttended = referralReportData.dateSAAAttended ?.let{ OffsetDateTime.ofInstant(referralReportData.dateSAAAttended, ZoneOffset.UTC)},
        dateFirstActionPlanSubmitted = referralReportData.dateFirstActionPlanSubmitted ?.let{ OffsetDateTime.ofInstant(referralReportData.dateFirstActionPlanSubmitted, ZoneOffset.UTC)},
        dateOfFirstActionPlanApproval = referralReportData.dateOfFirstActionPlanApproval ?. let{ OffsetDateTime.ofInstant(referralReportData.dateOfFirstActionPlanApproval, ZoneOffset.UTC)},
        dateOfFirstAttendedSession = referralReportData.dateOfFirstAttendedSession ?. let{ OffsetDateTime.ofInstant(referralReportData.dateOfFirstAttendedSession, ZoneOffset.UTC)},
        outcomesToBeAchievedCount = referralReportData.outcomesToBeAchievedCount,
        outcomesAchieved =  referralReportData.outcomesAchieved,
        countOfSessionsExpected = referralReportData.countOfSessionsExpected,
        countOfSessionsAttended = referralReportData.countOfSessionsAttended,
        endRequestedByPPAt = referralReportData.endRequestedByPPAt ?. let{ OffsetDateTime.ofInstant(referralReportData.endRequestedByPPAt, ZoneOffset.UTC)},
        endRequestedByPPReason = referralReportData.endRequestedByPPReason,
        dateEOSRSubmitted = referralReportData.dateEOSRSubmitted ?. let{ OffsetDateTime.ofInstant(referralReportData.dateEOSRSubmitted, ZoneOffset.UTC)},
        concludedAt = referralReportData.concludedAt ?. let{ OffsetDateTime.ofInstant(referralReportData.concludedAt, ZoneOffset.UTC)}
      )
    }
  }
}
