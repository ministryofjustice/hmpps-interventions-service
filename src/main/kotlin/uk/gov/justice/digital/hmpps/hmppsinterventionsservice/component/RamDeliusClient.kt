package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.component

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.AppointmentResponseDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.ConvictionDetails

@Component
class RamDeliusClient(private val ramDeliusApiClient: RestClient) {

  fun makeSyncPutRequest(uri: String, requestBody: Any) = ramDeliusApiClient.put(uri, requestBody).retrieve().toBodilessEntity().block()

  fun makePutAppointmentRequest(uri: String, requestBody: Any): AppointmentResponseDTO? = ramDeliusApiClient.put(uri, requestBody).retrieve().bodyToMono(AppointmentResponseDTO::class.java).block()

  fun makeGetConvictionRequest(uri: String): ConvictionDetails? = ramDeliusApiClient.get(uri).retrieve().bodyToMono(ConvictionDetails::class.java).block()
}
