package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.controller

import mu.KLogging
import org.springframework.http.HttpStatus
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.AmendComplexityLevelDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.AmendNeedAndRequirementDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.AmendReferralService
import java.util.UUID

@RestController
class AmendReferralController(
  private val amendReferralService: AmendReferralService
) {
  companion object : KLogging()

  @PatchMapping("/sent-referral/{referralId}/amend-complexity-level")
  fun updateComplexityLevel(
    @PathVariable referralId: UUID,
    @RequestBody complexityLevel: AmendComplexityLevelDTO,
    authentication: JwtAuthenticationToken
  ): AmendComplexityLevelDTO? {
    val referral = amendReferralService.getSentReferralForAuthenticatedUser(authentication, referralId)
    return amendReferralService.updateComplexityLevel(referral, complexityLevel, authentication)?.let {
      AmendComplexityLevelDTO.from(it)
    } ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "complexity level could not be updated")
  }

  @PatchMapping("/sent-referral/{referralId}/amend-needs-and-requirement")
  fun updateNeedsAndRequirements(
    @PathVariable referralId: UUID,
    @RequestBody needsAndRequirement: AmendNeedAndRequirementDTO,
    authentication: JwtAuthenticationToken
  ): AmendNeedAndRequirementDTO? {
    val referral = amendReferralService.getSentReferralForAuthenticatedUser(authentication, referralId)
    return amendReferralService.updateNeedAndRequirement(referral, needsAndRequirement, authentication)?.let {
      AmendNeedAndRequirementDTO.from(it)
    } ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "needs and requirements could not be updated")
  }
}
