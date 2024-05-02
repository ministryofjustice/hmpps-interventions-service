package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.listeners

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.component.EmailSender
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.component.SNSPublisher
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.EventDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.PersonReference
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.ReferralConcludedEvent
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AuthUser
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.EndOfServiceReport
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ReferralAssignment
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.WithdrawalReason
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.WithdrawalReasonRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.HMPPSAuthService
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.ReferralConcludedState
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.ReferralWithdrawalState
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.UserDetail
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.AssignmentsFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.AuthUserFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ReferralFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ReferralServiceUserFactory
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import java.util.stream.Stream

private val sentAtDefault = OffsetDateTime.of(2020, 1, 1, 1, 1, 1, 1, ZoneOffset.UTC)
private val concludedAtDefault = OffsetDateTime.of(2020, 2, 2, 2, 2, 2, 2, ZoneOffset.UTC)
private val submittedAtDefault = OffsetDateTime.of(2020, 3, 3, 3, 3, 3, 3, ZoneOffset.UTC)

private fun referralConcludedEvent(
  eventType: ReferralConcludedState,
  endOfServiceReport: EndOfServiceReport? = null,
): ReferralConcludedEvent {
  val referralFactory = ReferralFactory()
  val authUserFactory = AuthUserFactory()
  return ReferralConcludedEvent(
    "source",
    eventType,
    referralFactory.createEnded(
      id = UUID.fromString("68df9f6c-3fcb-4ec6-8fcf-96551cd9b080"),
      referenceNumber = "HAS71263",
      relevantSentenceId = 123456789,
      sentAt = sentAtDefault,
      concludedAt = concludedAtDefault,
      endOfServiceReport = endOfServiceReport,
      assignments = listOf(
        ReferralAssignment(
          OffsetDateTime.parse("2020-12-04T10:42:43+00:00"),
          authUserFactory.createSP("irrelevant"),
          authUserFactory.createSP("abc123"),
        ),
      ),
    ),
    "http://localhost:8080/sent-referral/68df9f6c-3fcb-4ec6-8fcf-96551cd9b080",
    ReferralWithdrawalState.PRE_ICA_WITHDRAWAL,
  )
}

internal class ReferralConcludedListenerTest {
  private val snsPublisher = mock<SNSPublisher>()
  private val listener = ReferralConcludedListener(snsPublisher)

  @ParameterizedTest
  @EnumSource(ReferralConcludedState::class)
  fun `publishes referral concluded event`(state: ReferralConcludedState) {
    val referralConcludedEvent = referralConcludedEvent(state)
    listener.onApplicationEvent(referralConcludedEvent)
    val snsEvent = EventDTO(
      "intervention.referral.concluded",
      "The referral has concluded, no more work is needed",
      "http://localhost:8080" + "/sent-referral/${referralConcludedEvent.referral.id}",
      referralConcludedEvent.referral.concludedAt!!,
      mapOf(
        "deliveryState" to state.name,
        "referralId" to UUID.fromString("68df9f6c-3fcb-4ec6-8fcf-96551cd9b080"),
      ),
      PersonReference.crn(referralConcludedEvent.referral.serviceUserCRN),
    )
    verify(snsPublisher).publish(referralConcludedEvent.referral.id, AuthUser.interventionsServiceUser, snsEvent)
  }
}

internal class ReferralConcludedNotificationListenerTest {
  private val emailSender = mock<EmailSender>()
  private val hmppsAuthService = mock<HMPPSAuthService>()
  private val referralFactory = ReferralFactory()
  private val assignmentsFactory = AssignmentsFactory()
  private val authUserFactory = AuthUserFactory()
  private val referralServiceUserFactory = ReferralServiceUserFactory()
  private val withdrawalReasonRepository = mock<WithdrawalReasonRepository>()

  private val sentReferral = referralFactory.createSent(
    id = UUID.fromString("68df9f6c-3fcb-4ec6-8fcf-96551cd9b080"),
    referenceNumber = "JS8762AC",
    assignments = assignmentsFactory.create(1),
    serviceUserData = referralServiceUserFactory.create("Alex", "River"),
    withdrawReasonCode = "MIS",
    withdrawReasonComments = "some comments",
    endRequestedBy = authUserFactory.create(),
  )
  companion object {
    @JvmStatic
    fun withdrawStateSource(): Stream<Arguments> {
      return Stream.of(
        arguments(ReferralWithdrawalState.PRE_ICA_WITHDRAWAL, "withDrawnReferralPreIcaTemplateId"),
        arguments(ReferralWithdrawalState.POST_ICA_WITHDRAWAL, "withDrawnReferralPostIcaTemplateId"),
        arguments(ReferralWithdrawalState.POST_ICA_CLOSE_REFERRAL_EARLY, "withDrawnReferralWithdrawnEarlyTemplateId"),
      )
    }
  }

  private fun notifyService(): ReferralConcludedNotificationListener {
    return ReferralConcludedNotificationListener(
      "withDrawnReferralPreIcaTemplateId",
      "withDrawnReferralPostIcaTemplateId",
      "withDrawnReferralWithdrawnEarlyTemplateId",
      emailSender,
      hmppsAuthService,
      withdrawalReasonRepository,
    )
  }

  @ParameterizedTest
  @MethodSource("withdrawStateSource")
  fun `referral sent event generates valid url and sends an email`(referralWithdrawalState: ReferralWithdrawalState, templateId: String) {
    val referralCancelledEvent = ReferralConcludedEvent(
      "source",
      ReferralConcludedState.CANCELLED,
      sentReferral,
      "http://localhost:8080/sent-referral/68df9f6c-3fcb-4ec6-8fcf-96551cd9b080",
      referralWithdrawalState,
    )
    whenever(hmppsAuthService.getUserDetail(any<AuthUser>())).thenReturn(UserDetail("tom", "tom@tom.tom", "jones"))
    whenever(hmppsAuthService.getUserDetail(eq(sentReferral.endRequestedBy!!))).thenReturn(UserDetail("bernard", "bernard@b.tom", "beaks"))
    whenever(withdrawalReasonRepository.findByCode("MIS")).thenReturn(WithdrawalReason("MIS", "some comments", "A"))
    notifyService().onApplicationEvent(referralCancelledEvent)
    val personalisationCaptor = argumentCaptor<Map<String, String>>()
    verify(emailSender).sendEmail(
      eq(templateId),
      eq("tom@tom.tom"),
      personalisationCaptor.capture(),
    )
    assertThat(personalisationCaptor.firstValue["caseworkerFirstName"]).isEqualTo("tom")
    assertThat(personalisationCaptor.firstValue["referralNumber"]).isEqualTo("JS8762AC")
    assertThat(personalisationCaptor.firstValue["popFullName"]).isEqualTo("Alex River")
    assertThat(personalisationCaptor.firstValue["changedByName"]).isEqualTo("bernard")
    assertThat(personalisationCaptor.firstValue["reasonForWithdrawal"]).isEqualTo("some comments")
  }

  @Test
  fun `referral sent event will not send an email if the referral is not assigned to everyone`() {
    whenever(hmppsAuthService.getUserDetail(any<AuthUser>())).thenReturn(UserDetail("tom", "tom@tom.tom", "jones"))
    whenever(hmppsAuthService.getUserDetail(eq(sentReferral.endRequestedBy!!))).thenReturn(UserDetail("bernard", "bernard@b.tom", "beaks"))
    whenever(withdrawalReasonRepository.findByCode("MIS")).thenReturn(WithdrawalReason("MIS", "some comments", "A"))
    val sentReferral = referralFactory.createSent(
      id = UUID.fromString("68df9f6c-3fcb-4ec6-8fcf-96551cd9b080"),
      referenceNumber = "JS8762AC",
      assignments = emptyList(),
      serviceUserData = referralServiceUserFactory.create("Alex", "River"),
      withdrawReasonCode = "MIS",
      withdrawReasonComments = "some comments",
      endRequestedBy = authUserFactory.create(),
    )

    val referralCancelledEvent = ReferralConcludedEvent(
      "source",
      ReferralConcludedState.CANCELLED,
      sentReferral,
      "http://localhost:8080/sent-referral/68df9f6c-3fcb-4ec6-8fcf-96551cd9b080",
      ReferralWithdrawalState.PRE_ICA_WITHDRAWAL,
    )
    notifyService().onApplicationEvent(referralCancelledEvent)
    verify(hmppsAuthService, times(1)).getUserDetail(any<AuthUser>())
    verify(emailSender, times(0)).sendEmail(any(), any(), any())
  }
}
