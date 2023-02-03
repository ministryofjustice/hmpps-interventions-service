package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto

import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.DraftReferral
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.PersonCurrentLocationType
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Referral
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class SentReferralDTO(
  val id: UUID,
  val sentAt: OffsetDateTime,
  val sentBy: AuthUserDTO,
  val referenceNumber: String,
  val interventionId: UUID,
  val serviceUserCRN: String,
  val relevantSentenceId: Long,
  val assignedTo: AuthUserDTO?,
  val referral: DraftReferralDTO,
  val actionPlanId: UUID?,
  val currentActionPlanId: UUID?,
  val approvedActionPlanIds: List<UUID>?,
  val endRequestedAt: OffsetDateTime?,
  val endRequestedReason: String?,
  val endRequestedComments: String?,
  val endOfServiceReport: EndOfServiceReportDTO?,
  val concludedAt: OffsetDateTime?,
  val supplementaryRiskId: UUID,
  val endOfServiceReportCreationRequired: Boolean,
  val createdBy: AuthUserDTO,
  val personCurrentLocationType: PersonCurrentLocationType?,
  val personCustodyPrisonId: String?,
  val expectedReleaseDate: LocalDate?,
  val expectedReleaseDateMissingReason: String?
) {
  companion object {
    fun from(referral: Referral, endOfServiceReportRequired: Boolean, draftReferral: DraftReferral? = null): SentReferralDTO {
      return SentReferralDTO(
        id = referral.id,
        sentAt = referral.sentAt!!,
        sentBy = AuthUserDTO.from(referral.sentBy!!),
        referenceNumber = referral.referenceNumber!!,
        assignedTo = referral.currentAssignee?.let { AuthUserDTO.from(it) },
        referral = draftReferral?.let { DraftReferralDTO.from(draftReferral) } ?: DraftReferralDTO.from(referral),
        interventionId = referral.intervention.id,
        serviceUserCRN = referral.serviceUserCRN,
        relevantSentenceId = referral.relevantSentenceId!!,
        actionPlanId = referral.currentActionPlan?.id,
        currentActionPlanId = referral.currentActionPlan?.id,
        approvedActionPlanIds = referral.actionPlans?.filter { it.approvedAt != null }?.map { it.id },
        endRequestedAt = referral.endRequestedAt,
        endRequestedReason = referral.endRequestedReason?.let { it.description },
        endRequestedComments = referral.endRequestedComments,
        endOfServiceReport = referral.endOfServiceReport?.let { EndOfServiceReportDTO.from(it) },
        concludedAt = referral.concludedAt,
        supplementaryRiskId = referral.supplementaryRiskId!!,
        endOfServiceReportCreationRequired = endOfServiceReportRequired,
        personCurrentLocationType = referral.referralLocation?.let { it.type },
        personCustodyPrisonId = referral.referralLocation?.let { it.prisonId },
        expectedReleaseDate = referral.referralLocation?.let { it.expectedReleaseDate },
        expectedReleaseDateMissingReason = referral.referralLocation?.let { it.expectedReleaseDateMissingReason },
        createdBy = AuthUserDTO.from(referral.createdBy)
      )
    }
  }
}
