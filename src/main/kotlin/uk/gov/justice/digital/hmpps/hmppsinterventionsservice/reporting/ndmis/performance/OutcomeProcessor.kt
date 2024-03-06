package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.reporting.ndmis.performance

import mu.KLogging
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Referral
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.reporting.SentReferralProcessor

@Component
class OutcomeProcessor() : SentReferralProcessor<List<OutcomeData>> {
  companion object : KLogging()

  override fun processSentReferral(referral: Referral): List<OutcomeData>? {
    logger.info("Processing the outocme data")

    try {
      return referral.endOfServiceReport?.outcomes?.map {
        OutcomeData(
          referralReference = referral.referenceNumber!!,
          referralId = referral.id,
          desiredOutcomeDescription = it.desiredOutcome.description,
          achievementLevel = it.achievementLevel,
        )
      }?.ifEmpty { null }
    } catch (e: Exception) {
      logger.info("The exception while processing outcome data is ${e.message}")
      throw e
    }
  }
}
