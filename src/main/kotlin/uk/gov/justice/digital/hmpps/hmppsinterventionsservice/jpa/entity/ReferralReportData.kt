package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity

import java.time.Instant
import java.util.UUID

interface ReferralReportData {
  val referralLink: String?
  val referralReference: String
  val referralId: UUID
  val contractId: String
  val organisationId: String
  val referringOfficerEmail: String?
  val caseworkerId: UUID?
  val serviceUserCRN: String
  val dateReferralReceived: Instant
  val dateSAABooked: Instant?
  val dateSAAAttended: Instant?
  val dateFirstActionPlanSubmitted: Instant?
  val dateOfFirstActionPlanApproval: Instant?
  val dateOfFirstAttendedSession: Instant?
  val outcomesToBeAchievedCount: Int?
  val outcomesAchieved: Double?
  val countOfSessionsExpected: Int?
  val countOfSessionsAttended: Int?
  val endRequestedByPPAt: Instant?
  val endRequestedByPPReason: CancellationReason?
  val dateEOSRSubmitted: Instant?
  val concludedAt: Instant?
}
