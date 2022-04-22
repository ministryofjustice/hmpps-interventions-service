package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.listeners

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.springframework.context.ApplicationListener
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.ReferralEvent
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.ReferralEventType
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Referral
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ReferralRepository
import java.time.Duration
import java.time.OffsetDateTime
import java.util.UUID

@Service
class ReferralMetricsListener(
  val registry: MeterRegistry,
  val repository: ReferralRepository,
) : ApplicationListener<ReferralEvent> {
  override fun onApplicationEvent(event: ReferralEvent) {
    if (event.type != ReferralEventType.SENT) {
      return
    }

    val current = event.referral
    val previous = findPreviousReferral(current)
      ?: return

    val elapsedTime = Duration.between(previous.sentAt, current.sentAt)
    timer(previous, current).record(elapsedTime)
  }

  private fun timer(previous: Referral, current: Referral): Timer {
    val interventionKind =
      if (current.intervention.id == previous.intervention.id) "same"
      else "different"

    return Timer.builder("intervention.referral.repeated_time")
      .tag("crn", current.serviceUserCRN)
      .tag("intervention", interventionKind)
      .register(registry)
  }

  private fun findPreviousReferral(current: Referral): Referral? {
    return repository.findAll(
      otherReferralsForSamePerson(current.id, current.serviceUserCRN),
      limitToLatestSent()
    ).firstOrNull()
  }

  private fun otherReferralsForSamePerson(id: UUID, crn: String) =
    Specification<Referral> { r, _, cb ->
      cb.and(
        cb.notEqual(r.get<String>("id"), id),
        cb.equal(r.get<String>("serviceUserCRN"), crn),
        cb.isNotNull(r.get<OffsetDateTime?>("sentAt"))
      )
    }

  private fun limitToLatestSent() = PageRequest.ofSize(1).withSort(Sort.Direction.DESC, "sentAt")
}
