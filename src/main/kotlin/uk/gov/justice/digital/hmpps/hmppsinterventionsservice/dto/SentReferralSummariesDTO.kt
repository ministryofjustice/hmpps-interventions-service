package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto

import org.apache.commons.lang3.StringUtils.isNotEmpty
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.PersonCurrentLocationType
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ReferralSummary
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
  val isReferralReleasingIn12Weeks: Boolean?,
) {
  companion object {
    fun from(referral: ReferralSummary): SentReferralSummariesDTO {
      val location = if (referral.locationType == PersonCurrentLocationType.CUSTODY) {
        referral.prisonId
      } else if (isNotEmpty(referral.probationOffice)) {
        referral.probationOffice
      } else if (isNotEmpty(referral.pdu)) {
        referral.pdu
      } else if (isNotEmpty(referral.nDeliusPDU)) {
        referral.nDeliusPDU
      } else {
        "Not available"
      }
      return SentReferralSummariesDTO(
        id = referral.referralId,
        sentAt = referral.sentAt,
        sentBy = AuthUserDTO(referral.sentByUserName, referral.sentByAuthSource, referral.sentByUserId),
        referenceNumber = referral.referenceNumber,
        assignedTo = referral.assignedToUserId?.let { AuthUserDTO(referral.assignedToUserName!!, referral.assignedToAuthSource!!, referral.assignedToUserId) },
        serviceUser = ServiceUserDTO.from(referral.crn, referral.serviceUserFirstName, referral.serviceUserLastName),
        serviceProvider = ServiceProviderDTO(referral.serviceProviderName, referral.serviceProviderId),
        interventionTitle = referral.interventionTitle,
        concludedAt = referral.concludedAt,
        expectedReleaseDate = referral.expectedReleaseDate,
        location = location,
        locationType = referral.locationType?.name,
        isReferralReleasingIn12Weeks = referral.isReferralReleasingIn12Weeks,
      )
    }
  }
}
