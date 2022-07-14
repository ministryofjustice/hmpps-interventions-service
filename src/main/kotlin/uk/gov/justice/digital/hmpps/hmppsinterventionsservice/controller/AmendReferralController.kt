package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.controller

import mu.KLogging
import org.springframework.http.HttpStatus
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.AmendComplexityLevelDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.AmendDesiredOutcomesDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.AmendReferralsDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.AmendReferralService
import java.util.UUID

@RestController
class AmendReferralController(
  private val amendReferralService: AmendReferralService
) {
  companion object : KLogging()

  @PostMapping("/sent-referral/{referralId}/service-category/{serviceCategoryId}/amend-complexity-level")
  fun updateComplexityLevel(
    @PathVariable referralId: UUID,
    @PathVariable serviceCategoryId: UUID,
    @RequestBody complexityLevel: AmendComplexityLevelDTO,
    authentication: JwtAuthenticationToken
  ): AmendReferralsDTO? {
    val referral = amendReferralService.getSentReferralForAuthenticatedUser(authentication, referralId)
    return amendReferralService.updateComplexityLevel(referral, complexityLevel, serviceCategoryId, authentication)?.let {
      AmendReferralsDTO.from(it)
    } ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "complexity level could not be updated")
  }

  @PostMapping("/sent-referral/{referralId}/service-category/{serviceCategoryId}/amend-desired-outcomes")
  fun amendDesiredOutcomes(
    authentication: JwtAuthenticationToken,
    @PathVariable referralId: UUID,
    @PathVariable serviceCategoryId: UUID,
    @RequestBody request: AmendDesiredOutcomesDTO
  ): AmendReferralsDTO {
    val referral = amendReferralService.getSentReferralForAuthenticatedUser(authentication, referralId)
    return amendReferralService.updateReferralDesiredOutcomes(referral, request, authentication, serviceCategoryId)?.let {
      AmendReferralsDTO.from(it)
    } ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "desired outcomes could not be updated")
  }
}
