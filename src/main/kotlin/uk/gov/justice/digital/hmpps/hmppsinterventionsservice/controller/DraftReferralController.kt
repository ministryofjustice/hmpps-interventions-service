package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.controller

import com.fasterxml.jackson.annotation.JsonView
import mu.KLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.server.ServerWebInputException
import org.springframework.web.servlet.support.ServletUriComponentsBuilder
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.authorization.UserMapper
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.CreateReferralRequestDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.DraftOasysRiskInformationDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.DraftReferralDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.SelectedDesiredOutcomesDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.SentReferralDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.SetComplexityLevelRequestDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.Views
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.DraftReferral
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.DraftOasysRiskInformationService
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.DraftReferralService
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.ReferralConcluder
import java.util.UUID
import javax.persistence.EntityNotFoundException

@RestController
class DraftReferralController(
  private val draftReferralService: DraftReferralService,
  private val referralConcluder: ReferralConcluder,
  private val userMapper: UserMapper,
  private val draftOasysRiskInformationService: DraftOasysRiskInformationService
) {
  companion object : KLogging()

  @JsonView(Views.SentReferral::class)
  @PostMapping("/draft-referral/{id}/send")
  fun sendDraftReferral(@PathVariable id: UUID, authentication: JwtAuthenticationToken): ResponseEntity<SentReferralDTO> {
    val user = userMapper.fromToken(authentication)

    val draftReferral = getDraftReferralForAuthenticatedUser(authentication, id)

    val sentReferral = draftReferralService.sendDraftReferral(draftReferral, user)

    val location = ServletUriComponentsBuilder
      .fromCurrentContextPath()
      .path("/sent-referral/{id}")
      .buildAndExpand(sentReferral.id)
      .toUri()

    return ResponseEntity
      .created(location)
      .body(SentReferralDTO.from(sentReferral, referralConcluder.requiresEndOfServiceReportCreation(sentReferral), draftReferral))
  }

  @PostMapping("/draft-referral")
  fun createDraftReferral(@RequestBody createReferralRequestDTO: CreateReferralRequestDTO, authentication: JwtAuthenticationToken): ResponseEntity<DraftReferralDTO> {
    val user = userMapper.fromToken(authentication)

    val referral = try {
      draftReferralService.createDraftReferral(
        user,
        createReferralRequestDTO.serviceUserCrn,
        createReferralRequestDTO.interventionId,
      )
    } catch (e: EntityNotFoundException) {
      throw ServerWebInputException("invalid intervention id [id=${createReferralRequestDTO.interventionId}]")
    }

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
  fun getDraftReferralByID(@PathVariable id: UUID, authentication: JwtAuthenticationToken): DraftReferralDTO {
    return DraftReferralDTO.from(getDraftReferralForAuthenticatedUser(authentication, id))
  }

  @PatchMapping("/draft-referral/{id}")
  fun patchDraftReferralByID(@PathVariable id: UUID, @RequestBody partialUpdate: DraftReferralDTO, authentication: JwtAuthenticationToken): DraftReferralDTO {
    val referralToUpdate = getDraftReferralForAuthenticatedUser(authentication, id)

    val updatedReferral = draftReferralService.updateDraftReferral(referralToUpdate, partialUpdate)
    return DraftReferralDTO.from(updatedReferral)
  }

  @PatchMapping("/draft-referral/{id}/complexity-level")
  fun setDraftReferralComplexityLevel(authentication: JwtAuthenticationToken, @PathVariable id: UUID, @RequestBody request: SetComplexityLevelRequestDTO): DraftReferralDTO {
    val referral = getDraftReferralForAuthenticatedUser(authentication, id)
    val updatedReferral = draftReferralService.updateDraftReferralComplexityLevel(referral, request.serviceCategoryId, request.complexityLevelId)
    return DraftReferralDTO.from(updatedReferral)
  }

  @PatchMapping("/draft-referral/{id}/desired-outcomes")
  fun setDraftReferralDesiredOutcomes(
    authentication: JwtAuthenticationToken,
    @PathVariable id: UUID,
    @RequestBody request: SelectedDesiredOutcomesDTO
  ): DraftReferralDTO {
    val referral = getDraftReferralForAuthenticatedUser(authentication, id)
    val updatedReferral = draftReferralService.updateDraftReferralDesiredOutcomes(referral, request.serviceCategoryId, request.desiredOutcomesIds)
    return DraftReferralDTO.from(updatedReferral)
  }

  @PatchMapping("/draft-referral/{id}/oasys-risk-information")
  fun setDraftReferralOasysRiskInformation(
    authentication: JwtAuthenticationToken,
    @PathVariable id: UUID,
    @RequestBody request: DraftOasysRiskInformationDTO
  ): DraftOasysRiskInformationDTO {
    val referral = getDraftReferralForAuthenticatedUser(authentication, id)
    val user = userMapper.fromToken(authentication)
    val draftOasysRiskInformation = draftOasysRiskInformationService.updateDraftOasysRiskInformation(referral, request, user)
    return DraftOasysRiskInformationDTO.from(draftOasysRiskInformation)
  }

  @GetMapping("/draft-referral/{id}/oasys-risk-information")
  fun getDraftReferralOasysRiskInformation(
    authentication: JwtAuthenticationToken,
    @PathVariable id: UUID
  ): DraftOasysRiskInformationDTO {
    // check that user has access to referral
    val referral = getDraftReferralForAuthenticatedUser(authentication, id)
    return draftOasysRiskInformationService.getDraftOasysRiskInformation(referral.id)
      ?.let { DraftOasysRiskInformationDTO.from(it) }
      ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "draft oasys risk information not found [id=$id]")
  }

  @GetMapping("/draft-referrals")
  fun getDraftReferrals(authentication: JwtAuthenticationToken): List<DraftReferralDTO> {
    val user = userMapper.fromToken(authentication)

    return draftReferralService.getDraftReferralsForUser(user).map { DraftReferralDTO.from(it) }
  }

  private fun getDraftReferralForAuthenticatedUser(authentication: JwtAuthenticationToken, id: UUID): DraftReferral {
    val user = userMapper.fromToken(authentication)
    return draftReferralService.getDraftReferralForUser(id, user)
      ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "draft referral not found [id=$id]")
  }
}
