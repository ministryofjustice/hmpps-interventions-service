package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto

import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Referral
import java.time.OffsetDateTime
import java.util.UUID

class SentReferralDashboardSummaryDTO(
  val id: UUID,
  val sentAt: OffsetDateTime,
  val referenceNumber: String,
  val assignedTo: AuthUserDTO?,
  val interventionId: UUID,
  val serviceUser: ServiceUserDTO,
) {
  companion object {
    fun from(referral: Referral): SentReferralDashboardSummaryDTO {
      return SentReferralDashboardSummaryDTO(
        id = referral.id,
        sentAt = referral.sentAt!!,
        referenceNumber = referral.referenceNumber!!,
        assignedTo = referral.currentAssignee?.let { AuthUserDTO.from(it) },
        interventionId = referral.intervention.id,
        serviceUser = ServiceUserDTO.from(referral.serviceUserCRN, referral.serviceUserData),
      )
    }
  }
}
