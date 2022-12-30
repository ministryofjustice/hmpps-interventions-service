package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.firstValue
import org.mockito.kotlin.mock
import org.mockito.kotlin.same
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.ReferralEventPublisher
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Referral
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.DeliverySessionRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ReferralRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ActionPlanFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.EndOfServiceReportFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ReferralFactory
import java.time.OffsetDateTime

internal class ReferralConcluderTest {
  private val referralRepository: ReferralRepository = mock()
  private val deliverySessionRepository: DeliverySessionRepository = mock()
  private val referralEventPublisher: ReferralEventPublisher = mock()

  private val referralFactory = ReferralFactory()
  private val actionPlanFactory = ActionPlanFactory()
  private val endOfServiceReportFactory = EndOfServiceReportFactory()

  private val referralConcluder = ReferralConcluder(
    referralRepository, deliverySessionRepository, referralEventPublisher
  )

  private fun createReferralWithSessions(totalSessions: Int, attendedOrLate: Int, didNotAttend: Int): Referral {
    assertThat(attendedOrLate + didNotAttend).isLessThanOrEqualTo(totalSessions)
      .withFailMessage("test setup error: the sum of attended and did not attend sessions must not be greater than the total sessions")

    val actionPlan = actionPlanFactory.createApproved(numberOfSessions = totalSessions)
    val referral = referralFactory.createSent(actionPlans = mutableListOf(actionPlan))
    whenever(deliverySessionRepository.countNumberOfAttendedSessions(referral.id)).thenReturn(attendedOrLate)
    whenever(deliverySessionRepository.countNumberOfSessionsWithAttendanceRecord(referral.id)).thenReturn(attendedOrLate + didNotAttend)
    return referral
  }

  @Test
  fun `signals end, concludes referral as cancelled without an action plan`() {
    val timeAtStart = OffsetDateTime.now()
    val referral = referralFactory.createSent()

    referralConcluder.concludeIfEligible(referral)

    verifySaveWithConcludedAtSet(referral, timeAtStart)
    verifyEndingEventPublished(referral, ReferralConcludedState.CANCELLED)
    verifyConcludedEventPublished(referral, ReferralConcludedState.CANCELLED)
  }

  @Test
  fun `signals end, concludes referral as cancelled without any recorded attendances`() {
    val timeAtStart = OffsetDateTime.now()
    val referral = createReferralWithSessions(totalSessions = 2, attendedOrLate = 0, didNotAttend = 0)

    referralConcluder.concludeIfEligible(referral)

    verifySaveWithConcludedAtSet(referral, timeAtStart)
    verifyEndingEventPublished(referral, ReferralConcludedState.CANCELLED)
    verifyConcludedEventPublished(referral, ReferralConcludedState.CANCELLED)
  }

  @Test
  fun `signals end, concludes a referral as cancelled without substantive delivery (only did not attend sessions)`() {
    val timeAtStart = OffsetDateTime.now()
    val referral = createReferralWithSessions(totalSessions = 2, attendedOrLate = 0, didNotAttend = 2)

    referralConcluder.concludeIfEligible(referral)

    verifySaveWithConcludedAtSet(referral, timeAtStart)
    verifyEndingEventPublished(referral, ReferralConcludedState.CANCELLED)
    verifyConcludedEventPublished(referral, ReferralConcludedState.CANCELLED)
  }

  @Test
  fun `signals end, concludes a referral as prematurely ended with substantive delivery, partial delivery, submitted end-of-service report`() {
    val timeAtStart = OffsetDateTime.now()
    val referral = createReferralWithSessions(totalSessions = 2, attendedOrLate = 1, didNotAttend = 0).also {
      it.endOfServiceReport = endOfServiceReportFactory.create(submittedAt = OffsetDateTime.now())
    }

    referralConcluder.concludeIfEligible(referral)

    verifySaveWithConcludedAtSet(referral, timeAtStart)
    verifyEndingEventPublished(referral, ReferralConcludedState.PREMATURELY_ENDED)
    verifyConcludedEventPublished(referral, ReferralConcludedState.PREMATURELY_ENDED)
  }

  @Test
  fun `signals end, concludes a referral as completed with substantive delivery, full delivery with no-show, submitted end-of-service report`() {
    val timeAtStart = OffsetDateTime.now()
    val referral = createReferralWithSessions(totalSessions = 2, attendedOrLate = 1, didNotAttend = 1).also {
      it.endOfServiceReport = endOfServiceReportFactory.create(submittedAt = OffsetDateTime.now())
    }

    referralConcluder.concludeIfEligible(referral)

    verifySaveWithConcludedAtSet(referral, timeAtStart)
    verifyEndingEventPublished(referral, ReferralConcludedState.COMPLETED)
    verifyConcludedEventPublished(referral, ReferralConcludedState.COMPLETED)
  }

  @Test
  fun `signals end, concludes a referral as completed with substantive delivery, full delivery, submitted end-of-service report`() {
    val timeAtStart = OffsetDateTime.now()
    val referral = createReferralWithSessions(totalSessions = 2, attendedOrLate = 2, didNotAttend = 0).also {
      it.endOfServiceReport = endOfServiceReportFactory.create(submittedAt = OffsetDateTime.now())
    }

    referralConcluder.concludeIfEligible(referral)

    verifySaveWithConcludedAtSet(referral, timeAtStart)
    verifyEndingEventPublished(referral, ReferralConcludedState.COMPLETED)
    verifyConcludedEventPublished(referral, ReferralConcludedState.COMPLETED)
  }

  @Test
  fun `signals end, concludes referral as completed when all sessions are attended, even if there is an action plan revision with more sessions`() {
    val timeAtStart = OffsetDateTime.now()
    val referral = createReferralWithSessions(totalSessions = 2, attendedOrLate = 2, didNotAttend = 0).also {
      it.endOfServiceReport = endOfServiceReportFactory.create(submittedAt = OffsetDateTime.now())
      it.actionPlans?.add(actionPlanFactory.createSubmitted(numberOfSessions = 10, referral = it))
    }

    referralConcluder.concludeIfEligible(referral)

    verifySaveWithConcludedAtSet(referral, timeAtStart)
    verifyEndingEventPublished(referral, ReferralConcludedState.COMPLETED)
    verifyConcludedEventPublished(referral, ReferralConcludedState.COMPLETED)
  }

  @Test
  fun `signals end, does not conclude a referral with substantive delivery, partial delivery, started end-of-service report`() {
    val referral = createReferralWithSessions(totalSessions = 2, attendedOrLate = 1, didNotAttend = 0).also {
      it.endOfServiceReport = endOfServiceReportFactory.create(submittedAt = null)
    }

    referralConcluder.concludeIfEligible(referral)

    verifyNoInteractions(referralRepository)
    verifyEndingEventPublished(referral, ReferralConcludedState.PREMATURELY_ENDED)
    verifyNoMoreInteractions(referralEventPublisher)
  }

  @Test
  fun `signals end, does not conclude a referral with substantive delivery, full delivery with no-show, started end-of-service report`() {
    val referral = createReferralWithSessions(totalSessions = 2, attendedOrLate = 1, didNotAttend = 1).also {
      it.endOfServiceReport = endOfServiceReportFactory.create(submittedAt = null)
    }

    referralConcluder.concludeIfEligible(referral)

    verifyNoInteractions(referralRepository)
    verifyEndingEventPublished(referral, ReferralConcludedState.COMPLETED)
    verifyNoMoreInteractions(referralEventPublisher)
  }

  @Test
  fun `signals end, does not conclude a referral with substantive delivery, partial delivery, missing end-of-service report`() {
    val referral = createReferralWithSessions(totalSessions = 2, attendedOrLate = 1, didNotAttend = 0)

    referralConcluder.concludeIfEligible(referral)

    verifyNoInteractions(referralRepository)
    verifyEndingEventPublished(referral, ReferralConcludedState.PREMATURELY_ENDED)
    verifyNoMoreInteractions(referralEventPublisher)
  }

  @Test
  fun `signals end, does not conclude a referral with substantive delivery, full delivery, missing end-of-service report`() {
    val referral = createReferralWithSessions(totalSessions = 2, attendedOrLate = 1, didNotAttend = 1)

    referralConcluder.concludeIfEligible(referral)

    verifyNoInteractions(referralRepository)
    verifyEndingEventPublished(referral, ReferralConcludedState.COMPLETED)
    verifyNoMoreInteractions(referralEventPublisher)
  }

  @Test
  fun `should not flag end of service report as required when one has already been created`() {
    val actionPlan = actionPlanFactory.createApproved(numberOfSessions = 2)
    val endOfServiceReport = endOfServiceReportFactory.create()
    val referralWithActionPlanAndSomeSessionsWithAttendanceRecord = referralFactory.createEnded(actionPlans = mutableListOf(actionPlan), endOfServiceReport = endOfServiceReport)
    whenever(deliverySessionRepository.countNumberOfAttendedSessions(referralWithActionPlanAndSomeSessionsWithAttendanceRecord.id)).thenReturn(1)

    val endOfServiceReportCreationRequired = referralConcluder.requiresEndOfServiceReportCreation(referralWithActionPlanAndSomeSessionsWithAttendanceRecord)

    assertThat(endOfServiceReportCreationRequired).isFalse
    verifyNoInteractions(referralRepository, referralEventPublisher)
  }

  @Test
  fun `should flag end of service report as required if it doesn't exist and when all sessions have been attended`() {
    val actionPlan = actionPlanFactory.createApproved(numberOfSessions = 2)
    val referralWithActionPlanAndSomeSessionsWithAttendanceRecord = referralFactory.createEnded(actionPlans = mutableListOf(actionPlan), endOfServiceReport = null)
    whenever(deliverySessionRepository.countNumberOfAttendedSessions(referralWithActionPlanAndSomeSessionsWithAttendanceRecord.id)).thenReturn(2)

    val endOfServiceReportCreationRequired = referralConcluder.requiresEndOfServiceReportCreation(referralWithActionPlanAndSomeSessionsWithAttendanceRecord)

    assertThat(endOfServiceReportCreationRequired).isTrue
    verifyNoInteractions(referralRepository, referralEventPublisher)
  }

  @Test
  fun `should flag end of service report as required if it doesn't exist and when at least one session has been attended and end has been requested`() {

    val actionPlan = actionPlanFactory.createApproved(numberOfSessions = 2)
    val referralWithActionPlanAndSomeSessionsWithAttendanceRecord = referralFactory.createEnded(actionPlans = mutableListOf(actionPlan), endOfServiceReport = null)
    whenever(deliverySessionRepository.countNumberOfAttendedSessions(referralWithActionPlanAndSomeSessionsWithAttendanceRecord.id)).thenReturn(1)

    val endOfServiceReportCreationRequired = referralConcluder.requiresEndOfServiceReportCreation(referralWithActionPlanAndSomeSessionsWithAttendanceRecord)

    assertThat(endOfServiceReportCreationRequired).isTrue
    verifyNoInteractions(referralRepository, referralEventPublisher)
  }

  @Test
  fun `should not flag end of service report as required when action plan is missing`() {
    val referralWithActionPlanAndSomeSessionsWithAttendanceRecord = referralFactory.createSent(actionPlans = null)

    val endOfServiceReportCreationRequired = referralConcluder.requiresEndOfServiceReportCreation(referralWithActionPlanAndSomeSessionsWithAttendanceRecord)

    assertThat(endOfServiceReportCreationRequired).isFalse
    verifyNoInteractions(deliverySessionRepository, referralRepository, referralEventPublisher)
  }

  @Test
  fun `should not flag end of service report as required when no sessions have been attended`() {
    val actionPlan = actionPlanFactory.createApproved(numberOfSessions = 2)
    val referralWithActionPlanAndSomeSessionsWithAttendanceRecord = referralFactory.createSent(actionPlans = mutableListOf(actionPlan))
    whenever(deliverySessionRepository.countNumberOfAttendedSessions(actionPlan.id)).thenReturn(0)

    val endOfServiceReportCreationRequired = referralConcluder.requiresEndOfServiceReportCreation(referralWithActionPlanAndSomeSessionsWithAttendanceRecord)

    assertThat(endOfServiceReportCreationRequired).isFalse
    verifyNoInteractions(referralRepository, referralEventPublisher)
  }

  @Test
  fun `should not flag end of service report as required when having only non-attended sessions`() {
    val actionPlan = actionPlanFactory.createApproved(numberOfSessions = 2)
    val referralWithActionPlanAndSomeSessionsWithAttendanceRecord = referralFactory.createSent(actionPlans = mutableListOf(actionPlan))
    whenever(deliverySessionRepository.countNumberOfAttendedSessions(actionPlan.id)).thenReturn(0)
    whenever(deliverySessionRepository.countNumberOfSessionsWithAttendanceRecord(actionPlan.id)).thenReturn(1)

    val endOfServiceReportCreationRequired = referralConcluder.requiresEndOfServiceReportCreation(referralWithActionPlanAndSomeSessionsWithAttendanceRecord)

    assertThat(endOfServiceReportCreationRequired).isFalse
    verifyNoInteractions(referralRepository, referralEventPublisher)
  }

  private fun verifyEndingEventPublished(referral: Referral, value: ReferralConcludedState) {
    verify(referralEventPublisher).referralEndingEvent(same(referral), eq(value))
  }

  private fun verifyConcludedEventPublished(referral: Referral, value: ReferralConcludedState) {
    verify(referralEventPublisher).referralConcludedEvent(same(referral), eq(value))
  }

  private fun verifySaveWithConcludedAtSet(referralWithNoActionPlan: Referral, timeAtStart: OffsetDateTime?) {
    val argumentCaptor: ArgumentCaptor<Referral> = ArgumentCaptor.forClass(Referral::class.java)
    verify(referralRepository).save(argumentCaptor.capture())
    assertThat(argumentCaptor.firstValue).isSameAs(referralWithNoActionPlan)
    assertThat(argumentCaptor.firstValue.concludedAt).isAfter(timeAtStart)
  }
}
