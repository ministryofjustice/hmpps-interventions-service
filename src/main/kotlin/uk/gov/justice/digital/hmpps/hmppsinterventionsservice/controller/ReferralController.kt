package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.controller

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.support.ServletUriComponentsBuilder
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.config.ReferralBadIDException
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.config.ReferralNotFoundException
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.DraftReferralDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.service.ReferralService
import java.util.UUID
import javax.validation.Valid

@RestController
class ReferralController(private val referralService: ReferralService) {

  @PostMapping("/draft-referral")
  fun createDraftReferral(): ResponseEntity<DraftReferralDTO> {
    val referral = referralService.createDraftReferral()
    val location = ServletUriComponentsBuilder
      .fromCurrentRequest()
      .path("/{id}")
      .buildAndExpand(referral.id)
      .toUri()

    return ResponseEntity
      .created(location)
      .body(DraftReferralDTO.from(referral))
  }

  @GetMapping("/draft-referral/{id}")
  fun getDraftReferralByID(@PathVariable id: String): DraftReferralDTO {
    val uuid = parseID(id)

    return referralService.getDraftReferral(uuid)
      ?.let { DraftReferralDTO.from(it) }
      ?: throw ReferralNotFoundException(uuid)
  }

  @PatchMapping("/draft-referral/{id}")
  fun patchDraftReferralByID(@PathVariable id: String, RequestBody partialUpdate: DraftReferralDTO): DraftReferralDTO {
    val uuid = parseID(id)

    val referralToUpdate = referralService.getDraftReferral(uuid)
      ?: throw ReferralNotFoundException(uuid)

    val updatedReferral = referralService.updateDraftReferral(referralToUpdate, partialUpdate)
    return DraftReferralDTO.from(updatedReferral)
  }

  private fun parseID(id: String): UUID {
    return try {
      UUID.fromString(id)
    } catch (e: IllegalArgumentException) {
      throw ReferralBadIDException(id)
    }
  }
}
