package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util

import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ActionPlan
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AuthUser
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.CancellationReason
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.DesiredOutcome
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.DraftReferral
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.EndOfServiceReport
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Intervention
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.PersonCurrentLocationType
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ProbationPractitionerDetails
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Referral
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ReferralAssignment
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ReferralDetails
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.SelectedDesiredOutcomesMapping
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ServiceCategory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ServiceUserData
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.SupplierAssessment
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

open class BaseReferralFactory(em: TestEntityManager? = null) : EntityFactory(em) {

  protected fun createReferral(
    id: UUID,
    createdAt: OffsetDateTime,
    createdBy: AuthUser,
    serviceUserCRN: String,
    intervention: Intervention,
    relevantSentenceId: Long? = null,
    desiredOutcomes: List<DesiredOutcome> = emptyList(),
    serviceUserData: ServiceUserData? = null,
    actionPlans: MutableList<ActionPlan>? = null,
    selectedServiceCategories: MutableSet<ServiceCategory>? = null,
    complexityLevelIds: MutableMap<UUID, UUID>? = null,
    additionalRiskInformation: String? = null,
    additionalRiskInformationUpdatedAt: OffsetDateTime? = null,
    hasAdditionalResponsibilities: Boolean? = false,
    whenUnavailable: String? = null,
    sentAt: OffsetDateTime? = null,
    sentBy: AuthUser? = null,
    referenceNumber: String? = null,
    supplementaryRiskId: UUID? = null,
    assignments: List<ReferralAssignment> = emptyList(),
    endRequestedAt: OffsetDateTime? = null,
    endRequestedBy: AuthUser? = null,
    endRequestedReason: CancellationReason? = null,
    endRequestedComments: String? = null,
    referralDetails: ReferralDetails? = null,
    concludedAt: OffsetDateTime? = null,
    supplierAssessment: SupplierAssessment? = null,
    endOfServiceReport: EndOfServiceReport? = null,
    accessibilityNeeds: String? = null,
    additionalNeedsInformation: String? = null,
    needsInterpreter: Boolean? = null,
    interpreterLanguage: String? = null,
    completionDeadline: LocalDate? = null,
    maximumEnforceableDays: Int? = null,
    probationPractitionerDetails: ProbationPractitionerDetails? = null,
  ): Referral {
    val referral = save(
      Referral(
        id = id,
        createdAt = createdAt,
        createdBy = createdBy,
        serviceUserCRN = serviceUserCRN,
        intervention = intervention,
        relevantSentenceId = relevantSentenceId,
        serviceUserData = serviceUserData,
        actionPlans = actionPlans,
        selectedServiceCategories = selectedServiceCategories,
        complexityLevelIds = complexityLevelIds,
        additionalRiskInformation = additionalRiskInformation,
        additionalRiskInformationUpdatedAt = additionalRiskInformationUpdatedAt,
        hasAdditionalResponsibilities = hasAdditionalResponsibilities,
        whenUnavailable = whenUnavailable,
        sentAt = sentAt,
        sentBy = sentBy,
        referenceNumber = referenceNumber,
        supplementaryRiskId = supplementaryRiskId,
        assignments = assignments.toMutableList(),
        endRequestedAt = endRequestedAt,
        endRequestedBy = endRequestedBy,
        endRequestedReason = endRequestedReason,
        endRequestedComments = endRequestedComments,
        concludedAt = concludedAt,
        endOfServiceReport = endOfServiceReport,
        supplierAssessment = supplierAssessment,
        accessibilityNeeds = accessibilityNeeds,
        additionalNeedsInformation = additionalNeedsInformation,
        needsInterpreter = needsInterpreter,
        interpreterLanguage = interpreterLanguage,
        probationPractitionerDetails = probationPractitionerDetails,
        referralDetailsHistory = if (referralDetails != null) {
          setOf(
            referralDetails.let {
              ReferralDetails(
                UUID.randomUUID(),
                null,
                it!!.referralId,
                createdAt,
                createdBy.id,
                "initial referral details",
                it.completionDeadline,
                it.furtherInformation,
                it.maximumEnforceableDays,
              )
            },
          )
        } else {
          emptySet()
        },

      ),
    )
    referral.selectedDesiredOutcomes = desiredOutcomes.map { SelectedDesiredOutcomesMapping(it.serviceCategoryId, it.id) }.toMutableList()
    save(referral)
    return referral
  }

  protected fun createDraftReferral(
    id: UUID,
    createdAt: OffsetDateTime,
    createdBy: AuthUser,
    serviceUserCRN: String,
    intervention: Intervention,
    relevantSentenceId: Long? = null,
    referralDetails: ReferralDetails? = null,
    desiredOutcomes: List<DesiredOutcome> = emptyList(),
    serviceUserData: ServiceUserData? = null,
    selectedServiceCategories: MutableSet<ServiceCategory>? = null,
    complexityLevelIds: MutableMap<UUID, UUID>? = null,
    additionalRiskInformation: String? = null,
    additionalRiskInformationUpdatedAt: OffsetDateTime? = null,
    personCurrentLocationType: PersonCurrentLocationType? = null,
    personCustodyPrisonId: String? = null,
    expectedReleaseDate: LocalDate? = null,
    probationOffice: String? = null,
  ): DraftReferral {
    val draftReferral = DraftReferral(
      id = id,
      createdAt = createdAt,
      createdBy = createdBy,
      serviceUserCRN = serviceUserCRN,
      intervention = intervention,
      relevantSentenceId = relevantSentenceId,
      personCurrentLocationType = personCurrentLocationType,
      personCustodyPrisonId = personCustodyPrisonId,
      expectedReleaseDate = expectedReleaseDate,
      serviceUserData = serviceUserData,
      selectedServiceCategories = selectedServiceCategories,
      complexityLevelIds = complexityLevelIds,
      additionalRiskInformation = additionalRiskInformation,
      additionalRiskInformationUpdatedAt = additionalRiskInformationUpdatedAt,
      probationOffice = probationOffice,
      referralDetailsHistory = if (referralDetails != null) {
        setOf(
          referralDetails.let {
            ReferralDetails(
              UUID.randomUUID(),
              null,
              it!!.referralId,
              createdAt,
              createdBy.id,
              "initial referral details",
              it.completionDeadline,
              it.furtherInformation,
              it.maximumEnforceableDays,
            )
          },
        )
      } else {
        emptySet()
      },
    )

    save(draftReferral)
    draftReferral.selectedDesiredOutcomes =
      desiredOutcomes.map { SelectedDesiredOutcomesMapping(it.serviceCategoryId, it.id) }.toMutableList()
    save(draftReferral)
    return draftReferral
  }
}
