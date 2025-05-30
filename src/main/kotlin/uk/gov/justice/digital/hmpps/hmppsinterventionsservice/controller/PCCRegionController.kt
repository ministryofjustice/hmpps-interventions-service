package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.controller

import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.PCCRegionDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.PCCRegionService

@RestController
@PreAuthorize("hasRole('ROLE_PROBATION') or hasRole('ROLE_CRS_PROVIDER') or hasRole('ROLE_INTERVENTIONS_API_READ_ALL')")
class PCCRegionController(private val pccRegionService: PCCRegionService) {
  @GetMapping("/pcc-regions")
  fun getAllPCCRegions(): List<PCCRegionDTO> = pccRegionService.getAllPCCRegions().map { PCCRegionDTO.from(it) }
}
