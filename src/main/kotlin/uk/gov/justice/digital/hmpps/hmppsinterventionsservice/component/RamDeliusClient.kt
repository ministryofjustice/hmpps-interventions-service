package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.component

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.AppointmentResponseDTO

@Component
class RamDeliusClient(private val ramDeliusApiClient: RestClient) {

  fun makeSyncPutRequest(uri: String, requestBody: Any): AppointmentResponseDTO? =
    ramDeliusApiClient.put(uri, requestBody).retrieve().bodyToMono(AppointmentResponseDTO::class.java).block()
}
