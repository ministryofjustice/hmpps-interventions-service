package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.controller

import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.Prisoner
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.PrisonerDetailsService

@RestController
@PreAuthorize("hasRole('ROLE_PROBATION') or hasRole('ROLE_CRS_PROVIDER') or hasRole('ROLE_INTERVENTIONS_API_READ_ALL')")
class PrisonerDetailsController(
  val prisonerDetailsService: PrisonerDetailsService,
) {

  @GetMapping("/prisoner/details/{crn}")
  fun prisonerDetails(
    @PathVariable crn: String,
    authentication: JwtAuthenticationToken,
  ): Prisoner? {
    return prisonerDetailsService.details(crn)
  }
}
