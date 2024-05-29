package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service

import com.microsoft.applicationinsights.TelemetryClient
import mu.KotlinLogging
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.WithdrawReferralRequestDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Referral

@Service
class TelemetryService(
  private val telemetryClient: TelemetryClient,
) {
  companion object {
    private val logger = KotlinLogging.logger {}
  }
  fun reportInvalidAssumption(assumption: String, information: Map<String, String> = emptyMap(), recoverable: Boolean = true) {
    telemetryClient.trackEvent(
      "InterventionsInvalidAssumption",
      mapOf("assumption" to assumption).plus(information),
      null,
    )

    if (!recoverable) {
      logger.warn("assumption proved invalid: $assumption")
    }
  }

  fun reportWithdrawalInformation(referral: Referral, withdrawReferralRequestDTO: WithdrawReferralRequestDTO) {
    telemetryClient.trackEvent(
      "ReferralWithdrawalInformation",
      mapOf(
        "referralId" to referral.id.toString(),
        "referral" to referral.referenceNumber!!,
        "withdrawalReasonCode" to if (referral.withdrawalReasonCode != null) referral.withdrawalReasonCode!! else "",
        "withdrawalComments" to if (referral.withdrawalComments != null) referral.withdrawalComments!! else "",
      ),
      null,
    )
  }
}
