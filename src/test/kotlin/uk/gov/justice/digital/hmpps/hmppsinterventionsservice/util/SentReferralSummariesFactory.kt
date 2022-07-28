package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util

import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ActionPlan
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AuthUser
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.DesiredOutcome
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.EndOfServiceReport
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Intervention
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Referral
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ReferralAssignment
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.SentReferralSummary
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ServiceCategory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ServiceUserData
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.SupplierAssessment
import java.time.OffsetDateTime
import java.util.UUID

class SentReferralSummariesFactory(em: TestEntityManager? = null) : BaseReferralFactory(em) {
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

    supplierAssessment: SupplierAssessment? = null,
    serviceUserData: ServiceUserData? = null
  ): SentReferralSummary {
    create(
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
      concludedAt = concludedAt,
      supplierAssessment = supplierAssessment,
      serviceUserData = serviceUserData
    )

    return SentReferralSummary(
      id = id,
      serviceUserCRN = serviceUserCRN,
      intervention = intervention,
      createdBy = createdBy,
      sentAt = sentAt,
      sentBy = sentBy,
      referenceNumber = referenceNumber,
      concludedAt = concludedAt,
      assignments = assignments.toMutableList(),
      serviceUserData = serviceUserData,
      actionPlans = actionPlans,
      supplierAssessment = supplierAssessment
    )
  }

  fun getReferralSummary(
    referral: Referral,
    endOfServiceReport: EndOfServiceReport? = null
  ): SentReferralSummary {
    return SentReferralSummary(
      id = referral.id,
      serviceUserCRN = referral.serviceUserCRN,
      createdBy = referral.createdBy,
      intervention = referral.intervention,
      sentAt = referral.sentAt!!,
      sentBy = referral.sentBy!!,
      referenceNumber = referral.referenceNumber!!,
      concludedAt = referral.concludedAt,
      assignments = referral.assignments,
      endRequestedAt = referral.endRequestedAt,
      endOfServiceReport = referral.endOfServiceReport ?: endOfServiceReport,
      serviceUserData = referral.serviceUserData,
      actionPlans = referral.actionPlans,
      supplierAssessment = referral.supplierAssessment
    )
  }
}
