package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.controller

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.server.ResponseStatusException
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.CreateInterventionDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.InterventionDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.InterventionCsvService
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.InterventionService
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.utils.StringToUUIDConverter

@RestController
class InterventionController(
  private val interventionService: InterventionService,
  private val interventionCsvService: InterventionCsvService,
) {

  @GetMapping("/intervention/{id}")
  fun getInterventionByID(@PathVariable id: String): InterventionDTO {
    val uuid = StringToUUIDConverter.parseID(id)

    return interventionService.getIntervention(uuid)
      ?.let { InterventionDTO.from(it) }
      ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "intervention not found [id=$uuid]")
  }

  @PostMapping("/intervention/add")
  fun createInterventions(@RequestParam("file") file: MultipartFile): List<CreateInterventionDTO> {
    return interventionCsvService.uploadCsvFile(file)
  }
}
