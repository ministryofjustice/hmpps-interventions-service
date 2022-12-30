package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.listeners

import org.springframework.context.ApplicationListener
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.component.SNSPublisher
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.EventDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.ReferralEvent
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.ReferralEventType
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.exception.AsyncEventExceptionHandling
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AuthUser
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.SNSService

@Service
class ReferralConcludedListener(
  private val snsPublisher: SNSPublisher,
) : ApplicationListener<ReferralEvent>, SNSService {
  @AsyncEventExceptionHandling
  override fun onApplicationEvent(event: ReferralEvent) {
    when (event.type) {
      ReferralEventType.CANCELLED -> {
        val snsEvent = EventDTO(
          "intervention.referral.cancelled",
          "A referral has been cancelled",
          event.detailUrl,
          event.referral.concludedAt!!,
          mapOf("referralId" to event.referral.id)
        )
        snsPublisher.publish(event.referral.id, event.referral.endRequestedBy!!, snsEvent)
      }
      ReferralEventType.PREMATURELY_ENDED -> {
        val snsEvent = EventDTO(
          "intervention.referral.prematurely-ended",
          "A referral has been ended prematurely",
          event.detailUrl,
          event.referral.concludedAt!!,
          mapOf("referralId" to event.referral.id)
        )
        snsPublisher.publish(event.referral.id, event.referral.endRequestedBy!!, snsEvent)
      }
      ReferralEventType.COMPLETED -> {
        val snsEvent = EventDTO(
          "intervention.referral.completed",
          "A referral has been completed",
          event.detailUrl,
          event.referral.concludedAt!!,
          mapOf("referralId" to event.referral.id)
        )
        // This is a system generated event at present and as such the actor will represent this
        snsPublisher.publish(event.referral.id, AuthUser.interventionsServiceUser, snsEvent)
      }
      else -> {}
    }
  }
}
