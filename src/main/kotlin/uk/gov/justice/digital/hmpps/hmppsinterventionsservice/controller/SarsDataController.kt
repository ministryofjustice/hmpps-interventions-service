package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.controller

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.SarDataDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.SarsDataService

@RestController
@PreAuthorize("hasRole('SAR_DATA_ACCESS')")
class SarsDataController(
  val sarsDataService: SarsDataService,
) {

  @GetMapping("/subject-access-request")
  fun sarData(
    @RequestParam crn: String? = null,
    @RequestParam fromDate: String? = null,
    @RequestParam toDate: String? = null,
    authentication: JwtAuthenticationToken,
  ): ResponseEntity<SarDataDTO> {
    if (crn == null) {
      return ResponseEntity(null, null, 209)
    }
    val sarsData = sarsDataService.getSarsReferralData(crn, fromDate, toDate)
    if (sarsData.isEmpty()) {
      return ResponseEntity(SarDataDTO.from(crn, sarsData), HttpStatus.NO_CONTENT)
    }
    return ResponseEntity<SarDataDTO>(SarDataDTO.from(crn, sarsData), HttpStatus.OK)
  }
}
