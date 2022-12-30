package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util

import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ActionPlan
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AuthUser
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.CancellationReason
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.DesiredOutcome
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.DraftReferral
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.EndOfServiceReport
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Intervention
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Referral
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ReferralAssignment
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ReferralDetails
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ServiceCategory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ServiceUserData
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.SupplierAssessment
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class ReferralFactory(em: TestEntityManager? = null) : BaseReferralFactory(em) {
  private val authUserFactory = AuthUserFactory(em)
  private val interventionFactory = InterventionFactory(em)
  private val cancellationReasonFactory = CancellationReasonFactory(em)
  private val referalDetailsFactory = ReferralDetailsFactory(em)

  fun createDraft(
    id: UUID = UUID.randomUUID(),
    createdAt: OffsetDateTime = OffsetDateTime.now(),
    createdBy: AuthUser = authUserFactory.create(),
    serviceUserCRN: String = "X123456",
    intervention: Intervention = interventionFactory.create(),
    selectedServiceCategories: MutableSet<ServiceCategory>? = null,
    desiredOutcomes: List<DesiredOutcome> = emptyList(),
    serviceUserData: ServiceUserData? = null,
    complexityLevelIds: MutableMap<UUID, UUID>? = null,
    additionalRiskInformation: String? = null,
    additionalRiskInformationUpdatedAt: OffsetDateTime? = null,
    completionDeadline: LocalDate? = null,
    maximumEnforceableDays: Int? = null,
    referralDetail: ReferralDetails? = null
  ): DraftReferral {
    return createDraftReferral(
      id = id,
      createdAt = createdAt,
      createdBy = createdBy,
      serviceUserCRN = serviceUserCRN,
      intervention = intervention,
      selectedServiceCategories = selectedServiceCategories,
      desiredOutcomes = desiredOutcomes,
      serviceUserData = serviceUserData,
      complexityLevelIds = complexityLevelIds,
      additionalRiskInformation = additionalRiskInformation,
      additionalRiskInformationUpdatedAt = additionalRiskInformationUpdatedAt,
      referralDetails = referralDetail
    )
  }

  fun createSent(
    id: UUID = UUID.randomUUID(),
    createdAt: OffsetDateTime = OffsetDateTime.now(),
    createdBy: AuthUser = authUserFactory.create(),
    serviceUserCRN: String = "X123456",
    relevantSentenceId: Long = 1234567L,
    intervention: Intervention = interventionFactory.create(),
    selectedServiceCategories: MutableSet<ServiceCategory>? = null,
    desiredOutcomes: List<DesiredOutcome> = emptyList(),
    actionPlans: MutableList<ActionPlan>? = mutableListOf(),
    hasAdditionalResponsibilities: Boolean? = false,
    whenUnavailable: String? = null,
    sentAt: OffsetDateTime? = OffsetDateTime.now(),
    sentBy: AuthUser = authUserFactory.create(),
    referenceNumber: String? = "JS18726AC",
    supplementaryRiskId: UUID = UUID.randomUUID(),
    additionalRiskInformationUpdatedAt: OffsetDateTime? = null,
    assignments: List<ReferralAssignment> = emptyList(),
    supplierAssessment: SupplierAssessment? = null,
    serviceUserData: ServiceUserData? = null,
    complexityLevelIds: MutableMap<UUID, UUID>? = null,
    createDraft: Boolean = true,
    accessibilityNeeds: String? = null,
    additionalNeedsInformation: String? = null,
    completionDeadline: LocalDate? = null,
    maximumEnforceableDays: Int? = null,
    referralDetail: ReferralDetails? = null,
    needsInterpreter: Boolean? = null,
    interpreterLanguage: String? = null,

  ): Referral {
    if (createDraft) {
      createDraft(
        id = id,
        createdAt = createdAt,
        createdBy = createdBy,
        serviceUserCRN = serviceUserCRN,
        intervention = intervention,
      )
    }
    return createReferral(
      id = id,
      createdAt = createdAt,
      createdBy = createdBy,
      serviceUserCRN = serviceUserCRN,
      relevantSentenceId = relevantSentenceId,
      intervention = intervention,
      selectedServiceCategories = selectedServiceCategories,
      desiredOutcomes = desiredOutcomes,
      actionPlans = actionPlans,
      sentAt = sentAt,
      sentBy = sentBy,
      referenceNumber = referenceNumber,
      supplementaryRiskId = supplementaryRiskId,
      referralDetails = referralDetail,
      hasAdditionalResponsibilities = hasAdditionalResponsibilities,
      whenUnavailable = whenUnavailable,
      completionDeadline = completionDeadline,
      maximumEnforceableDays = maximumEnforceableDays,
      assignments = assignments,
      supplierAssessment = supplierAssessment,
      additionalRiskInformationUpdatedAt = additionalRiskInformationUpdatedAt,
      serviceUserData = serviceUserData,
      complexityLevelIds = complexityLevelIds,
      accessibilityNeeds = accessibilityNeeds,
      additionalNeedsInformation = additionalNeedsInformation,
      needsInterpreter = needsInterpreter,
      interpreterLanguage = interpreterLanguage,
    )
  }

  fun createAssigned(
    id: UUID = UUID.randomUUID(),
    createdAt: OffsetDateTime = OffsetDateTime.now(),
    createdBy: AuthUser = authUserFactory.create(),
    serviceUserCRN: String = "X123456",
    relevantSentenceId: Long = 1234567L,
    intervention: Intervention = interventionFactory.create(),
    selectedServiceCategories: MutableSet<ServiceCategory>? = null,
    desiredOutcomes: List<DesiredOutcome> = emptyList(),
    actionPlans: MutableList<ActionPlan>? = mutableListOf(),

    sentAt: OffsetDateTime = OffsetDateTime.now(),
    sentBy: AuthUser = authUserFactory.create(),
    referenceNumber: String? = "JS18726AC",
    supplementaryRiskId: UUID = UUID.randomUUID(),

    assignments: List<ReferralAssignment> = listOf(
      ReferralAssignment(OffsetDateTime.now(), authUserFactory.createSP(), authUserFactory.createSP())
    ),

    supplierAssessment: SupplierAssessment? = null,
    serviceUserData: ServiceUserData? = null,
  ): Referral {
    createDraft(
      id = id,
      createdAt = createdAt,
      createdBy = createdBy,
      serviceUserCRN = serviceUserCRN,
      intervention = intervention,
      selectedServiceCategories = selectedServiceCategories,
    )
    return createReferral(
      id = id,
      createdAt = createdAt,
      createdBy = createdBy,
      serviceUserCRN = serviceUserCRN,
      relevantSentenceId = relevantSentenceId,
      intervention = intervention,
      selectedServiceCategories = selectedServiceCategories,
      desiredOutcomes = desiredOutcomes,
      actionPlans = actionPlans,

      sentAt = sentAt,
      sentBy = sentBy,
      referenceNumber = referenceNumber,
      supplementaryRiskId = supplementaryRiskId,

      assignments = assignments,
      supplierAssessment = supplierAssessment,
      serviceUserData = serviceUserData
    )
  }

  fun createEnded(
    id: UUID = UUID.randomUUID(),
    createdAt: OffsetDateTime = OffsetDateTime.now(),
    createdBy: AuthUser = authUserFactory.create(),
    serviceUserCRN: String = "X123456",
    intervention: Intervention = interventionFactory.create(),
    selectedServiceCategories: MutableSet<ServiceCategory>? = null,
    sentAt: OffsetDateTime = OffsetDateTime.now(),
    sentBy: AuthUser = authUserFactory.create(),
    referenceNumber: String? = "JS18726AC",
    relevantSentenceId: Long? = 123456L,
    supplementaryRiskId: UUID = UUID.randomUUID(),
    actionPlans: MutableList<ActionPlan>? = mutableListOf(),
    supplierAssessment: SupplierAssessment? = null,

    assignments: List<ReferralAssignment> = emptyList(),

    endRequestedAt: OffsetDateTime? = OffsetDateTime.now(),
    endRequestedBy: AuthUser? = authUserFactory.create(),
    endRequestedReason: CancellationReason? = cancellationReasonFactory.create(),
    endRequestedComments: String? = null,

    concludedAt: OffsetDateTime? = null,

    endOfServiceReport: EndOfServiceReport? = null,
  ): Referral {
    createDraft(
      id = id,
      createdAt = createdAt,
      createdBy = createdBy,
      serviceUserCRN = serviceUserCRN,
      intervention = intervention,
      selectedServiceCategories = selectedServiceCategories,
    )
    return createReferral(
      id = id,
      createdAt = createdAt,
      createdBy = createdBy,
      serviceUserCRN = serviceUserCRN,
      intervention = intervention,
      selectedServiceCategories = selectedServiceCategories,
      actionPlans = actionPlans,
      supplierAssessment = supplierAssessment,

      sentAt = sentAt,
      sentBy = sentBy,
      referenceNumber = referenceNumber,
      supplementaryRiskId = supplementaryRiskId,
      relevantSentenceId = relevantSentenceId,

      assignments = assignments,

      endRequestedAt = endRequestedAt,
      endRequestedBy = endRequestedBy,
      endRequestedReason = endRequestedReason,
      endRequestedComments = endRequestedComments,

      concludedAt = concludedAt,

      endOfServiceReport = endOfServiceReport,
    )
  }
}
