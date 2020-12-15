package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.controller

import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.ResponseEntity
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.support.ServletUriComponentsBuilder
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.DraftReferral
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Referral
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ReferralRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.service.ReferralService
import java.util.UUID

@RestController
class ReferralController(private val repository: ReferralRepository) {
  @PostMapping("/draft-referral")
  fun createDraftReferral(authentication: JwtAuthenticationToken): ResponseEntity<Any> {
    val userId = authentication.token.getClaim<String?>("user_id")
      ?: return ResponseEntity.badRequest().body("no user_id claim in auth token")

    val referral = Referral(createdByUserId = userId)
    repository.save(referral)

    val location = ServletUriComponentsBuilder
      .fromCurrentRequest()
      .path("/{id}")
      .buildAndExpand(referral.id)
      .toUri()

    return ResponseEntity
      .created(location)
      .body(DraftReferral(referral))
  }

  @GetMapping("/draft-referral/{id}")
  fun getDraftReferralByID(@PathVariable id: String): ResponseEntity<Any> {
    val uuid = try {
      UUID.fromString(id)
    } catch (e: IllegalArgumentException) {
      return ResponseEntity.badRequest().body("malformed id")
    }

    return repository.findByIdOrNull(uuid)
      ?.let { ResponseEntity.ok(DraftReferral(it)) }
      ?: ResponseEntity.notFound().build()
  }

  @PatchMapping("/draft-referral/{id}")
  fun patchDraftReferralByID(@PathVariable id: String, @RequestBody partialUpdate: DraftReferral): ResponseEntity<Any> {
    val uuid = try {
      UUID.fromString(id)
    } catch (e: IllegalArgumentException) {
      return ResponseEntity.badRequest().body("malformed id")
    }

    return repository.findByIdOrNull(uuid)
      ?.let {
        ReferralService.updateReferral(it, partialUpdate)
        repository.save(it)
        ResponseEntity.ok(it)
      }
      ?: ResponseEntity.notFound().build()
  }
}
