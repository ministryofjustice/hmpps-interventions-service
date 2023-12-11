package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto

import org.apache.commons.lang3.StringUtils.isNotEmpty
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.PersonCurrentLocationType
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.model.ReferralSummary
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
      val location = if (referral.referralType != null && PersonCurrentLocationType.valueOf(referral.referralType) == PersonCurrentLocationType.CUSTODY) {
        referral.prisonId
      } else if (isNotEmpty(referral.probationOffice)) {
        referral.probationOffice
      } else if (isNotEmpty(referral.pdu)) {
        referral.pdu
      } else if (isNotEmpty(referral.ndeliusPdu)) {
        referral.ndeliusPdu
      } else {
        "Not available"
      }
      return SentReferralSummariesDTO(
        id = referral.id,
        sentAt = referral.sentAt,
        sentBy = AuthUserDTO.from(referral.sentUserName, referral.sentAuthSource, referral.sentBy),
        referenceNumber = referral.referenceNumber,
        assignedTo = assignedAuthUser(referral.assignedAuthSource, referral.assignedToId, referral.assignedUserName),
        serviceUser = ServiceUserDTO.from(referral.crn, referral.serviceUserFirstName, referral.serviceUserLastName),
        serviceProvider = ServiceProviderDTO.from(referral.serviceProviderName, referral.serviceProviderId),
        interventionTitle = referral.interventionTitle,
        concludedAt = referral.concludedAt,
        expectedReleaseDate = referral.expectedReleaseDate,
        location = location,
        locationType = if (referral.referralType != null) PersonCurrentLocationType.valueOf(referral.referralType).name else null,
        isReferralReleasingIn12Weeks = referral.isReferralReleasingIn12Weeks,
      )
    }

    private fun assignedAuthUser(assignedId: String?, assignedAuthSource: String?, assignedUserName: String?): AuthUserDTO? {
      if (assignedId != null && assignedAuthSource != null && assignedUserName != null) {
        return AuthUserDTO.from(assignedUserName, assignedAuthSource, assignedId)
      }
      return null
    }
  }
}
