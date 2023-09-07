package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.controller

import ServiceProviderSentReferralSummaryDTO
import com.fasterxml.jackson.annotation.JsonView
import com.microsoft.applicationinsights.TelemetryClient
import jakarta.annotation.Nullable
import mu.KLogging
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.authorization.ClientApiAccessChecker
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.authorization.UserMapper
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.controller.mappers.CancellationReasonMapper
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.ActionPlanSummaryDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.DashboardType
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.EndReferralRequestDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.ReferralAssignmentDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.ReferralDetailsDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.SentReferralDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.SentReferralSummariesDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.ServiceCategoryFullDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.SupplierAssessmentDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.UpdateReferralDetailsDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.Views
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AuthUser
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.CancellationReason
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Referral
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.SentReferralSummary
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.SupplierAssessment
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.ActionPlanService
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.ReferralConcluder
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.ReferralService
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.ServiceCategoryService
import java.util.UUID

@RestController
class ReferralController(
  private val referralService: ReferralService,
  private val referralConcluder: ReferralConcluder,
  private val serviceCategoryService: ServiceCategoryService,
  private val userMapper: UserMapper,
  private val clientApiAccessChecker: ClientApiAccessChecker,
  private val cancellationReasonMapper: CancellationReasonMapper,
  private val actionPlanService: ActionPlanService,
  private val telemetryClient: TelemetryClient,
) {
  companion object : KLogging()

  @JsonView(Views.SentReferral::class)
  @PostMapping("/sent-referral/{id}/assign")
  fun assignSentReferral(
    @PathVariable id: UUID,
    @RequestBody referralAssignment: ReferralAssignmentDTO,
    authentication: JwtAuthenticationToken,
  ): SentReferralDTO {
    val sentReferral = getSentReferralForAuthenticatedUser(authentication, id)

    val assignedBy = userMapper.fromToken(authentication)
    val assignedTo = AuthUser(
      id = referralAssignment.assignedTo.userId,
      authSource = referralAssignment.assignedTo.authSource,
      userName = referralAssignment.assignedTo.username,
    )
    return SentReferralDTO.from(
      referralService.assignSentReferral(sentReferral, assignedBy, assignedTo),
      referralConcluder.requiresEndOfServiceReportCreation(sentReferral),
    )
  }

  @JsonView(Views.SentReferral::class)
  @GetMapping("/sent-referral/{id}")
  fun getSentReferral(@PathVariable id: UUID, authentication: JwtAuthenticationToken): SentReferralDTO {
    val referral = getSentReferralAuthenticatedRequest(authentication, id)
    return SentReferralDTO.from(referral, referralConcluder.requiresEndOfServiceReportCreation(referral))
  }

  @GetMapping("/sent-referrals/summaries")
  fun getSentReferralsSummaries(
    authentication: JwtAuthenticationToken,
    @Nullable
    @RequestParam(name = "concluded", required = false)
    concluded: Boolean?,
    @Nullable
    @RequestParam(name = "cancelled", required = false)
    cancelled: Boolean?,
    @Nullable
    @RequestParam(name = "unassigned", required = false)
    unassigned: Boolean?,
    @Nullable
    @RequestParam(name = "assignedTo", required = false)
    assignedToUserId: String?,
    @Nullable
    @RequestParam(name = "search", required = false)
    searchText: String?,
    @PageableDefault(page = 0, size = 50, sort = ["sentAt"]) page: Pageable,
  ): Page<SentReferralSummariesDTO> {
    val user = userMapper.fromToken(authentication)
    return (referralService.getSentReferralSummaryForUser(user, concluded, cancelled, unassigned, assignedToUserId, page, searchText) as Page<SentReferralSummary>).map { SentReferralSummariesDTO.from(it) }.also {
      telemetryClient.trackEvent(
        "PagedDashboardRequest",
        null,
        mutableMapOf("totalNumberOfReferrals" to it.totalElements.toDouble(), "pageSize" to page.pageSize.toDouble()),
      )
    }
  }

  @Deprecated(message = "This is a temporary solution to by-pass the extremely long wait times in production that occurs with /sent-referrals")
  @JsonView(Views.SentReferral::class)
  @GetMapping("/sent-referrals/summary/service-provider")
  fun getServiceProviderSentReferralsSummary(
    authentication: JwtAuthenticationToken,
    @Nullable
    @RequestParam(name = "dashboardType", required = false)
    dashboardTypeSelection: String?,
  ): List<ServiceProviderSentReferralSummaryDTO> {
    val user = userMapper.fromToken(authentication)
    val dashboardType = dashboardTypeSelection?.let { DashboardType.valueOf(it) }
    return referralService.getServiceProviderSummaries(user, dashboardType)
      .map { ServiceProviderSentReferralSummaryDTO.from(it) }.also {
        telemetryClient.trackEvent(
          "ServiceProviderReferralSummaryRequest",
          null,
          mutableMapOf("totalNumberOfReferrals" to it.size.toDouble()),
        )
      }
  }

  @JsonView(Views.SentReferral::class)
  @PostMapping("/sent-referral/{id}/end")
  fun endSentReferral(@PathVariable id: UUID, @RequestBody endReferralRequest: EndReferralRequestDTO, authentication: JwtAuthenticationToken): SentReferralDTO {
    val sentReferral = getSentReferralForAuthenticatedUser(authentication, id)

    val cancellationReason = cancellationReasonMapper.mapCancellationReasonIdToCancellationReason(endReferralRequest.reasonCode)

    val user = userMapper.fromToken(authentication)
    return SentReferralDTO.from(
      referralService.requestReferralEnd(sentReferral, user, cancellationReason, endReferralRequest.comments),
      referralConcluder.requiresEndOfServiceReportCreation(sentReferral),
    )
  }

  @GetMapping("/service-category/{id}")
  fun getServiceCategoryByID(@PathVariable id: UUID): ServiceCategoryFullDTO {
    return serviceCategoryService.getServiceCategoryByID(id)
      ?.let { ServiceCategoryFullDTO.from(it) }
      ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "service category not found [id=$id]")
  }

  @GetMapping("/referral-cancellation-reasons")
  fun getCancellationReasons(): List<CancellationReason> {
    return referralService.getCancellationReasons()
  }

  @GetMapping("sent-referral/{id}/supplier-assessment")
  fun getSupplierAssessmentAppointment(
    @PathVariable id: UUID,
    authentication: JwtAuthenticationToken,
  ): SupplierAssessmentDTO {
    val sentReferral = getSentReferralAuthenticatedRequest(authentication, id)

    val supplierAssessment = getSupplierAssessment(sentReferral)
    return SupplierAssessmentDTO.from(supplierAssessment)
  }

  @GetMapping("/sent-referral/{id}/approved-action-plans")
  fun getReferralApprovedActionPlans(
    @PathVariable id: UUID,
  ): List<ActionPlanSummaryDTO> {
    val actionPlans = actionPlanService.getApprovedActionPlansByReferral(id)
    return ActionPlanSummaryDTO.from(actionPlans)
  }

  @PostMapping("/sent-referral/{referralId}/referral-details")
  fun updateReferralDetails(
    @PathVariable referralId: UUID,
    @RequestBody referralDetails: UpdateReferralDetailsDTO,
    authentication: JwtAuthenticationToken,
  ): ReferralDetailsDTO? {
    val user = userMapper.fromToken(authentication)
    val referral = getSentReferralForAuthenticatedUser(authentication, referralId)
    return referralService.updateReferralDetails(referral, referralDetails, user)?.let {
      ReferralDetailsDTO.from(it)
    } ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "no updatable values present in request")
  }

  private fun getSupplierAssessment(sentReferral: Referral): SupplierAssessment {
    return sentReferral.supplierAssessment ?: throw ResponseStatusException(
      HttpStatus.NOT_FOUND,
      "Supplier assessment does not exist for referral[id=${sentReferral.id}]",
    )
  }

  private fun getSentReferralAuthenticatedRequest(authentication: JwtAuthenticationToken, id: UUID) =
    if (clientApiAccessChecker.isClientRequestWithReadAllRole(authentication)) {
      referralService.getSentReferral(id)
        ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "sent referral not found [id=$id]")
    } else {
      getSentReferralForAuthenticatedUser(authentication, id)
    }

  private fun getSentReferralForAuthenticatedUser(authentication: JwtAuthenticationToken, id: UUID): Referral {
    val user = userMapper.fromToken(authentication)
    return referralService.getSentReferralForUser(id, user)
      ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "sent referral not found [id=$id]")
  }
}
