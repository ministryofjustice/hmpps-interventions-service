package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.controller

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.support.ServletUriComponentsBuilder
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.DraftReferralDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.ServiceCategoryDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.service.ReferralService
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.service.ServiceCategoryService
import java.util.UUID

@RestController
class ReferralController(
  private val referralService: ReferralService,
  private val serviceCategoryService: ServiceCategoryService
) {

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
  fun getDraftReferralByID(@PathVariable id: String): ResponseEntity<Any> {
    val uuid = try {
      UUID.fromString(id)
    } catch (e: IllegalArgumentException) {
      return ResponseEntity.badRequest().body("malformed id")
    }

    return referralService.getDraftReferral(uuid)
      ?.let { ResponseEntity.ok(DraftReferralDTO.from(it)) }
      ?: ResponseEntity.notFound().build()
  }

  @PatchMapping("/draft-referral/{id}")
  fun patchDraftReferralByID(@PathVariable id: String, @RequestBody partialUpdate: DraftReferralDTO): ResponseEntity<Any> {
    val uuid = try {
      UUID.fromString(id)
    } catch (e: IllegalArgumentException) {
      return ResponseEntity.badRequest().body("malformed id")
    }

    val referralToUpdate = referralService.getDraftReferral(uuid)
      ?: return ResponseEntity.notFound().build()

    val updatedReferral = referralService.updateDraftReferral(referralToUpdate, partialUpdate)
    return ResponseEntity.ok(DraftReferralDTO.from(updatedReferral))
  }

  @GetMapping("/draft-referrals")
  fun getDraftReferralsCreatedByUserID(@RequestParam userID: String): List<DraftReferralDTO> {
    return referralService.getDraftReferralsCreatedByUserID(userID)
  }

  @GetMapping("/service-category/{id}")
  fun getServiceCategoryByID(@PathVariable id: String): ResponseEntity<Any> {
    val uuid = try {
      UUID.fromString(id)
    } catch (e: IllegalArgumentException) {
      return ResponseEntity.badRequest().body("malformed id")
    }

    return serviceCategoryService.getServiceCategoryByID(uuid)
      ?.let { ResponseEntity.ok(ServiceCategoryDTO.from(it)) }
      ?: ResponseEntity.notFound().build()
  }
}
