package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.DeliverySession
import java.util.UUID

interface DeliverySessionRepository : JpaRepository<DeliverySession, UUID> {
  fun findAllByReferralId(referralId: UUID): List<DeliverySession>
  fun findByReferralIdAndSessionNumber(referralId: UUID, sessionNumber: Int): DeliverySession?

  @Deprecated(
    "looking up by action plan ID is no longer necessary",
    ReplaceWith("findByReferralIdAndSessionNumber(referralId, sessionNumber"),
  )
  @Query("select aps from DeliverySession aps left join ActionPlan ap on ap.referral = aps.referral where ap.id = :actionPlanId and aps.sessionNumber = :sessionNumber")
  fun findAllByActionPlanIdAndSessionNumber(actionPlanId: UUID, sessionNumber: Int): DeliverySession?

  @Deprecated(
    "looking up by action plan ID is no longer necessary",
    ReplaceWith("findAllByReferralId(referralId"),
  )
  @Query("select ds from DeliverySession ds left join ActionPlan ap on ap.referral = ds.referral where ap.id = :actionPlanId")
  fun findAllByActionPlanId(actionPlanId: UUID): List<DeliverySession>

  @Query(
    "select count(sesh) from DeliverySession sesh join sesh.appointments appt " +
      "where sesh.referral.id = :referralId and appt.attended is not null " +
      "and appt.appointmentFeedbackSubmittedAt is not null and appt.superseded = false and appt.stale = false",
  )
  fun countNumberOfSessionsWithAttendanceRecord(referralId: UUID): Int

  @Query(
    "select count(sesh) from DeliverySession sesh join sesh.appointments appt " +
      "where sesh.referral.id = :referralId and appt.attended in ('YES', 'LATE') " +
      "and appt.appointmentFeedbackSubmittedAt is not null and appt.stale = false",
  )
  fun countNumberOfAttendedSessions(referralId: UUID): Int

  @Query(
    "select count(sesh) from DeliverySession sesh join sesh.appointments appt " +
      "where sesh.referral.id = :referralId and appt.attended in ('NO') " +
      "and appt.appointmentFeedbackSubmittedAt is not null and appt.stale = false",
  )
  fun countNumberOfNotAttendedSessions(referralId: UUID): Int
}
