package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.controller

import org.apache.commons.lang3.StringUtils
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.SarDataDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.SarsDataService

/**
 * Controller for Subject Access Request (SAR) endpoints.
 *
 * GET /subject-access-request         — returns SAR data for a given CRN
 * GET /subject-access-request/template — returns the Mustache template used to render the SAR PDF
 */
@RestController
@PreAuthorize("hasRole('ROLE_SAR_DATA_ACCESS')")
@RequestMapping("/subject-access-request")
class SarsDataController(
  val sarsDataService: SarsDataService,
) {

  @GetMapping
  fun sarData(
    @RequestParam crn: String? = null,
    @RequestParam fromDate: String? = null,
    @RequestParam toDate: String? = null,
  ): ResponseEntity<SarDataDTO> {
    if (crn == null) {
      return ResponseEntity(null, null, 209)
    }

    val from: String? = if (StringUtils.isNotBlank(fromDate)) fromDate else null
    val to: String? = if (StringUtils.isNotBlank(toDate)) toDate else null

    val sarsData = sarsDataService.getSarsReferralData(crn, from, to)
    if (sarsData.isEmpty()) {
      return ResponseEntity(SarDataDTO.from(crn, sarsData), HttpStatus.NO_CONTENT)
    }
    return ResponseEntity<SarDataDTO>(SarDataDTO.from(crn, sarsData), HttpStatus.OK)
  }

  @GetMapping("/template", produces = [MediaType.TEXT_PLAIN_VALUE])
  fun getTemplate(): ResponseEntity<String> {
    val template = javaClass.getResourceAsStream("/sar_template.mustache")
      ?.bufferedReader(Charsets.UTF_8)
      ?.use { it.readText() }
      ?: return ResponseEntity.internalServerError()
        .body("SAR template not found at /sar_template.mustache")

    return ResponseEntity.ok()
      .contentType(MediaType.TEXT_PLAIN)
      .body(template)
  }
}
