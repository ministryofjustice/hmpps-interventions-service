package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto

import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ReferralDetails
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

data class UpdateReferralDetailsDTO(
  val maximumEnforceableDays: Int?,
  val completionDeadline: LocalDate?,
  val furtherInformation: String?,
  val expectedReleaseDate: LocalDate?,
  val expectedReleaseDateMissingReason: String?,
  val reasonForChange: String,
  val reasonForReferral: String?,
  val reasonForReferralFurtherInformation: String?,
  val reasonForReferralCreationBeforeAllocation: String?,
) {
  val isValidUpdate: Boolean get() = maximumEnforceableDays != null || completionDeadline !== null || furtherInformation !== null || reasonForReferral != null || reasonForReferralCreationBeforeAllocation != null
}

data class ReferralDetailsDTO(
  val referralId: UUID,
  val maximumEnforceableDays: Int?,
  val completionDeadline: LocalDate?,
  val furtherInformation: String?,
  val reasonForChange: String,
  val createdById: String,
  val createdAt: OffsetDateTime,
  val reasonForReferral: String? = null,
  val reasonForReferralFurtherInformation: String? = null,
  val prisonEstablishment: String? = null,
  val probationOffice: String? = null,
) {
  companion object {
    fun from(referralDetails: ReferralDetails): ReferralDetailsDTO {
      return ReferralDetailsDTO(
        referralId = referralDetails.referralId,
        maximumEnforceableDays = referralDetails.maximumEnforceableDays,
        completionDeadline = referralDetails.completionDeadline,
        furtherInformation = referralDetails.furtherInformation,
        reasonForChange = referralDetails.reasonForChange,
        createdById = referralDetails.createdByUserId,
        createdAt = referralDetails.createdAt,
        reasonForReferral = referralDetails.reasonForReferral,
        reasonForReferralFurtherInformation = referralDetails.reasonForReferralFurtherInformation,
      )
    }
  }
}
