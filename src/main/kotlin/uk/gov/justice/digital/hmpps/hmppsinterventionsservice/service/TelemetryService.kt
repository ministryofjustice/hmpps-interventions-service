package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service

import com.microsoft.applicationinsights.TelemetryClient
import org.springframework.stereotype.Service

@Service
class TelemetryService(
  private val telemetryClient: TelemetryClient,
) {
  fun reportInvalidAssumption(assumption: String, information: Map<String, String> = emptyMap()) {
    telemetryClient.trackEvent(
      "InterventionsInvalidAssumption",
      mapOf("assumption" to assumption).plus(information),
      null
    )
  }
}
