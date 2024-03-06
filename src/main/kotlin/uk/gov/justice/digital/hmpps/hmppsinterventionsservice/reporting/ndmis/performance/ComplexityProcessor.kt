package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.reporting.ndmis.performance

import mu.KLogging
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Referral
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.reporting.SentReferralProcessor

@Component
class ComplexityProcessor() : SentReferralProcessor<List<ComplexityData>> {
  companion object : KLogging()

  override fun processSentReferral(referral: Referral): List<ComplexityData> {
    logger.info("Processing the complexity data")
    try {
      return referral.selectedServiceCategories!!.map {
        ComplexityData(
          referralReference = referral.referenceNumber!!,
          referralId = referral.id,
          interventionTitle = referral.intervention.title,
          serviceCategoryId = it.id,
          serviceCategoryName = it.name,
          complexityLevelTitle = it.complexityLevels.find { complexityLevel ->
            complexityLevel.id == referral.complexityLevelIds?.get(it.id)
          }?.title.orEmpty(),
        )
      }
    } catch (e: Exception) {
      logger.info("The exception while processing complexity data is ${e.message}")
      throw e
    }
  }
}
