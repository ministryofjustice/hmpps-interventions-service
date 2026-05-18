package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.SASReferralDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.SASReferralService

@PreAuthorize("hasRole('ROLE_SAS_RM__REFERRAL_RO')")
@RestController
class SASReferralController(
  private val sasReferralService: SASReferralService,
) {

  @Operation(
    summary = "Retrieve all referrals for a given CRN (Case Reference Number)",
    description = """Returns a list of referral summaries for the given CRN.
      Each referral includes a status that can be one of:
      - DRAFT: referral created but not yet sent to a service provider
      - LIVE: referral sent to a service provider and currently active
      - COMPLETED: referral concluded with an end-of-service report submitted
      - WITHDRAWN: referral withdrawn by the probation practitioner""",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Successfully retrieved accommodation referrals for the CRN",
        content = [Content(array = ArraySchema(schema = Schema(implementation = SASReferralDTO::class)))],
      ),
      ApiResponse(responseCode = "401", description = "Unauthorised – missing or invalid JWT token"),
      ApiResponse(responseCode = "403", description = "Forbidden – caller does not have ROLE_SAS_RM__REFERRAL_RO"),
      ApiResponse(responseCode = "404", description = "No referrals found for the given CRN"),
    ],
  )
  @GetMapping("/sas-referral-details/{crn}")
  fun getReferralDetails(
    @Parameter(description = "Case Reference Number (CRN) of the service user", required = true, example = "X123456")
    @PathVariable crn: String,
  ): List<SASReferralDTO> {
    val referrals = sasReferralService.getReferralsByCrn(crn)
    if (referrals.isEmpty()) {
      throw ResponseStatusException(HttpStatus.NOT_FOUND, "no referrals found for crn=$crn")
    }
    return referrals
  }
}
