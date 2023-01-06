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
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.DeliverySessionRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ReferralRepository
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
  private val deliverySessionRepository: DeliverySessionRepository = mock()
  private val referralEventPublisher: ReferralEventPublisher = mock()

  private val referralFactory = ReferralFactory()
  private val actionPlanFactory = ActionPlanFactory()
  private val endOfServiceReportFactory = EndOfServiceReportFactory()

  private val concluder = ReferralConcluder(
    referralRepository, deliverySessionRepository, referralEventPublisher
  )

  data class WhenInState(val attendedOrLate: Int, val notAttended: Int, val notStarted: Int)
  data class ExpectThat(
    val requiresEoSR: Boolean,
    val endingWith: ReferralConcludedState? = null,
    val concludesWith: ReferralConcludedState? = null,
  )
  data class Example(
    val state: WhenInState,
    val beforeCancel: ExpectThat,
    val afterCancel: ExpectThat,
    val afterEoSR: ExpectThat? = null,
  )

  companion object {
    @JvmStatic
    fun examples(): Stream<Arguments> = Stream.of(
      arguments(
        named(
          "no sessions have any attendance",
          Example(
            state = WhenInState(attendedOrLate = 0, notAttended = 0, notStarted = 1),
            beforeCancel = ExpectThat(requiresEoSR = false),
            afterCancel = ExpectThat(requiresEoSR = false, endingWith = CANCELLED, concludesWith = CANCELLED),
          )
        )
      ),
      arguments(
        named(
          "finished delivery, without first substantive appointment (FSA)",
          Example(
            state = WhenInState(attendedOrLate = 0, notAttended = 1, notStarted = 0),
            beforeCancel = ExpectThat(requiresEoSR = false),
            afterCancel = ExpectThat(requiresEoSR = false, endingWith = CANCELLED, concludesWith = CANCELLED),
          )
        )
      ),
      arguments(
        named(
          "in-progress delivery, without first substantive appointment (FSA)",
          Example(
            state = WhenInState(attendedOrLate = 0, notAttended = 1, notStarted = 1),
            beforeCancel = ExpectThat(requiresEoSR = false),
            afterCancel = ExpectThat(requiresEoSR = false, endingWith = CANCELLED, concludesWith = CANCELLED),
          )
        )
      ),
      arguments(
        named(
          "in-progress delivery, with first substantive appointment (FSA)",
          Example(
            state = WhenInState(attendedOrLate = 1, notAttended = 0, notStarted = 1),
            beforeCancel = ExpectThat(requiresEoSR = false),
            afterCancel = ExpectThat(requiresEoSR = true, endingWith = PREMATURELY_ENDED),
            afterEoSR = ExpectThat(requiresEoSR = false, endingWith = PREMATURELY_ENDED, concludesWith = PREMATURELY_ENDED),
          )
        )
      ),
      arguments(
        named(
          "finished delivery, some missed sessions, with first substantive appointment (FSA)",
          Example(
            state = WhenInState(attendedOrLate = 1, notAttended = 1, notStarted = 0),
            beforeCancel = ExpectThat(requiresEoSR = true),
            afterCancel = ExpectThat(requiresEoSR = true, endingWith = COMPLETED),
            afterEoSR = ExpectThat(requiresEoSR = false, endingWith = COMPLETED, concludesWith = COMPLETED),
          )
        )
      ),
      arguments(
        named(
          "finished delivery, fully attended delivery",
          Example(
            state = WhenInState(attendedOrLate = 2, notAttended = 0, notStarted = 0),
            beforeCancel = ExpectThat(requiresEoSR = true),
            afterCancel = ExpectThat(requiresEoSR = true, endingWith = COMPLETED),
            afterEoSR = ExpectThat(requiresEoSR = false, endingWith = COMPLETED, concludesWith = COMPLETED),
          )
        )
      ),
    )
  }

  private fun createReferralWithSessions(state: WhenInState): Referral {
    val totalSessions = state.attendedOrLate + state.notAttended + state.notStarted

    val actionPlan = actionPlanFactory.createApproved(numberOfSessions = totalSessions)
    val referral = referralFactory.createSent(actionPlans = mutableListOf(actionPlan))
    whenever(deliverySessionRepository.countNumberOfAttendedSessions(referral.id))
      .thenReturn(state.attendedOrLate)
    whenever(deliverySessionRepository.countNumberOfSessionsWithAttendanceRecord(referral.id))
      .thenReturn(state.attendedOrLate + state.notAttended)
    return referral
  }

  @ParameterizedTest(name = "{displayName} {index}: {0}")
  @MethodSource("examples")
  fun `in-progress referral EoSR check`(example: Example) {
    val referral = createReferralWithSessions(example.state)

    assertThat(concluder.requiresEndOfServiceReportCreation(referral)).isEqualTo(example.beforeCancel.requiresEoSR)
  }

  @ParameterizedTest(name = "{displayName} {index}: {0}")
  @MethodSource("examples")
  fun `cancelled referrals EoSR check`(example: Example) {
    val referral = createReferralWithSessions(example.state)
    // not great -- this should be a service for atomic operation
    referral.endRequestedAt = OffsetDateTime.now()
    referral.endRequestedBy = AuthUser.interventionsServiceUser
    referral.endRequestedReason = CancellationReason("TST", "Test")

    assertThat(concluder.requiresEndOfServiceReportCreation(referral)).isEqualTo(example.afterCancel.requiresEoSR)
  }

  @ParameterizedTest(name = "{displayName} {index}: {0}")
  @MethodSource("examples")
  fun `cancelled referrals event check`(example: Example) {
    val referral = createReferralWithSessions(example.state)
    // not great -- this should be a service for atomic operation
    referral.endRequestedAt = OffsetDateTime.now()
    referral.endRequestedBy = AuthUser.interventionsServiceUser
    referral.endRequestedReason = CancellationReason("TST", "Test")

    concluder.concludeIfEligible(referral)
    verifyEndingWith(referral, example.afterCancel.endingWith)
    verifyConcludesWith(referral, example.afterCancel.concludesWith)
  }

  @ParameterizedTest(name = "{displayName} {index}: {0}")
  @MethodSource("examples")
  fun `referrals with submitted end-of-service reports`(example: Example) {
    if (example.afterEoSR == null) {
      return
    }

    val referral = createReferralWithSessions(example.state)
    referral.endOfServiceReport = endOfServiceReportFactory.create(referral = referral, submittedAt = OffsetDateTime.now())

    assertThat(concluder.requiresEndOfServiceReportCreation(referral)).isEqualTo(example.afterEoSR.requiresEoSR)
    concluder.concludeIfEligible(referral)
    verifyEndingWith(referral, example.afterEoSR.endingWith)
    verifyConcludesWith(referral, example.afterEoSR.concludesWith)
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
    }
    if (concludesWith != null) {
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
