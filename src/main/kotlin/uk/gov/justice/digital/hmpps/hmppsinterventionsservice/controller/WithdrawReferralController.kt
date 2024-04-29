package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.controller

import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.authorization.UserMapper
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.SentReferralDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.WithdrawReferralRequestDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Referral
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.WithdrawalReason
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.ReferralConcluder
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.ReferralService
import java.util.UUID

@RestController
@PreAuthorize("hasRole('ROLE_PROBATION') or hasRole('ROLE_CRS_PROVIDER') or hasRole('ROLE_INTERVENTIONS_API_READ_ALL')")
class WithdrawReferralController(
  val referralService: ReferralService,
  val userMapper: UserMapper,
  val referralConcluder: ReferralConcluder,
) {

  @GetMapping("/referral-withdrawal-reasons")
  fun getWithdrawalReasons(): List<WithdrawalReason> {
    return referralService.getWithdrawalReasons()
  }

  @PostMapping("/sent-referral/{id}/withdraw-referral")
  fun submitWithdrawalReasons(
    @PathVariable("id") referralId: UUID,
    @RequestBody withdrawReferralRequestDTO: WithdrawReferralRequestDTO,
    authentication: JwtAuthenticationToken,
  ): SentReferralDTO {
    val user = userMapper.fromToken(authentication)
    val sentReferral = getSentReferralForAuthenticatedUser(authentication, referralId)
    return SentReferralDTO.from(
      referralService.requestReferralEnd(sentReferral, user, withdrawReferralRequestDTO),
      referralConcluder.requiresEndOfServiceReportCreation(sentReferral),
    )
  }

  private fun getSentReferralForAuthenticatedUser(authentication: JwtAuthenticationToken, id: UUID): Referral {
    val user = userMapper.fromToken(authentication)
    return referralService.getSentReferralForUser(id, user)
      ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "sent referral not found [id=$id]")
  }
}
