package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository

import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.reporting.serviceprovider.performance.model.ReferralPerformanceReport
import java.math.BigDecimal
import java.sql.Timestamp
import java.time.OffsetDateTime
import java.time.ZoneId
import java.util.*

@Repository
class ReferralPerformanceReportRepositoryImpl : ReferralPerformanceReportRepository {

  @PersistenceContext
  private lateinit var entityManager: EntityManager

  private fun reportQuery(): String {
    return "select * from performance_report(array[:contractReferences],:from, :to)"
  }

  private fun eosrScoreQuery(): String {
    return "select * from performance_report_eosr_outcome_score(:eosrId)"
  }

  private fun attendedAppointmentCountQuery(): String {
    return "select * from performance_report_attendances_count(:referralId)"
  }

  private fun firstAttendanceDateQuery(): String {
    return "select * from performance_report_first_attendance(:referralId)"
  }

  override fun serviceProviderReportReferrals(
    from: OffsetDateTime,
    to: OffsetDateTime,
    contractReferences: List<String>,
  ): List<ReferralPerformanceReport> {
    val query = entityManager.createNativeQuery(reportQuery())
    query.setParameter("contractReferences", contractReferences)
    query.setParameter("from", from)
    query.setParameter("to", to)
    val result = query.resultList as List<Array<Any>>
    val records: MutableList<ReferralPerformanceReport> = mutableListOf()
    result.forEach { row ->
      val referralId = row[0] as UUID
      val referralReference = row[1] as String
      val contractReference = row[2] as String
      val organisationId = row[3] as String
      val currentAssigneeEmail = row[4] as String?
      val crn = row[5] as String
      val dateReferralReceived = timestampToOffset(row[6] as Timestamp?)
      val dateSupplierAssessmentFirstArranged = timestampToOffset(row[7] as Timestamp?) as OffsetDateTime?
      val dateSupplierAssessmentFirstScheduledFor = timestampToOffset(row[8] as Timestamp?) as OffsetDateTime?
      val dateSupplierAssessmentFirstNotAttended = timestampToOffset(row[9] as Timestamp?) as OffsetDateTime?
      val dateSupplierAssessmentFirstAttended = timestampToOffset(row[10] as Timestamp?) as OffsetDateTime?
      val dateSupplierAssessmentFirstCompleted = timestampToOffset(row[11] as Timestamp?) as OffsetDateTime?
      val supplierAssessmentAttendedOnTime = stringToBoolean(row[12] as String?)
      val firstActionPlanSubmittedAt = timestampToOffset(row[13] as Timestamp?) as OffsetDateTime?
      val firstActionPlanApprovedAt = timestampToOffset(row[14] as Timestamp?) as OffsetDateTime?
      val approvedActionPlanId = row[15] as UUID?
      val numberOfOutcomes = (row[16] as Int).toLong()
      val endOfServiceReportId = row[17] as UUID?
      val numberOfSessions = row[18] as Int?
      val endRequestedAt = timestampToOffset(row[19] as Timestamp?) as OffsetDateTime?
      val endRequestedReason = row[20] as String?
      val eosrSubmittedAt = timestampToOffset(row[21] as Timestamp?) as OffsetDateTime?
      val concludedAt = timestampToOffset(row[22] as Timestamp?) as OffsetDateTime?
      records.add(
        ReferralPerformanceReport(
          referralId = referralId,
          referralReference = referralReference,
          contractReference = contractReference,
          organisationId = organisationId,
          currentAssigneeEmail = currentAssigneeEmail,
          crn = crn,
          dateReferralReceived = dateReferralReceived,
          dateSupplierAssessmentFirstArranged = dateSupplierAssessmentFirstArranged,
          dateSupplierAssessmentFirstScheduledFor = dateSupplierAssessmentFirstScheduledFor,
          dateSupplierAssessmentFirstNotAttended = dateSupplierAssessmentFirstNotAttended,
          dateSupplierAssessmentFirstAttended = dateSupplierAssessmentFirstAttended,
          dateSupplierAssessmentFirstCompleted = dateSupplierAssessmentFirstCompleted,
          supplierAssessmentAttendedOnTime = supplierAssessmentAttendedOnTime,
          firstActionPlanSubmittedAt = firstActionPlanSubmittedAt,
          firstActionPlanApprovedAt = firstActionPlanApprovedAt,
          approvedActionPlanId = approvedActionPlanId,
          numberOfOutcomes = numberOfOutcomes,
          endOfServiceReportId = endOfServiceReportId,
          numberOfSessions = numberOfSessions,
          endRequestedAt = endRequestedAt,
          endRequestedReason = endRequestedReason,
          eosrSubmittedAt = eosrSubmittedAt,
          concludedAt = concludedAt,
        ),
      )
    }
    return records
  }
  override fun eosrAchievementScore(eosrId: UUID): BigDecimal {
    val query = entityManager.createNativeQuery(eosrScoreQuery())
    query.setParameter("eosrId", eosrId)
    val result = query.resultList as BigDecimal
    return result
  }

  override fun firstAttendanceDate(referralId: UUID): OffsetDateTime {
    val query = entityManager.createNativeQuery(firstAttendanceDateQuery())
    query.setParameter("referralId", referralId)
    val result = query.resultList as Timestamp?
    return timestampToOffset(result)
  }

  override fun attendanceCount(referralId: UUID): Integer {
    val query = entityManager.createNativeQuery(attendedAppointmentCountQuery())
    query.setParameter("referralId", referralId)
    val result = query.resultList as Integer
    return result
  }

  fun timestampToOffset(timestamp: Timestamp?): OffsetDateTime {
    val currentTimeMillis = System.currentTimeMillis()
    val resolved = timestamp ?: Timestamp(currentTimeMillis)
    return OffsetDateTime.ofInstant(resolved.toInstant(), ZoneId.of("UTC"))
  }

  fun stringToBoolean(inStr: String?): Boolean {
    val resolved = inStr ?: "false"
    return resolved.toBoolean()
  }
}
