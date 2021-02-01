package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.controller

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.InterventionDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.InterventionService
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.utils.StringToUUIDConverter

@RestController
class InterventionController(
  private val interventionService: InterventionService,
) {

  @GetMapping("/intervention/{id}")
  fun getInterventionByID(@PathVariable id: String): InterventionDTO {
    val uuid = StringToUUIDConverter.parseID(id)

    return interventionService.getIntervention(uuid)
      ?.let { InterventionDTO.from(it) }
      ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "intervention not found [id=$uuid]")
  }
}
