package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.component

import org.springframework.stereotype.Component

@Component
class RamDeliusClient(private val ramDeliusApiClient: RestClient) {
  fun makeSyncPutRequest(uri: String, requestBody: Any) {
    ramDeliusApiClient.put(uri, requestBody).retrieve().toBodilessEntity().block()
  }
}
