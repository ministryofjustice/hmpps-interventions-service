package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.kotlin.firstValue
import org.mockito.kotlin.mock
import org.mockito.kotlin.same
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.ReferralEventPublisher
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.ReferralEventType
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Referral
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ActionPlanRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ReferralRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ActionPlanFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.EndOfServiceReportFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ReferralFactory
import java.time.OffsetDateTime

internal class ReferralConcluderTest {

  private val referralRepository: ReferralRepository = mock()
  private val actionPlanRepository: ActionPlanRepository = mock()
  private val referralEventPublisher: ReferralEventPublisher = mock()

  private val referralFactory = ReferralFactory()
  private val actionPlanFactory = ActionPlanFactory()
  private val endOfServiceReportFactory = EndOfServiceReportFactory()

  private val referralConcluder = ReferralConcluder(
    referralRepository, actionPlanRepository, referralEventPublisher
  )

  private fun createReferralWithSessions(totalSessions: Int, attendedOrLate: Int, didNotAttend: Int): Referral {
    assertThat(attendedOrLate + didNotAttend).isLessThanOrEqualTo(totalSessions)
      .withFailMessage("test setup error: the sum of attended and did not attend sessions must not be greater than the total sessions")

    val actionPlan = actionPlanFactory.create(numberOfSessions = totalSessions)
    val referral = referralFactory.createSent(actionPlans = mutableListOf(actionPlan))
    whenever(actionPlanRepository.countNumberOfAttendedSessions(referral.id)).thenReturn(attendedOrLate)
    whenever(actionPlanRepository.countNumberOfSessionsWithAttendanceRecord(referral.id)).thenReturn(attendedOrLate + didNotAttend)
    return referral
  }

  @Test
  fun `concludes referral as cancelled when ending a referral with no action plan`() {
    val timeAtStart = OffsetDateTime.now()
    val referralWithNoActionPlan = referralFactory.createSent()

    referralConcluder.concludeIfEligible(referralWithNoActionPlan)

    verifySaveWithConcludedAtSet(referralWithNoActionPlan, timeAtStart)
    verifyEventPublished(referralWithNoActionPlan, ReferralEventType.CANCELLED)
  }

  @Test
  fun `concludes referral as cancelled when ending a referral with no sessions with recorded attendances`() {
    val timeAtStart = OffsetDateTime.now()
    val referralWithActionPlanAndNoSessionsWithAttendanceRecord =
      createReferralWithSessions(totalSessions = 2, attendedOrLate = 0, didNotAttend = 0)

    referralConcluder.concludeIfEligible(referralWithActionPlanAndNoSessionsWithAttendanceRecord)

    verifySaveWithConcludedAtSet(referralWithActionPlanAndNoSessionsWithAttendanceRecord, timeAtStart)
    verifyEventPublished(referralWithActionPlanAndNoSessionsWithAttendanceRecord, ReferralEventType.CANCELLED)
  }

  @Test
  fun `concludes referral as cancelled when ending a referral without a substantive appointment (only not attended sessions)`() {
    val timeAtStart = OffsetDateTime.now()
    val referralWithActionPlanAndNoSessionsWithAttendanceRecord =
      createReferralWithSessions(totalSessions = 2, attendedOrLate = 0, didNotAttend = 2)

    referralConcluder.concludeIfEligible(referralWithActionPlanAndNoSessionsWithAttendanceRecord)

    verifySaveWithConcludedAtSet(referralWithActionPlanAndNoSessionsWithAttendanceRecord, timeAtStart)
    verifyEventPublished(referralWithActionPlanAndNoSessionsWithAttendanceRecord, ReferralEventType.CANCELLED)
  }

  @Test
  fun `concludes referral as prematurely ended when ending a referral with a substantive appointment and an end of service report submitted`() {
    val timeAtStart = OffsetDateTime.now()
    val referralWithActionPlanAndSomeSessionsWithAttendanceRecord =
      createReferralWithSessions(totalSessions = 2, attendedOrLate = 1, didNotAttend = 0)
    referralWithActionPlanAndSomeSessionsWithAttendanceRecord.endOfServiceReport = endOfServiceReportFactory.create(submittedAt = OffsetDateTime.now())

    referralConcluder.concludeIfEligible(referralWithActionPlanAndSomeSessionsWithAttendanceRecord)

    verifySaveWithConcludedAtSet(referralWithActionPlanAndSomeSessionsWithAttendanceRecord, timeAtStart)
    verifyEventPublished(referralWithActionPlanAndSomeSessionsWithAttendanceRecord, ReferralEventType.PREMATURELY_ENDED)
  }

  @Test
  fun `concludes referral as completed when ending a referral when all sessions have some kind of attendance and has an end service report submitted`() {
    val timeAtStart = OffsetDateTime.now()
    val referralWithActionPlanAndSomeSessionsWithAttendanceRecord =
      createReferralWithSessions(totalSessions = 2, attendedOrLate = 1, didNotAttend = 1)
    referralWithActionPlanAndSomeSessionsWithAttendanceRecord.endOfServiceReport = endOfServiceReportFactory.create(submittedAt = OffsetDateTime.now())

    referralConcluder.concludeIfEligible(referralWithActionPlanAndSomeSessionsWithAttendanceRecord)

    verifySaveWithConcludedAtSet(referralWithActionPlanAndSomeSessionsWithAttendanceRecord, timeAtStart)
    verifyEventPublished(referralWithActionPlanAndSomeSessionsWithAttendanceRecord, ReferralEventType.COMPLETED)
  }

  @Test
  fun `concludes referral as completed when ending a referral when all sessions have been attended and has an end service report submitted`() {
    val timeAtStart = OffsetDateTime.now()
    val referralWithActionPlanAndSomeSessionsWithAttendanceRecord =
      createReferralWithSessions(totalSessions = 2, attendedOrLate = 2, didNotAttend = 0)
    referralWithActionPlanAndSomeSessionsWithAttendanceRecord.endOfServiceReport = endOfServiceReportFactory.create(submittedAt = OffsetDateTime.now())

    referralConcluder.concludeIfEligible(referralWithActionPlanAndSomeSessionsWithAttendanceRecord)

    verifySaveWithConcludedAtSet(referralWithActionPlanAndSomeSessionsWithAttendanceRecord, timeAtStart)
    verifyEventPublished(referralWithActionPlanAndSomeSessionsWithAttendanceRecord, ReferralEventType.COMPLETED)
  }

  @Test
  fun `does not conclude a referral when ending a referral with some sessions with recorded attendances and an end of service report has not been submitted`() {
    val referralWithActionPlanAndSomeSessionsWithAttendanceRecord =
      createReferralWithSessions(totalSessions = 2, attendedOrLate = 1, didNotAttend = 0)
    referralWithActionPlanAndSomeSessionsWithAttendanceRecord.endOfServiceReport = endOfServiceReportFactory.create(submittedAt = null)

    referralConcluder.concludeIfEligible(referralWithActionPlanAndSomeSessionsWithAttendanceRecord)

    verifyNoInteractions(referralRepository, referralEventPublisher)
  }

  @Test
  fun `does not conclude a referral when ending a referral with all sessions with recorded attendances and an end of service report has not been submitted`() {
    val referralWithActionPlanAndSomeSessionsWithAttendanceRecord =
      createReferralWithSessions(totalSessions = 2, attendedOrLate = 1, didNotAttend = 1)
    referralWithActionPlanAndSomeSessionsWithAttendanceRecord.endOfServiceReport = endOfServiceReportFactory.create(submittedAt = null)

    referralConcluder.concludeIfEligible(referralWithActionPlanAndSomeSessionsWithAttendanceRecord)

    verifyNoInteractions(referralRepository, referralEventPublisher)
  }

  @Test
  fun `does not conclude a referral when ending a referral with some sessions with recorded attendances and an end of service report does not exist`() {
    val referralWithActionPlanAndSomeSessionsWithAttendanceRecord =
      createReferralWithSessions(totalSessions = 2, attendedOrLate = 1, didNotAttend = 0)

    referralConcluder.concludeIfEligible(referralWithActionPlanAndSomeSessionsWithAttendanceRecord)

    verifyNoInteractions(referralRepository, referralEventPublisher)
  }

  @Test
  fun `does not conclude a referral when ending a referral with all sessions with recorded attendances and an end of service report does not exist`() {
    val referralWithActionPlanAndSomeSessionsWithAttendanceRecord =
      createReferralWithSessions(totalSessions = 2, attendedOrLate = 1, didNotAttend = 1)

    referralConcluder.concludeIfEligible(referralWithActionPlanAndSomeSessionsWithAttendanceRecord)

    verifyNoInteractions(referralRepository, referralEventPublisher)
  }

  @Test
  fun `should not flag end of service report as required when one has already been created`() {

    val actionPlan = actionPlanFactory.create(numberOfSessions = 2)
    val endOfServiceReport = endOfServiceReportFactory.create()
    val referralWithActionPlanAndSomeSessionsWithAttendanceRecord = referralFactory.createEnded(actionPlans = mutableListOf(actionPlan), endOfServiceReport = endOfServiceReport)
    whenever(actionPlanRepository.countNumberOfAttendedSessions(referralWithActionPlanAndSomeSessionsWithAttendanceRecord.id)).thenReturn(1)

    val endOfServiceReportCreationRequired = referralConcluder.requiresEndOfServiceReportCreation(referralWithActionPlanAndSomeSessionsWithAttendanceRecord)

    assertThat(endOfServiceReportCreationRequired).isFalse
    verifyNoInteractions(referralRepository, referralEventPublisher)
  }

  @Test
  fun `should flag end of service report as required if it doesn't exist and when all sessions have been attended`() {

    val actionPlan = actionPlanFactory.create(numberOfSessions = 2)
    val referralWithActionPlanAndSomeSessionsWithAttendanceRecord = referralFactory.createEnded(actionPlans = mutableListOf(actionPlan), endOfServiceReport = null)
    whenever(actionPlanRepository.countNumberOfAttendedSessions(referralWithActionPlanAndSomeSessionsWithAttendanceRecord.id)).thenReturn(2)

    val endOfServiceReportCreationRequired = referralConcluder.requiresEndOfServiceReportCreation(referralWithActionPlanAndSomeSessionsWithAttendanceRecord)

    assertThat(endOfServiceReportCreationRequired).isTrue
    verifyNoInteractions(referralRepository, referralEventPublisher)
  }

  @Test
  fun `should flag end of service report as required if it doesn't exist and when at least one session has been attended and end has been requested`() {

    val actionPlan = actionPlanFactory.create(numberOfSessions = 2)
    val referralWithActionPlanAndSomeSessionsWithAttendanceRecord = referralFactory.createEnded(actionPlans = mutableListOf(actionPlan), endOfServiceReport = null)
    whenever(actionPlanRepository.countNumberOfAttendedSessions(referralWithActionPlanAndSomeSessionsWithAttendanceRecord.id)).thenReturn(1)

    val endOfServiceReportCreationRequired = referralConcluder.requiresEndOfServiceReportCreation(referralWithActionPlanAndSomeSessionsWithAttendanceRecord)

    assertThat(endOfServiceReportCreationRequired).isTrue
    verifyNoInteractions(referralRepository, referralEventPublisher)
  }

  @Test
  fun `should not flag end of service report as required when action plan is missing`() {

    val referralWithActionPlanAndSomeSessionsWithAttendanceRecord = referralFactory.createSent(actionPlans = null)

    val endOfServiceReportCreationRequired = referralConcluder.requiresEndOfServiceReportCreation(referralWithActionPlanAndSomeSessionsWithAttendanceRecord)

    assertThat(endOfServiceReportCreationRequired).isFalse
    verifyNoInteractions(actionPlanRepository, referralRepository, referralEventPublisher)
  }

  @Test
  fun `should not flag end of service report as required when no sessions have been attended`() {

    val actionPlan = actionPlanFactory.create(numberOfSessions = 2)
    val referralWithActionPlanAndSomeSessionsWithAttendanceRecord = referralFactory.createSent(actionPlans = mutableListOf(actionPlan))
    whenever(actionPlanRepository.countNumberOfAttendedSessions(actionPlan.id)).thenReturn(0)

    val endOfServiceReportCreationRequired = referralConcluder.requiresEndOfServiceReportCreation(referralWithActionPlanAndSomeSessionsWithAttendanceRecord)

    assertThat(endOfServiceReportCreationRequired).isFalse
    verifyNoInteractions(referralRepository, referralEventPublisher)
  }

  @Test
  fun `should not flag end of service report as required when having only non-attended sessions`() {

    val actionPlan = actionPlanFactory.create(numberOfSessions = 2)
    val referralWithActionPlanAndSomeSessionsWithAttendanceRecord = referralFactory.createSent(actionPlans = mutableListOf(actionPlan))
    whenever(actionPlanRepository.countNumberOfAttendedSessions(actionPlan.id)).thenReturn(0)
    whenever(actionPlanRepository.countNumberOfSessionsWithAttendanceRecord(actionPlan.id)).thenReturn(1)

    val endOfServiceReportCreationRequired = referralConcluder.requiresEndOfServiceReportCreation(referralWithActionPlanAndSomeSessionsWithAttendanceRecord)

    assertThat(endOfServiceReportCreationRequired).isFalse
    verifyNoInteractions(referralRepository, referralEventPublisher)
  }

  private fun verifyEventPublished(referralWithNoActionPlan: Referral, value: ReferralEventType) {
    verify(referralEventPublisher).referralConcludedEvent(same(referralWithNoActionPlan), same(value))
  }

  private fun verifySaveWithConcludedAtSet(referralWithNoActionPlan: Referral, timeAtStart: OffsetDateTime?) {
    val argumentCaptor: ArgumentCaptor<Referral> = ArgumentCaptor.forClass(Referral::class.java)
    verify(referralRepository).save(argumentCaptor.capture())
    assertThat(argumentCaptor.firstValue).isSameAs(referralWithNoActionPlan)
    assertThat(argumentCaptor.firstValue.concludedAt).isAfter(timeAtStart)
  }
}
