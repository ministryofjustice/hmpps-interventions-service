package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jobs.oneoff.loadstatus

import mu.KLogging
import net.logstash.logback.argument.StructuredArguments.kv
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.item.ItemProcessor
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Referral
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Status
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.DeliverySessionRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ReferralRepository

@Component
@JobScope
class LoadStatusProcessor(
  private val deliverySessionRepository: DeliverySessionRepository,
  private val referralRepository: ReferralRepository,
) : ItemProcessor<Referral, Referral> {
  companion object : KLogging()

  override fun process(referral: Referral): Referral {
    logger.info("processing referral {} for status", kv("referralId", referral.id))
    return updateReferralStatus(referral)
  }

  fun updateReferralStatus(referral: Referral): Referral {
    val status = if (deliveredFirstSubstantiveAppointment(referral)) Status.POST_ICA else Status.PRE_ICA
    referral.status = status
    return referralRepository.save(referral)
  }

  private fun countSessionsAttended(referral: Referral): Int {
    return deliverySessionRepository.countNumberOfAttendedSessions(referral.id)
  }

  private fun deliveredFirstSubstantiveAppointment(referral: Referral): Boolean {
    return countSessionsAttended(referral) > 0
  }
}
