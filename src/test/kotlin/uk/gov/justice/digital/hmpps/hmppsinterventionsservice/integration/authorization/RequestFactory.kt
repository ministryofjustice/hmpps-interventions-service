package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.integration.authorization

import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.AuthUserDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.CreateCaseNoteDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.CreateReferralRequestDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.DraftReferralDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.ReferralAssignmentDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.WithdrawReferralRequestDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.integration.SetupAssistant
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.ReferralWithdrawalState
import java.util.UUID

enum class Request {
  CreateDraftReferral,
  GetDraftReferral,
  UpdateDraftReferral,
  SendDraftReferral,
  GetSentReferral,
  AssignSentReferral,
  EndSentReferral,
  GetDraftReferrals,
  GetSentReferralSummaries,
  GetServiceProviderReferralsSummary,
  CreateCaseNote,
}

class RequestFactory(private val webTestClient: WebTestClient, private val setupAssistant: SetupAssistant) {
  fun create(request: Request, token: String?, vararg urlParams: String, body: Any? = null): WebTestClient.RequestHeadersSpec<*> {
    val spec = when (request) {
      Request.CreateDraftReferral -> webTestClient.post().uri("/draft-referral").bodyValue(
        if (body != null) body as CreateReferralRequestDTO else CreateReferralRequestDTO("X123456", UUID.randomUUID()),
      )

      Request.GetDraftReferral -> webTestClient.get().uri("/draft-referral/${urlParams[0]}")
      Request.UpdateDraftReferral -> webTestClient.patch().uri("/draft-referral/${urlParams[0]}").bodyValue(
        if (body != null) body as DraftReferralDTO else DraftReferralDTO(),
      )
      Request.SendDraftReferral -> webTestClient.post().uri("/draft-referral/${urlParams[0]}/send")

      Request.GetSentReferral -> webTestClient.get().uri("/sent-referral/${urlParams[0]}")
      Request.AssignSentReferral -> webTestClient.post().uri("/sent-referral/${urlParams[0]}/assign").bodyValue(
        if (body != null) body as ReferralAssignmentDTO else ReferralAssignmentDTO(AuthUserDTO.from(setupAssistant.createSPUser())),
      )
      Request.EndSentReferral -> webTestClient.post().uri("/sent-referral/${urlParams[0]}/withdraw-referral").bodyValue(
        if (body != null) body as WithdrawReferralRequestDTO else WithdrawReferralRequestDTO(setupAssistant.randomWithDrawReason().code, "comments", ReferralWithdrawalState.PRE_ICA_WITHDRAWAL.name),
      )

      Request.GetDraftReferrals -> webTestClient.get().uri("/draft-referrals")
      Request.GetSentReferralSummaries -> webTestClient.get().uri("/sent-referrals/summaries")
      Request.GetServiceProviderReferralsSummary -> webTestClient.get().uri("/sent-referrals/summary/service-provider")

      Request.CreateCaseNote -> webTestClient.post().uri("/case-note").bodyValue(
        if (body != null) body as CreateCaseNoteDTO else CreateCaseNoteDTO(urlParams[0] as UUID, "subject", "body", false),
      )
    }

    return if (token != null) {
      spec.headers { http -> http.setBearerAuth(token) }
    } else {
      spec
    }
  }
}
