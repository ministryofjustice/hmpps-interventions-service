package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.Named.named
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.ArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.firstValue
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.same
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.ReferralEventPublisher
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AuthUser
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.CancellationReason
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Referral
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ActionPlanRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.DeliverySessionRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ReferralRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.findLatestApprovedActionPlan
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.ReferralConcludedState.CANCELLED
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.ReferralConcludedState.COMPLETED
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.ReferralConcludedState.PREMATURELY_ENDED
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ActionPlanFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.EndOfServiceReportFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ReferralFactory
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import java.util.stream.Stream

internal class ReferralConcluderTest {
  private val referralRepository: ReferralRepository = mock()
  private val actionPlanRepository: ActionPlanRepository = mock()
  private val deliverySessionRepository: DeliverySessionRepository = mock()
  private val referralEventPublisher: ReferralEventPublisher = mock()

  private val referralFactory = ReferralFactory()
  private val actionPlanFactory = ActionPlanFactory()
  private val endOfServiceReportFactory = EndOfServiceReportFactory()

  private val concluder = ReferralConcluder(
    referralRepository, deliverySessionRepository, actionPlanRepository, referralEventPublisher
  )

  data class WhenInState(val attendedOrLate: Int, val notAttended: Int, val withoutOutcome: Int)
  data class ExpectThat(
    val canStartEoSR: Boolean,
    val endingWith: ReferralConcludedState? = null,
    val concludesWith: ReferralConcludedState? = null,
  )

  companion object {
    private val notStarted = named(
      "no sessions have any attendance",
      WhenInState(attendedOrLate = 0, notAttended = 0, withoutOutcome = 1)
    )
    private val allSessionsHaveOutcomeNoFSA = named(
      "finished delivery, without first substantive appointment (FSA)",
      WhenInState(attendedOrLate = 0, notAttended = 1, withoutOutcome = 0)
    )
    private val inProgressNoFSA = named(
      "in-progress delivery, without first substantive appointment (FSA)",
      WhenInState(attendedOrLate = 0, notAttended = 1, withoutOutcome = 1)
    )
    private val inProgressWithFSA = named(
      "in-progress delivery, with first substantive appointment (FSA)",
      WhenInState(attendedOrLate = 1, notAttended = 0, withoutOutcome = 1)
    )
    private val allSessionsHaveOutcomeSomeDNAWithFSA = named(
      "finished delivery, some missed/did not attend (DNA) sessions, with first substantive appointment (FSA)",
      WhenInState(attendedOrLate = 1, notAttended = 1, withoutOutcome = 0)
    )
    private val allSessionsHaveOutcomeAllAttendedWithFSA = named(
      "finished delivery, fully attended delivery",
      WhenInState(attendedOrLate = 2, notAttended = 0, withoutOutcome = 0)
    )

    @JvmStatic
    fun inProgressExamples(): Stream<Arguments> = Stream.of(
      arguments(notStarted, ExpectThat(canStartEoSR = false)),
      arguments(allSessionsHaveOutcomeNoFSA, ExpectThat(canStartEoSR = false)),
      arguments(inProgressNoFSA, ExpectThat(canStartEoSR = false)),
      arguments(inProgressWithFSA, ExpectThat(canStartEoSR = false)),
      arguments(allSessionsHaveOutcomeSomeDNAWithFSA, ExpectThat(canStartEoSR = true)),
      arguments(allSessionsHaveOutcomeAllAttendedWithFSA, ExpectThat(canStartEoSR = true)),
    )

    @JvmStatic
    fun afterCancelExamples(): Stream<Arguments> = Stream.of(
      arguments(notStarted, ExpectThat(canStartEoSR = false, endingWith = CANCELLED, concludesWith = CANCELLED)),
      arguments(
        allSessionsHaveOutcomeNoFSA,
        ExpectThat(canStartEoSR = false, endingWith = CANCELLED, concludesWith = CANCELLED)
      ),
      arguments(inProgressNoFSA, ExpectThat(canStartEoSR = false, endingWith = CANCELLED, concludesWith = CANCELLED)),
      arguments(inProgressWithFSA, ExpectThat(canStartEoSR = true, endingWith = PREMATURELY_ENDED)),
      arguments(allSessionsHaveOutcomeSomeDNAWithFSA, ExpectThat(canStartEoSR = true, endingWith = COMPLETED)),
      arguments(allSessionsHaveOutcomeAllAttendedWithFSA, ExpectThat(canStartEoSR = true, endingWith = COMPLETED)),
    )

    @JvmStatic
    fun afterEoSRSubmittedExamples(): Stream<Arguments> = Stream.of(
      arguments(
        inProgressWithFSA,
        ExpectThat(canStartEoSR = false, endingWith = PREMATURELY_ENDED, concludesWith = PREMATURELY_ENDED)
      ),
      arguments(
        allSessionsHaveOutcomeSomeDNAWithFSA,
        ExpectThat(canStartEoSR = false, endingWith = COMPLETED, concludesWith = COMPLETED)
      ),
      arguments(
        allSessionsHaveOutcomeAllAttendedWithFSA,
        ExpectThat(canStartEoSR = false, endingWith = COMPLETED, concludesWith = COMPLETED)
      ),
    )
  }

  private fun createReferralWithSessions(state: WhenInState): Referral {
    val totalSessions = state.attendedOrLate + state.notAttended + state.withoutOutcome

    val actionPlan = actionPlanFactory.createApproved(numberOfSessions = totalSessions)
    val referral = referralFactory.createSent(actionPlans = mutableListOf(actionPlan))
    whenever(deliverySessionRepository.countNumberOfAttendedSessions(referral.id))
      .thenReturn(state.attendedOrLate)
    whenever(deliverySessionRepository.countNumberOfSessionsWithAttendanceRecord(referral.id))
      .thenReturn(state.attendedOrLate + state.notAttended)
    whenever(actionPlanRepository.findAllByReferralIdAndApprovedAtIsNotNull(referral.id))
      .thenReturn(referral.actionPlans)
    whenever(actionPlanRepository.findLatestApprovedActionPlan(referral.id))
      .thenReturn(actionPlan)
    return referral
  }

  @ParameterizedTest
  @MethodSource("inProgressExamples")
  fun `in-progress referral EoSR check`(state: WhenInState, expectation: ExpectThat) {
    val referral = createReferralWithSessions(state)

    assertThat(concluder.requiresEndOfServiceReportCreation(referral))
      .describedAs("requires end-of-service report")
      .isEqualTo(expectation.canStartEoSR)
  }

  @ParameterizedTest
  @MethodSource("afterCancelExamples")
  fun `cancelled referrals EoSR check`(state: WhenInState, expectation: ExpectThat) {
    val referral = createReferralWithSessions(state)
    // not great -- this should be a service for atomic operation
    referral.endRequestedAt = OffsetDateTime.now()
    referral.endRequestedBy = AuthUser.interventionsServiceUser
    referral.endRequestedReason = CancellationReason("TST", "Test")

    assertThat(concluder.requiresEndOfServiceReportCreation(referral))
      .describedAs("requires end-of-service report")
      .isEqualTo(expectation.canStartEoSR)
  }

  @ParameterizedTest
  @MethodSource("afterCancelExamples")
  fun `cancelled referrals event check`(state: WhenInState, expectation: ExpectThat) {
    val referral = createReferralWithSessions(state)
    // not great -- this should be a service for atomic operation
    referral.endRequestedAt = OffsetDateTime.now()
    referral.endRequestedBy = AuthUser.interventionsServiceUser
    referral.endRequestedReason = CancellationReason("TST", "Test")

    concluder.concludeIfEligible(referral)
    verifyEndingWith(referral, expectation.endingWith)
    verifyConcludesWith(referral, expectation.concludesWith)
  }

  @ParameterizedTest
  @MethodSource("afterEoSRSubmittedExamples")
  fun `referrals with submitted end-of-service reports`(state: WhenInState, expectation: ExpectThat) {
    val referral = createReferralWithSessions(state)
    referral.endOfServiceReport =
      endOfServiceReportFactory.create(referral = referral, submittedAt = OffsetDateTime.now())

    assertThat(concluder.requiresEndOfServiceReportCreation(referral))
      .describedAs("requires end-of-service report")
      .isEqualTo(expectation.canStartEoSR)

    concluder.concludeIfEligible(referral)
    verifyEndingWith(referral, expectation.endingWith)
    verifyConcludesWith(referral, expectation.concludesWith)
  }

  private fun verifyEndingWith(referral: Referral, endingWith: ReferralConcludedState?) {
    if (endingWith != null) {
      verify(referralEventPublisher).referralEndingEvent(same(referral), eq(endingWith))
    } else {
      verify(referralEventPublisher, never()).referralEndingEvent(same(referral), any())
    }
  }

  private fun verifyConcludesWith(referral: Referral, concludesWith: ReferralConcludedState?) {
    if (concludesWith != null) {
      verifySaveWithConcludedAtSet(referral)
      verify(referralEventPublisher).referralConcludedEvent(same(referral), eq(concludesWith))
    } else {
      verify(referralEventPublisher, never()).referralConcludedEvent(same(referral), any())
    }
  }

  private fun verifySaveWithConcludedAtSet(referral: Referral) {
    val argumentCaptor: ArgumentCaptor<Referral> = ArgumentCaptor.forClass(Referral::class.java)
    verify(referralRepository).save(argumentCaptor.capture())
    assertThat(argumentCaptor.firstValue).isSameAs(referral)
    assertThat(argumentCaptor.firstValue.concludedAt).isCloseToUtcNow(within(1, ChronoUnit.MINUTES))
  }
}
