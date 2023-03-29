package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.util.UriComponentsBuilder
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.component.RamDeliusClient
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Referral
import java.time.OffsetDateTime
import java.util.UUID

@Service
class RamDeliusReferralService(
  @Value("\${interventions-ui.baseurl}") private val interventionsUiBaseUrl: String,
  @Value("\${interventions-ui.locations.probation-practitioner.referral-details}") private val referralDetailsLocation: String,
  @Value("\${refer-and-monitor-and-delius.locations.referral-start}") private val referralStartLocation: String,
  private val ramDeliusClient: RamDeliusClient,
) : CommunityAPIService {
  fun send(referral: Referral) {
    val url = UriComponentsBuilder.fromHttpUrl(interventionsUiBaseUrl)
      .path(referralDetailsLocation)
      .buildAndExpand(referral.id)
      .toString()

    referralStartRequest(referral, url)
  }

  private fun referralStartRequest(referral: Referral, url: String) {
    val referRequest = ReferralSentRequest(
      referral.intervention.dynamicFrameworkContract.contractType.code,
      referral.sentAt!!,
      referral.relevantSentenceId!!,
      referral.id,
      getNotes(referral, url, "Referral Sent"),
    )

    val pathToSend = UriComponentsBuilder.fromPath(referralStartLocation)
      .buildAndExpand(referral.serviceUserCRN)
      .toString()

    ramDeliusClient.makeSyncPutRequest(
      pathToSend,
      referRequest,
    )
  }
}

data class ReferralSentRequest(
  val contractType: String,
  val startedAt: OffsetDateTime,
  val sentenceId: Long,
  val referralId: UUID,
  val notes: String,
)
