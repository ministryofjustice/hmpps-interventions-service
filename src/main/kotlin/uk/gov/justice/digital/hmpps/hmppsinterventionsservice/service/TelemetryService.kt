package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service

import com.microsoft.applicationinsights.TelemetryClient
import mu.KotlinLogging
import org.springframework.stereotype.Service

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
      logger.error("assumption proved invalid: $assumption")
    }
  }
}
