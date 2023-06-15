package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto

import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.PersonCurrentLocationType
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.SentReferralSummary
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

data class SentReferralSummariesDTO(
  val id: UUID,
  val sentAt: OffsetDateTime,
  val sentBy: AuthUserDTO,
  val referenceNumber: String,
  val assignedTo: AuthUserDTO?,
  val serviceUser: ServiceUserDTO,
  val serviceProvider: ServiceProviderDTO,
  val interventionTitle: String,
  val concludedAt: OffsetDateTime?,
  val expectedReleaseDate: LocalDate?,
  val location: String?,
  val locationType: String?,
) {
  companion object {
    fun from(referral: SentReferralSummary): SentReferralSummariesDTO {
      val location = if (referral.referralLocation?.type == PersonCurrentLocationType.CUSTODY) {
        referral.referralLocation?.prisonId
      } else if (referral.probationPractitionerDetails?.probationOffice != null) {
        referral.probationPractitionerDetails?.probationOffice
      } else if (referral.probationPractitionerDetails?.pdu != null) {
        referral.probationPractitionerDetails?.pdu
      } else if (referral.probationPractitionerDetails?.nDeliusPDU != null) {
        referral.probationPractitionerDetails?.nDeliusPDU
      } else {
        "Not available"
      }
      return SentReferralSummariesDTO(
        id = referral.id,
        sentAt = referral.sentAt,
        sentBy = AuthUserDTO.from(referral.sentBy),
        referenceNumber = referral.referenceNumber,
        assignedTo = referral.currentAssignee?.let { AuthUserDTO.from(it) },
        serviceUser = ServiceUserDTO.from(referral.serviceUserCRN, referral.serviceUserData),
        serviceProvider = ServiceProviderDTO.from(referral.intervention.dynamicFrameworkContract.primeProvider),
        interventionTitle = referral.intervention.title,
        concludedAt = referral.concludedAt,
        expectedReleaseDate = referral.referralLocation?.expectedReleaseDate,
        location = location,
        locationType = referral.referralLocation?.type?.name,
      )
    }
  }
}
