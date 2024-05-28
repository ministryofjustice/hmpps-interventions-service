package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository

import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.reporting.serviceprovider.performance.model.ReferralPerformanceReport
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.*

interface ReferralPerformanceReportRepository {
  // queries for reporting
  fun serviceProviderReportReferrals(from: OffsetDateTime, to: OffsetDateTime, contractReferences: List<String>): List<ReferralPerformanceReport>
  fun eosrAchievementScore(eosrId: UUID): BigDecimal
  fun firstAttendanceDate(referralId: UUID): OffsetDateTime
  fun attendanceCount(referralId: UUID): Integer
}
