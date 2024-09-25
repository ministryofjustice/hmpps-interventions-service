package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.controller

import mu.KLogging
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.NO_CONTENT
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.authorization.UserMapper
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.AmendComplexityLevelDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.AmendDesiredOutcomesDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.AmendExpectedReleaseDateDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.AmendNeedsAndRequirementsDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.AmendPrisonEstablishmentDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.AmendProbationOfficeDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.AmendProbationPractitionerNameDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.ChangelogDetailsDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.ChangelogValuesDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.AmendReferralService
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.HMPPSAuthService
import java.util.UUID

@RestController
@PreAuthorize("hasRole('ROLE_PROBATION') or hasRole('ROLE_CRS_PROVIDER') or hasRole('ROLE_INTERVENTIONS_API_READ_ALL')")
class AmendReferralController(
  private val amendReferralService: AmendReferralService,
  private val hmppsAuthService: HMPPSAuthService,
  private val userMapper: UserMapper,
) {
  companion object : KLogging()

  @PostMapping("/sent-referral/{referralId}/service-category/{serviceCategoryId}/amend-complexity-level")
  fun updateComplexityLevel(
    @PathVariable referralId: UUID,
    @PathVariable serviceCategoryId: UUID,
    @RequestBody complexityLevel: AmendComplexityLevelDTO,
    authentication: JwtAuthenticationToken,
  ): ResponseEntity<Any> {
    amendReferralService.updateComplexityLevel(referralId, complexityLevel, serviceCategoryId, authentication)
    return ResponseEntity(NO_CONTENT)
  }

  @PostMapping("/sent-referral/{referralId}/service-category/{serviceCategoryId}/amend-desired-outcomes")
  fun amendDesiredOutcomes(
    authentication: JwtAuthenticationToken,
    @PathVariable referralId: UUID,
    @PathVariable serviceCategoryId: UUID,
    @RequestBody request: AmendDesiredOutcomesDTO,
  ): ResponseEntity<Any> {
    amendReferralService.updateReferralDesiredOutcomes(referralId, request, authentication, serviceCategoryId)
    return ResponseEntity(NO_CONTENT)
  }

  @PostMapping("/sent-referral/{referralId}/amend-needs-and-requirements/{needsAndRequirementsType}")
  fun amendNeedsAndRequirements(
    authentication: JwtAuthenticationToken,
    @PathVariable referralId: UUID,
    @PathVariable needsAndRequirementsType: String,
    @RequestBody request: AmendNeedsAndRequirementsDTO,
  ): ResponseEntity<Any> {
    when (needsAndRequirementsType) {
      "additional-responsibilities" -> amendReferralService.amendCaringOrEmploymentResponsibilities(referralId, request, authentication)
      "accessibility-needs" -> amendReferralService.amendAccessibilityNeeds(referralId, request, authentication)
      "identify-needs" -> amendReferralService.amendIdentifyNeeds(referralId, request, authentication)
      "interpreter-required" -> amendReferralService.amendInterpreterRequired(referralId, request, authentication)
      else -> return ResponseEntity("No accepted needs and requirements type", BAD_REQUEST)
    }
    return ResponseEntity(NO_CONTENT)
  }

  @PostMapping("/sent-referral/{referralId}/amend-prison-establishment")
  fun amendPrisonEstablishment(
    authentication: JwtAuthenticationToken,
    @PathVariable referralId: UUID,
    @RequestBody request: AmendPrisonEstablishmentDTO,
  ): ResponseEntity<Any> {
    val user = userMapper.fromToken(authentication)
    amendReferralService.amendPrisonEstablishment(referralId, request, authentication, user)
    return ResponseEntity(NO_CONTENT)
  }

  @PostMapping("/sent-referral/{referralId}/amend-expected-release-date")
  fun amendExpectedReleaseDate(
    authentication: JwtAuthenticationToken,
    @PathVariable referralId: UUID,
    @RequestBody request: AmendExpectedReleaseDateDTO,
  ): ResponseEntity<Any> {
    val user = userMapper.fromToken(authentication)
    amendReferralService.amendExpectedReleaseDate(referralId, request, authentication, user)
    return ResponseEntity(NO_CONTENT)
  }

  @PostMapping("/sent-referral/{referralId}/amend-expected-probation-office")
  fun amendExpectedProbationOffice(
    authentication: JwtAuthenticationToken,
    @PathVariable referralId: UUID,
    @RequestBody request: AmendProbationOfficeDTO,
  ): ResponseEntity<Any> {
    val user = userMapper.fromToken(authentication)
    amendReferralService.amendExpectedProbationOffice(referralId, request, authentication, user)
    return ResponseEntity(NO_CONTENT)
  }

  @PostMapping("/sent-referral/{referralId}/amend-pp-probation-office")
  fun amendProbationPractitionerProbationOffice(
    authentication: JwtAuthenticationToken,
    @PathVariable referralId: UUID,
    @RequestBody request: AmendProbationOfficeDTO,
  ): ResponseEntity<Any> {
    val user = userMapper.fromToken(authentication)
    amendReferralService.amendProbationPractitionerProbationOffice(referralId, request, authentication, user)
    return ResponseEntity(NO_CONTENT)
  }

  @PostMapping("/sent-referral/{referralId}/amend-probation-practitioner-name")
  fun amendProbationPractitionerName(
    authentication: JwtAuthenticationToken,
    @PathVariable referralId: UUID,
    @RequestBody request: AmendProbationPractitionerNameDTO,
  ): ResponseEntity<Any> {
    val user = userMapper.fromToken(authentication)
    amendReferralService.amendProbationPractitionerName(referralId, request, authentication, user)
    return ResponseEntity(NO_CONTENT)
  }

  @GetMapping("/sent-referral/{referralId}/change-log")
  fun getChangelog(
    @PathVariable referralId: UUID,
    authentication: JwtAuthenticationToken,
  ): List<ChangelogValuesDTO>? {
    val referral = amendReferralService.getSentReferralForAuthenticatedUser(referralId, authentication)
    return amendReferralService.getListOfChangeLogEntries(referral).map {
      ChangelogValuesDTO.from(it, hmppsAuthService.getUserDetail(it.changedBy))
    }
  }

  @GetMapping("/sent-referral/change-log/{changeLogId}")
  fun getChangelogDetails(
    @PathVariable changeLogId: UUID,
    authentication: JwtAuthenticationToken,
  ): ChangelogDetailsDTO {
    val changeLog = amendReferralService.getChangeLogById(changeLogId, authentication)
    return ChangelogDetailsDTO.from(changeLog, hmppsAuthService.getUserDetail(changeLog.changelog.changedBy))
  }
}
