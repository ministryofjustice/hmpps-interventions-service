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
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ReferralServiceUserData
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
  private val probationPractitionerDetailsFactory = ProbationPractitionerDetailsFactory(em)

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
    referralDetail: ReferralDetails? = null,
    personCurrentLocationType: PersonCurrentLocationType? = null,
    personCustodyPrisonId: String? = null,
    expectedReleaseDate: LocalDate? = null,
    ndeliusPPName: String? = "Bob",
    ndeliusPPEmailAddress: String? = "bob@example.com",
    ndeliusPDU: String? = "Hackney and City",
    ppName: String? = "Alice",
    ppEmailAddress: String? = "alice@example.com",
    ppProbationOffice: String? = "London",
    ppPdu: String? = "East Sussex",
    hasValidDeliusPPDetails: Boolean = false,
    isReferralReleasingIn12Weeks: Boolean = false,
    hasMainPointOfContactDetails: Boolean = false,
    roleOrJobTitle: String? = "Probation Practitioner",
    ppEstablishment: String? = "aaa",
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
      referralDetails = referralDetail,
      personCurrentLocationType = personCurrentLocationType,
      personCustodyPrisonId = personCustodyPrisonId,
      expectedReleaseDate = expectedReleaseDate,
      ndeliusPPName = ndeliusPPName,
      ndeliusPPEmailAddress = ndeliusPPEmailAddress,
      ndeliusPDU = ndeliusPDU,
      ppName = ppName,
      ppEmailAddress = ppEmailAddress,
      ppProbationOffice = ppProbationOffice,
      ppPdu = ppPdu,
      hasValidDeliusPPDetails = hasValidDeliusPPDetails,
      isReferralReleasingIn12Weeks = isReferralReleasingIn12Weeks,
      hasMainPointOfContactDetails = hasMainPointOfContactDetails,
      roleOrJobTitle = roleOrJobTitle,
      ppEstablishment = ppEstablishment,
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
    serviceUserData: ReferralServiceUserData? = null,
    complexityLevelIds: MutableMap<UUID, UUID>? = null,
    createDraft: Boolean = true,
    accessibilityNeeds: String? = null,
    additionalNeedsInformation: String? = null,
    completionDeadline: LocalDate? = null,
    maximumEnforceableDays: Int? = null,
    referralDetail: ReferralDetails? = null,
    needsInterpreter: Boolean? = null,
    interpreterLanguage: String? = null,
    probationPractitionerDetails: ProbationPractitionerDetails? = null,
    ppEstablishment: String? = null,
  ): Referral {
    if (createDraft) {
      createDraft(
        id = id,
        createdAt = createdAt,
        createdBy = createdBy,
        serviceUserCRN = serviceUserCRN,
        intervention = intervention,
        ppEstablishment = ppEstablishment,
      )
    }
    val referral = createReferral(
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
      assignments = assignments,
      supplierAssessment = supplierAssessment,
      additionalRiskInformationUpdatedAt = additionalRiskInformationUpdatedAt,
      serviceUserData = serviceUserData,
      complexityLevelIds = complexityLevelIds,
      accessibilityNeeds = accessibilityNeeds,
      additionalNeedsInformation = additionalNeedsInformation,
      needsInterpreter = needsInterpreter,
      interpreterLanguage = interpreterLanguage,
      probationPractitionerDetails = probationPractitionerDetails,
    )
    val probationPractitionerDetails = probationPractitionerDetailsFactory.create(referral = referral)
    referral.probationPractitionerDetails = probationPractitionerDetails
    save(referral)
    return referral
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
      ReferralAssignment(OffsetDateTime.now(), authUserFactory.createSP(), authUserFactory.createSP()),
    ),

    supplierAssessment: SupplierAssessment? = null,
    serviceUserData: ReferralServiceUserData? = null,
  ): Referral {
    createDraft(
      id = id,
      createdAt = createdAt,
      createdBy = createdBy,
      serviceUserCRN = serviceUserCRN,
      intervention = intervention,
      selectedServiceCategories = selectedServiceCategories,
    )
    val referral = createReferral(
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
      serviceUserData = serviceUserData,
    )
    val probationPractitionerDetails = probationPractitionerDetailsFactory.create(referral = referral)
    referral.probationPractitionerDetails = probationPractitionerDetails
    save(referral)
    return referral
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
    desiredOutcomes: List<DesiredOutcome> = emptyList(),

    assignments: List<ReferralAssignment> = emptyList(),

    endRequestedAt: OffsetDateTime? = OffsetDateTime.now(),
    endRequestedBy: AuthUser? = authUserFactory.create(),
    endRequestedReason: CancellationReason? = cancellationReasonFactory.create(),
    endRequestedComments: String? = null,

    concludedAt: OffsetDateTime? = null,

    endOfServiceReport: EndOfServiceReport? = null,
    ndeliusPPName: String? = "Bob",
    ndeliusPPEmailAddress: String? = "bob@example.com",
    ndeliusPDU: String? = "Hackney and City",
    ppName: String? = "Alice",
    ppEmailAddress: String? = "alice@example.com",
    ppProbationOffice: String? = "London",
    ppPdu: String? = "East Sussex",
  ): Referral {
    createDraft(
      id = id,
      createdAt = createdAt,
      createdBy = createdBy,
      serviceUserCRN = serviceUserCRN,
      intervention = intervention,
      selectedServiceCategories = selectedServiceCategories,
    )
    val referral = createReferral(
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
      desiredOutcomes = desiredOutcomes,

      assignments = assignments,

      endRequestedAt = endRequestedAt,
      endRequestedBy = endRequestedBy,
      endRequestedReason = endRequestedReason,
      endRequestedComments = endRequestedComments,

      concludedAt = concludedAt,

      endOfServiceReport = endOfServiceReport,
    )
    val probationPractitionerDetails = probationPractitionerDetailsFactory.create(
      referral = referral,
      nDeliusName = ndeliusPPName,
      nDeliusEmailAddress = ndeliusPPEmailAddress,
      nDeliusPdu = ndeliusPDU,
      name = ppName,
      emailAddress = ppEmailAddress,
      pdu = ppPdu,
      probationOffice = ppProbationOffice,
    )
    referral.probationPractitionerDetails = probationPractitionerDetails
    save(referral)
    return referral
  }
}
