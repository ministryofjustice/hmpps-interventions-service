package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util

import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ActionPlan
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AuthUser
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.DesiredOutcome
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Intervention
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.PersonCurrentLocationType
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Referral
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ReferralAssignment
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ReferralServiceUserData
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ServiceCategory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.SupplierAssessment
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ReferralSummary
import java.time.OffsetDateTime
import java.util.UUID

class ReferralSummariesFactory(em: TestEntityManager? = null) : BaseReferralFactory(em) {
  private val authUserFactory = AuthUserFactory(em)
  private val interventionFactory = InterventionFactory(em)

  fun createSent(
    id: UUID = UUID.randomUUID(),
    createdAt: OffsetDateTime = OffsetDateTime.now(),
    concludedAt: OffsetDateTime? = createdAt.plusHours(3),
    createdBy: AuthUser = authUserFactory.create(),
    serviceUserCRN: String = "X123456",
    relevantSentenceId: Long = 1234567L,
    intervention: Intervention = interventionFactory.create(),
    selectedServiceCategories: MutableSet<ServiceCategory>? = null,
    desiredOutcomes: List<DesiredOutcome> = emptyList(),
    actionPlans: MutableList<ActionPlan>? = mutableListOf(),
    sentAt: OffsetDateTime = OffsetDateTime.now(),
    sentBy: AuthUser = authUserFactory.create(),
    referenceNumber: String = "JS18726AC",
    supplementaryRiskId: UUID = UUID.randomUUID(),
    assignments: List<ReferralAssignment> = emptyList(),
    serviceUserData: ReferralServiceUserData? = null,
  ): ReferralSummary {
    val referral = createReferral(
      id = id,
      createdAt = createdAt,
      createdBy = createdBy,
      serviceUserCRN = serviceUserCRN,
      intervention = intervention,
      relevantSentenceId = relevantSentenceId,
      desiredOutcomes = desiredOutcomes,
      serviceUserData = serviceUserData,
      actionPlans = actionPlans,
      selectedServiceCategories = selectedServiceCategories,
      sentAt = sentAt,
      sentBy = sentBy,
      referenceNumber = referenceNumber,
      supplementaryRiskId = supplementaryRiskId,
      assignments = assignments,
      concludedAt = concludedAt,
    )

    return ReferralSummary(
      referralId = id,
      crn = serviceUserCRN,
      interventionTitle = intervention.title,
      sentAt = sentAt,
      dynamicFrameworkContractId = intervention.dynamicFrameworkContract.id,
      assignedToUserId = referral.currentAssignee?.id,
      assignedToUserName = referral.currentAssignee?.userName,
      assignedToAuthSource = referral.currentAssignee?.authSource,
      sentByUserId = sentBy.id,
      sentByUserName = sentBy.userName,
      sentByAuthSource = sentBy.authSource,
      referenceNumber = referenceNumber,
      serviceUserLastName = referral.serviceUserData?.lastName,
      serviceUserFirstName = referral.serviceUserData?.firstName,
      locationType = referral.referralLocation?.type,
      prisonId = referral.referralLocation?.prisonId,
      expectedReleaseDate = referral.referralLocation?.expectedReleaseDate,
      isReferralReleasingIn12Weeks = referral.referralLocation?.isReferralReleasingIn12Weeks,
      probationOffice = referral.probationPractitionerDetails?.probationOffice,
      pdu = referral.probationPractitionerDetails?.pdu,
      nDeliusPDU = referral.probationPractitionerDetails?.nDeliusPDU,
      serviceProviderName = referral.intervention.dynamicFrameworkContract.primeProvider.name,
      serviceProviderId = referral.intervention.dynamicFrameworkContract.primeProvider.id,
      concludedAt = concludedAt,
    )
  }

  fun createReferralSummaryWithReferral(
    id: UUID = UUID.randomUUID(),
    createdAt: OffsetDateTime = OffsetDateTime.now(),
    concludedAt: OffsetDateTime? = createdAt.plusHours(3),
    createdBy: AuthUser = authUserFactory.create(),
    serviceUserCRN: String = "X123456",
    relevantSentenceId: Long = 1234567L,
    intervention: Intervention = interventionFactory.create(),
    selectedServiceCategories: MutableSet<ServiceCategory>? = null,
    desiredOutcomes: List<DesiredOutcome> = emptyList(),
    actionPlans: MutableList<ActionPlan>? = mutableListOf(),
    sentAt: OffsetDateTime = OffsetDateTime.now(),
    sentBy: AuthUser = authUserFactory.create(),
    referenceNumber: String = "JS18726AC",
    supplementaryRiskId: UUID = UUID.randomUUID(),
    assignments: List<ReferralAssignment> = emptyList(),
    supplierAssessment: SupplierAssessment? = null,
    serviceUserData: ReferralServiceUserData? = null,
    locationType: PersonCurrentLocationType? = PersonCurrentLocationType.COMMUNITY,
    pdu: String? = null,
    nDeliusPDU: String? = null,
    probationOffice: String? = null,
    prisonId: String? = null,
  ): Pair<ReferralSummary, Referral> {
    val referral = createReferral(
      id = id,
      createdAt = createdAt,
      createdBy = createdBy,
      serviceUserCRN = serviceUserCRN,
      intervention = intervention,
      relevantSentenceId = relevantSentenceId,
      desiredOutcomes = desiredOutcomes,
      serviceUserData = serviceUserData,
      actionPlans = actionPlans,
      selectedServiceCategories = selectedServiceCategories,
      sentAt = sentAt,
      sentBy = sentBy,
      referenceNumber = referenceNumber,
      supplementaryRiskId = supplementaryRiskId,
      assignments = assignments,
      concludedAt = concludedAt,
    )

    val referralSummary = ReferralSummary(
      referralId = id,
      crn = serviceUserCRN,
      interventionTitle = intervention.title,
      sentAt = sentAt,
      dynamicFrameworkContractId = intervention.dynamicFrameworkContract.id,
      assignedToUserId = referral.currentAssignee?.id,
      assignedToUserName = referral.currentAssignee?.userName,
      assignedToAuthSource = referral.currentAssignee?.authSource,
      sentByUserId = sentBy.id,
      sentByUserName = sentBy.userName,
      sentByAuthSource = sentBy.authSource,
      referenceNumber = referenceNumber,
      serviceUserLastName = referral.serviceUserData?.lastName,
      serviceUserFirstName = referral.serviceUserData?.firstName,
      locationType = locationType,
      prisonId = prisonId,
      expectedReleaseDate = referral.referralLocation?.expectedReleaseDate,
      isReferralReleasingIn12Weeks = referral.referralLocation?.isReferralReleasingIn12Weeks,
      probationOffice = probationOffice,
      pdu = pdu,
      nDeliusPDU = nDeliusPDU,
      serviceProviderName = referral.intervention.dynamicFrameworkContract.primeProvider.name,
      serviceProviderId = referral.intervention.dynamicFrameworkContract.primeProvider.id,
      concludedAt = concludedAt,
    )

    return referralSummary to referral
  }

  fun getReferralSummary(
    referral: Referral,
  ): ReferralSummary = ReferralSummary(
    referralId = referral.id,
    crn = referral.serviceUserCRN,
    interventionTitle = referral.intervention.title,
    sentAt = referral.sentAt!!,
    dynamicFrameworkContractId = referral.intervention.dynamicFrameworkContract.id,
    assignedToUserId = referral.currentAssignee?.id,
    assignedToUserName = referral.currentAssignee?.userName,
    assignedToAuthSource = referral.currentAssignee?.authSource,
    sentByUserId = referral.sentBy?.id!!,
    sentByUserName = referral.sentBy?.userName!!,
    sentByAuthSource = referral.sentBy?.authSource!!,
    referenceNumber = referral.referenceNumber!!,
    serviceUserLastName = referral.serviceUserData?.lastName,
    serviceUserFirstName = referral.serviceUserData?.firstName,
    locationType = referral.referralLocation?.type,
    prisonId = referral.referralLocation?.prisonId,
    expectedReleaseDate = referral.referralLocation?.expectedReleaseDate,
    isReferralReleasingIn12Weeks = referral.referralLocation?.isReferralReleasingIn12Weeks,
    probationOffice = referral.probationPractitionerDetails?.probationOffice,
    pdu = referral.probationPractitionerDetails?.pdu,
    nDeliusPDU = referral.probationPractitionerDetails?.nDeliusPDU,
    serviceProviderName = referral.intervention.dynamicFrameworkContract.primeProvider.name,
    serviceProviderId = referral.intervention.dynamicFrameworkContract.primeProvider.id,
    concludedAt = referral.concludedAt,
  )
}
