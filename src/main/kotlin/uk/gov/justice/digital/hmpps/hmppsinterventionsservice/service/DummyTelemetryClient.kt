package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service

import com.microsoft.applicationinsights.TelemetryClient
import org.springframework.stereotype.Service

// Will be replaced by the Agent during runtime
@Service
class DummyTelemetryClient : TelemetryClient()
