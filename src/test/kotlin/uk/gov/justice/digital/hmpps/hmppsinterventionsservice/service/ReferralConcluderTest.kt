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

  @Test
  fun `concludes referral as cancelled when ending a referral with no action plan`() {

    val timeAtStart = OffsetDateTime.now()
    val referralWithNoActionPlan = referralFactory.createSent()

    referralConcluder.concludeIfEligible(referralWithNoActionPlan)

    verifySaveWithConcludedAtSet(referralWithNoActionPlan, timeAtStart)
    verifyEventPublished(referralWithNoActionPlan, ReferralEventType.CANCELLED)
  }

  @Test
  fun `concludes referral as cancelled when ending a referral with no sessions attended`() {
    val timeAtStart = OffsetDateTime.now()
    val actionPlan = actionPlanFactory.create(numberOfSessions = 2)
    val referralWithActionPlanAndNoAttendedSessions = referralFactory.createSent(actionPlans = mutableListOf(actionPlan))
    whenever(actionPlanRepository.countNumberOfAttendedSessions(referralWithActionPlanAndNoAttendedSessions.id)).thenReturn(0)

    referralConcluder.concludeIfEligible(referralWithActionPlanAndNoAttendedSessions)

    verifySaveWithConcludedAtSet(referralWithActionPlanAndNoAttendedSessions, timeAtStart)
    verifyEventPublished(referralWithActionPlanAndNoAttendedSessions, ReferralEventType.CANCELLED)
  }

  @Test
  fun `concludes referral as prematurely ended when ending a referral with some sessions attended and an end of service report submitted`() {

    val timeAtStart = OffsetDateTime.now()
    val actionPlan = actionPlanFactory.create(numberOfSessions = 2)
    val referralWithActionPlanAndSomeAttendedSessions = referralFactory.createSent(actionPlans = mutableListOf(actionPlan))
    referralWithActionPlanAndSomeAttendedSessions.endOfServiceReport = endOfServiceReportFactory.create(submittedAt = OffsetDateTime.now())
    whenever(actionPlanRepository.countNumberOfAttendedSessions(referralWithActionPlanAndSomeAttendedSessions.id)).thenReturn(1)

    referralConcluder.concludeIfEligible(referralWithActionPlanAndSomeAttendedSessions)

    verifySaveWithConcludedAtSet(referralWithActionPlanAndSomeAttendedSessions, timeAtStart)
    verifyEventPublished(referralWithActionPlanAndSomeAttendedSessions, ReferralEventType.PREMATURELY_ENDED)
  }

  @Test
  fun `concludes referral as completed when ending a referral with all sessions attended and an end of service report submitted`() {

    val timeAtStart = OffsetDateTime.now()
    val actionPlan = actionPlanFactory.create(numberOfSessions = 2)
    val referralWithActionPlanAndSomeAttendedSessions = referralFactory.createSent(actionPlans = mutableListOf(actionPlan))
    referralWithActionPlanAndSomeAttendedSessions.endOfServiceReport = endOfServiceReportFactory.create(submittedAt = OffsetDateTime.now())
    whenever(actionPlanRepository.countNumberOfAttendedSessions(referralWithActionPlanAndSomeAttendedSessions.id)).thenReturn(2)

    referralConcluder.concludeIfEligible(referralWithActionPlanAndSomeAttendedSessions)

    verifySaveWithConcludedAtSet(referralWithActionPlanAndSomeAttendedSessions, timeAtStart)
    verifyEventPublished(referralWithActionPlanAndSomeAttendedSessions, ReferralEventType.COMPLETED)
  }

  @Test
  fun `does not conclude a referral when ending a referral with some sessions attended and an end of service report has not been submitted`() {

    val actionPlan = actionPlanFactory.create(numberOfSessions = 2)
    val referralWithActionPlanAndSomeAttendedSessions = referralFactory.createSent(actionPlans = mutableListOf(actionPlan))
    referralWithActionPlanAndSomeAttendedSessions.endOfServiceReport = endOfServiceReportFactory.create(submittedAt = null)
    whenever(actionPlanRepository.countNumberOfAttendedSessions(referralWithActionPlanAndSomeAttendedSessions.id)).thenReturn(1)

    referralConcluder.concludeIfEligible(referralWithActionPlanAndSomeAttendedSessions)

    verifyNoInteractions(referralRepository, referralEventPublisher)
  }

  @Test
  fun `does not conclude a referral when ending a referral with all sessions attended and an end of service report has not been submitted`() {

    val actionPlan = actionPlanFactory.create(numberOfSessions = 2)
    val referralWithActionPlanAndSomeAttendedSessions = referralFactory.createSent(actionPlans = mutableListOf(actionPlan))
    referralWithActionPlanAndSomeAttendedSessions.endOfServiceReport = endOfServiceReportFactory.create(submittedAt = null)
    whenever(actionPlanRepository.countNumberOfAttendedSessions(referralWithActionPlanAndSomeAttendedSessions.id)).thenReturn(2)

    referralConcluder.concludeIfEligible(referralWithActionPlanAndSomeAttendedSessions)

    verifyNoInteractions(referralRepository, referralEventPublisher)
  }

  @Test
  fun `does not conclude a referral when ending a referral with some sessions attended and an end of service report does not exist`() {

    val actionPlan = actionPlanFactory.create(numberOfSessions = 2)
    val referralWithActionPlanAndSomeAttendedSessions = referralFactory.createSent(actionPlans = mutableListOf(actionPlan))
    whenever(actionPlanRepository.countNumberOfAttendedSessions(referralWithActionPlanAndSomeAttendedSessions.id)).thenReturn(1)

    referralConcluder.concludeIfEligible(referralWithActionPlanAndSomeAttendedSessions)

    verifyNoInteractions(referralRepository, referralEventPublisher)
  }

  @Test
  fun `does not conclude a referral when ending a referral with all sessions attended and an end of service report does not exit`() {

    val actionPlan = actionPlanFactory.create(numberOfSessions = 2)
    val referralWithActionPlanAndSomeAttendedSessions = referralFactory.createSent(actionPlans = mutableListOf(actionPlan))
    whenever(actionPlanRepository.countNumberOfAttendedSessions(referralWithActionPlanAndSomeAttendedSessions.id)).thenReturn(2)

    referralConcluder.concludeIfEligible(referralWithActionPlanAndSomeAttendedSessions)

    verifyNoInteractions(referralRepository, referralEventPublisher)
  }

  @Test
  fun `should not flag end of service report as required when one has already been created`() {

    val actionPlan = actionPlanFactory.create(numberOfSessions = 2)
    val endOfServiceReport = endOfServiceReportFactory.create()
    val referralWithActionPlanAndSomeAttendedSessions = referralFactory.createEnded(actionPlans = mutableListOf(actionPlan), endOfServiceReport = endOfServiceReport)
    whenever(actionPlanRepository.countNumberOfAttendedSessions(referralWithActionPlanAndSomeAttendedSessions.id)).thenReturn(1)

    val endOfServiceReportCreationRequired = referralConcluder.requiresEndOfServiceReportCreation(referralWithActionPlanAndSomeAttendedSessions)

    assertThat(endOfServiceReportCreationRequired).isFalse
    verifyNoInteractions(referralRepository, referralEventPublisher)
  }

  @Test
  fun `should flag end of service report as required if it doesn't exist and when all sessions have been attended`() {

    val actionPlan = actionPlanFactory.create(numberOfSessions = 2)
    val referralWithActionPlanAndSomeAttendedSessions = referralFactory.createEnded(actionPlans = mutableListOf(actionPlan), endOfServiceReport = null)
    whenever(actionPlanRepository.countNumberOfAttendedSessions(referralWithActionPlanAndSomeAttendedSessions.id)).thenReturn(2)

    val endOfServiceReportCreationRequired = referralConcluder.requiresEndOfServiceReportCreation(referralWithActionPlanAndSomeAttendedSessions)

    assertThat(endOfServiceReportCreationRequired).isTrue
    verifyNoInteractions(referralRepository, referralEventPublisher)
  }

  @Test
  fun `should flag end of service report as required if it doesn't exist and when at least one session has been attended and end has been requested`() {

    val actionPlan = actionPlanFactory.create(numberOfSessions = 2)
    val referralWithActionPlanAndSomeAttendedSessions = referralFactory.createEnded(actionPlans = mutableListOf(actionPlan), endOfServiceReport = null)
    whenever(actionPlanRepository.countNumberOfAttendedSessions(referralWithActionPlanAndSomeAttendedSessions.id)).thenReturn(1)

    val endOfServiceReportCreationRequired = referralConcluder.requiresEndOfServiceReportCreation(referralWithActionPlanAndSomeAttendedSessions)

    assertThat(endOfServiceReportCreationRequired).isTrue
    verifyNoInteractions(referralRepository, referralEventPublisher)
  }

  @Test
  fun `should not flag end of service report as required when action plan exists`() {

    val referralWithActionPlanAndSomeAttendedSessions = referralFactory.createSent(actionPlans = null)

    val endOfServiceReportCreationRequired = referralConcluder.requiresEndOfServiceReportCreation(referralWithActionPlanAndSomeAttendedSessions)

    assertThat(endOfServiceReportCreationRequired).isFalse
    verifyNoInteractions(actionPlanRepository, referralRepository, referralEventPublisher)
  }

  @Test
  fun `should not flag end of service report as required when no sessions have been attended`() {

    val actionPlan = actionPlanFactory.create(numberOfSessions = 2)
    val referralWithActionPlanAndSomeAttendedSessions = referralFactory.createSent(actionPlans = mutableListOf(actionPlan))
    whenever(actionPlanRepository.countNumberOfAttendedSessions(actionPlan.id)).thenReturn(0)

    val endOfServiceReportCreationRequired = referralConcluder.requiresEndOfServiceReportCreation(referralWithActionPlanAndSomeAttendedSessions)

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
