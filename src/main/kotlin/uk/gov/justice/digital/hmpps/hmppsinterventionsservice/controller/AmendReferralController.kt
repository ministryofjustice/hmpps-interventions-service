package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.controller

import mu.KLogging
import org.springframework.http.HttpStatus.NO_CONTENT
import org.springframework.http.ResponseEntity
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.AmendComplexityLevelDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.AmendDesiredOutcomesDTO
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
  ): ResponseEntity<Any> {
    amendReferralService.updateComplexityLevel(referralId, complexityLevel, serviceCategoryId, authentication)
    return ResponseEntity(NO_CONTENT)
  }

  @PostMapping("/sent-referral/{referralId}/service-category/{serviceCategoryId}/amend-desired-outcomes")
  fun amendDesiredOutcomes(
    authentication: JwtAuthenticationToken,
    @PathVariable referralId: UUID,
    @PathVariable serviceCategoryId: UUID,
    @RequestBody request: AmendDesiredOutcomesDTO
  ): ResponseEntity<Any> {
    amendReferralService.updateReferralDesiredOutcomes(referralId, request, authentication, serviceCategoryId)
    return ResponseEntity(NO_CONTENT)
  }
}
