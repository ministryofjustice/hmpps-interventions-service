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
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.model.ReferralSummary
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class ReferralSummaryFactory(em: TestEntityManager? = null) : BaseReferralFactory(em) {
  private val authUserFactory = AuthUserFactory(em)
  private val interventionFactory = InterventionFactory(em)
  private val endOfServiceReportFactory = EndOfServiceReportFactory(em)

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
    supplierAssessment: SupplierAssessment? = null,
    serviceUserData: ReferralServiceUserData? = null,
    endRequestedAt: OffsetDateTime? = createdAt.plusHours(2),
    eosrId: UUID? = null,
    appointmentId: UUID? = null,
  ): ReferralSummary {
    createReferral(
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
      supplierAssessment = supplierAssessment,
    )

    return ReferralSummary(
      id = id,
      crn = serviceUserCRN,
      interventionTitle = intervention.title,
      createdBy = createdBy.id,
      sentAt = sentAt,
      sentBy = sentBy.id,
      referenceNumber = referenceNumber,
      concludedAt = concludedAt,
      assignedToId = null,
      assignedAuthSource = null,
      assignedUserName = null,
      sentUserName = sentBy.userName,
      sentAuthSource = sentBy.authSource,
      serviceUserFirstName = if (serviceUserData != null) serviceUserData.firstName!! else null,
      serviceUserLastName = if (serviceUserData != null) serviceUserData.lastName!! else null,
      serviceProviderId = intervention.dynamicFrameworkContract.primeProvider.id,
      serviceProviderName = intervention.dynamicFrameworkContract.primeProvider.name,
      expectedReleaseDate = null,
      isReferralReleasingIn12Weeks = null,
      referralType = null,
      prisonId = null,
      probationOffice = null,
      pdu = null,
      ndeliusPdu = null,
      contractId = intervention.dynamicFrameworkContract.id,
      endRequestedAt = endRequestedAt,
      eosrId = eosrId,
      appointmentId = appointmentId,
    )
  }

  fun createSentReferralSummaryWithReferral(
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
    type: PersonCurrentLocationType? = PersonCurrentLocationType.CUSTODY,
    prisonId: String? = "prisonId1",
    probationOffice: String? = "office1",
    pdu: String? = "pdu1",
    nDeliusPDU: String? = "pdu1",
    endRequestedAt: OffsetDateTime? = createdAt.plusHours(2),
    eosrId: UUID? = null,
    appointmentId: UUID? = null,
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
      supplierAssessment = supplierAssessment,
    )

    val referralSummary =
      ReferralSummary(
        id = id,
        crn = serviceUserCRN,
        interventionTitle = intervention.title,
        createdBy = createdBy.id,
        sentAt = sentAt,
        sentBy = sentBy.id,
        referenceNumber = referenceNumber,
        concludedAt = concludedAt,
        assignedToId = null,
        assignedAuthSource = null,
        assignedUserName = null,
        sentUserName = sentBy.userName,
        sentAuthSource = sentBy.authSource,
        serviceUserFirstName = if (serviceUserData != null) serviceUserData.firstName!! else null,
        serviceUserLastName = if (serviceUserData != null) serviceUserData.lastName!! else null,
        serviceProviderId = intervention.dynamicFrameworkContract.primeProvider.id,
        serviceProviderName = intervention.dynamicFrameworkContract.primeProvider.name,
        expectedReleaseDate = LocalDate.now(),
        isReferralReleasingIn12Weeks = true,
        referralType = type?.name,
        prisonId = prisonId,
        probationOffice = probationOffice,
        pdu = pdu,
        ndeliusPdu = nDeliusPDU,
        contractId = intervention.dynamicFrameworkContract.id,
        endRequestedAt = endRequestedAt,
        eosrId = eosrId,
        appointmentId = appointmentId,
      )
    return referralSummary to referral
  }

  fun getReferralSummary(
    referral: Referral,
  ): ReferralSummary {
    return ReferralSummary(
      id = referral.id,
      crn = referral.serviceUserCRN,
      createdBy = referral.createdBy.id,
      interventionTitle = referral.intervention.title,
      sentAt = referral.sentAt!!,
      sentBy = referral.sentBy!!.id,
      referenceNumber = referral.referenceNumber!!,
      concludedAt = referral.concludedAt,
      assignedToId = referral.currentAssignee?.id,
      assignedUserName = referral.currentAssignee?.userName,
      assignedAuthSource = referral.currentAssignee?.authSource,
      sentAuthSource = referral.sentBy!!.authSource,
      sentUserName = referral.sentBy!!.userName,
      serviceUserFirstName = if (referral.serviceUserData != null) referral.serviceUserData?.firstName!! else null,
      serviceUserLastName = if (referral.serviceUserData != null) referral.serviceUserData?.lastName!! else null,
      serviceProviderId = referral.intervention.dynamicFrameworkContract.primeProvider.id,
      serviceProviderName = referral.intervention.dynamicFrameworkContract.primeProvider.name,
      expectedReleaseDate = referral.referralLocation?.expectedReleaseDate,
      isReferralReleasingIn12Weeks = referral.referralLocation?.isReferralReleasingIn12Weeks,
      referralType = referral.referralLocation?.type?.name,
      prisonId = referral.referralLocation?.prisonId,
      probationOffice = referral.probationPractitionerDetails?.probationOffice,
      pdu = referral.probationPractitionerDetails?.pdu,
      ndeliusPdu = referral.probationPractitionerDetails?.nDeliusPDU,
      contractId = referral.intervention.dynamicFrameworkContract.id,
      endRequestedAt = referral.endRequestedAt,
      eosrId = referral.endOfServiceReport?.id,
      appointmentId = referral.supplierAssessment?.currentAppointment?.id,
    )
  }
}
