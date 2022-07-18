package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.controller

import mu.KLogging
import org.springframework.http.HttpStatus
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.authorization.UserMapper
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.AmendComplexityLevelDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.AmendNeedAndRequirementDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Referral
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.AmendReferralService
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.ReferralService
import java.util.UUID

@RestController
class AmendReferralController(
  private val amendReferralService: AmendReferralService,
  private val userMapper: UserMapper,
  private val referralService: ReferralService
) {
  companion object : KLogging()

  @PatchMapping("/sent-referral/{referralId}/amend-complexity-level")
  fun updateComplexityLevel(
    @PathVariable referralId: UUID,
    @RequestBody complexityLevel: AmendComplexityLevelDTO,
    authentication: JwtAuthenticationToken
  ): AmendComplexityLevelDTO? {
    val user = userMapper.fromToken(authentication)
    val referral = getSentReferralForAuthenticatedUser(authentication, referralId)
    return amendReferralService.updateComplexityLevel(referral, complexityLevel, user)?.let {
      AmendComplexityLevelDTO.from(it)
    } ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "complexity level could not be updated")
  }

  private fun getSentReferralForAuthenticatedUser(authentication: JwtAuthenticationToken, id: UUID): Referral {
    val user = userMapper.fromToken(authentication)
    return referralService.getSentReferralForUser(id, user)
      ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "sent referral not found [id=$id]")
  }

  @PatchMapping("/sent-referral/{referralId}/amend-needs-and-requirement")
  fun updateNeedsAndRequirements(
    @PathVariable referralId: UUID,
    @RequestBody needsAndRequirement: AmendNeedAndRequirementDTO,
    authentication: JwtAuthenticationToken
  ): AmendNeedAndRequirementDTO? {
    val user = userMapper.fromToken(authentication)
    val referral = getSentReferralForAuthenticatedUser(authentication, referralId)
    return amendReferralService.updateNeedAndRequirement(referral, needsAndRequirement, user)?.let {
      AmendNeedAndRequirementDTO.from(it)
    } ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "complexity level could not be updated")
  }
}
