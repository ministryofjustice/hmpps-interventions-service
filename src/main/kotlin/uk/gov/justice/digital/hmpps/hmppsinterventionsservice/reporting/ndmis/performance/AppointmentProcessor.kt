package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.reporting.ndmis.performance

import jakarta.persistence.EntityManager
import mu.KLogging
import org.springframework.batch.item.ItemProcessor
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Attended
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.reporting.QueryLoader
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.util.*

@Component
class AppointmentProcessor(
  private val entityManager: EntityManager,
  private val queryLoader: QueryLoader,
) : ItemProcessor<ReferralMetadata, List<AppointmentData>> {
  companion object : KLogging()

  override fun process(referralData: ReferralMetadata): List<AppointmentData>? {
    val appointments: MutableList<AppointmentData> = mutableListOf()
    try {
      val query = entityManager.createNativeQuery(
        this.queryLoader.loadQuery("ndmis-appointment-report.sql"),
      )
      query.setParameter("referralId", referralData.referralId)
      @Suppress("UNCHECKED_CAST")
      val results = query.resultList as List<Array<Any>>
      results.map { row ->
        appointments.add(
          AppointmentData(
            referralReference = row[0] as String,
            referralId = row[1] as UUID,
            appointmentId = row[2] as UUID,
            appointmentTime = NdmisDateTime(instantToOffsetNotNull(row[3] as Instant)),
            durationInMinutes = row[4] as Int,
            bookedAt = NdmisDateTime(instantToOffsetNotNull(row[5] as Instant)),
            attended = (row[6] as String?)?.let { Attended.valueOf(row[6] as String) },
            attendanceSubmittedAt = instantToOffsetNull(row[7] as Instant?)?.let { NdmisDateTime(it) },
            notifyPPOfAttendanceBehaviour = row[8] as Boolean?,
            deliusAppointmentId = (row[9] as? Long)?.toString() ?: "",
            reasonForAppointment = AppointmentReason.valueOf(row[10] as String),
          ),
        )
      }
    } catch (e: Exception) {
      logger.error("Error processing appointments for referral ${referralData.referralId}", e)
      return null
    }
    // Only return appointments if the list is not empty
    return appointments.ifEmpty { null }
  }

  private fun instantToOffsetNotNull(instant: Instant?): OffsetDateTime {
    val resolved = instant ?: Instant.now()
    return OffsetDateTime.ofInstant(resolved, ZoneId.systemDefault())
  }

  private fun instantToOffsetNull(instant: Instant?): OffsetDateTime? {
    val resolved = instant ?: Instant.now()
    return instant ?.let { OffsetDateTime.ofInstant(resolved, ZoneId.systemDefault()) }
  }
}
