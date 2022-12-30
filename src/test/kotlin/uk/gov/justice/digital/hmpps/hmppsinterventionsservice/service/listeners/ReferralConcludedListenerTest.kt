package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.listeners

import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.component.SNSPublisher
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.EventDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.ReferralEvent
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.ReferralEventType
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.ReferralEventType.CANCELLED
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.ReferralEventType.COMPLETED
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.ReferralEventType.PREMATURELY_ENDED
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AuthUser
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ReferralAssignment
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.AuthUserFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ReferralFactory
import java.time.OffsetDateTime
import java.util.UUID

internal class ReferralConcludedListenerTest {
  private val snsPublisher = mock<SNSPublisher>()
  private val referralFactory = ReferralFactory()
  private val authUserFactory = AuthUserFactory()

  private val listener = ReferralConcludedListener(snsPublisher)

  private fun referralConcludedEvent(eventType: ReferralEventType) = ReferralEvent(
    "source",
    eventType,
    referralFactory.createEnded(
      id = UUID.fromString("68df9f6c-3fcb-4ec6-8fcf-96551cd9b080"),
      referenceNumber = "HAS71263",
      concludedAt = OffsetDateTime.parse("2020-12-04T10:42:43+00:00"),
      endRequestedBy = AuthUser("ecd7b8d690", "irrelevant", "irrelevant"),
      assignments = listOf(
        ReferralAssignment(
          OffsetDateTime.parse("2020-12-04T10:42:43+00:00"),
          authUserFactory.createSP("irrelevant"),
          authUserFactory.createSP("abc123")
        )
      )
    ),
    "http://localhost:8080/sent-referral/68df9f6c-3fcb-4ec6-8fcf-96551cd9b080"
  )

  @Test
  fun `publishes referral cancelled event message`() {
    val referralConcludedEvent = referralConcludedEvent(CANCELLED)
    listener.onApplicationEvent(referralConcludedEvent)
    val snsEvent = EventDTO(
      "intervention.referral.cancelled",
      "A referral has been cancelled",
      "http://localhost:8080" + "/sent-referral/${referralConcludedEvent.referral.id}",
      referralConcludedEvent.referral.concludedAt!!,
      mapOf("referralId" to UUID.fromString("68df9f6c-3fcb-4ec6-8fcf-96551cd9b080"))
    )
    verify(snsPublisher).publish(
      referralConcludedEvent.referral.id,
      referralConcludedEvent.referral.endRequestedBy!!,
      snsEvent
    )
  }

  @Test
  fun `publishes referral prematurely ended event message`() {
    val referralConcludedEvent = referralConcludedEvent(PREMATURELY_ENDED)
    listener.onApplicationEvent(referralConcludedEvent)
    val snsEvent = EventDTO(
      "intervention.referral.prematurely-ended",
      "A referral has been ended prematurely",
      "http://localhost:8080" + "/sent-referral/${referralConcludedEvent.referral.id}",
      referralConcludedEvent.referral.concludedAt!!,
      mapOf("referralId" to UUID.fromString("68df9f6c-3fcb-4ec6-8fcf-96551cd9b080"))
    )
    verify(snsPublisher).publish(
      referralConcludedEvent.referral.id,
      referralConcludedEvent.referral.endRequestedBy!!,
      snsEvent
    )
  }

  @Test
  fun `publishes referral completed event message with actor as Interventions service user`() {
    val referralConcludedEvent = referralConcludedEvent(COMPLETED)
    listener.onApplicationEvent(referralConcludedEvent)
    val snsEvent = EventDTO(
      "intervention.referral.completed",
      "A referral has been completed",
      "http://localhost:8080" + "/sent-referral/${referralConcludedEvent.referral.id}",
      referralConcludedEvent.referral.concludedAt!!,
      mapOf("referralId" to UUID.fromString("68df9f6c-3fcb-4ec6-8fcf-96551cd9b080"))
    )
    verify(snsPublisher).publish(
      referralConcludedEvent.referral.id,
      AuthUser("00000000-0000-0000-0000-000000000000", "urn:hmpps:interventions", "hmpps-interventions-service"),
      snsEvent
    )
  }
}
