package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.controller

import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.component.LocationMapper
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.ProbationCaseReferralDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.ProbationCaseService
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.ReferralService

@RestController
class ProbationCaseController(
  val probationCaseService: ProbationCaseService,
  val referralService: ReferralService,
  val locationMapper: LocationMapper,
) {
  @PreAuthorize("hasRole('ROLE_INTERVENTIONS_REFER_AND_MONITOR')")
  @GetMapping("/probation-case/{crn}/referral")
  fun getReferralByCrn(
    @PathVariable crn: String,
    authentication: JwtAuthenticationToken,
  ): ResponseEntity<List<ProbationCaseReferralDTO>> {
    val probationCaseDetails = probationCaseService.getProbationCaseDetails(crn)
    val location = locationMapper.expandPathToCurrentRequestUrl("/probation-case/{crn}/referral", crn)

    return ResponseEntity.created(location).body(probationCaseDetails)
  }
}
