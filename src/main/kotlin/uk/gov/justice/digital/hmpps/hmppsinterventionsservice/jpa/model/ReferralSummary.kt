package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.model

import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "referral_summary")
data class ReferralSummary(
  @Id val id: UUID,
  val sentAt: OffsetDateTime,
  val sentBy: String,
  val referenceNumber: String,
  val crn: String,
  val createdBy: String,
  val assignedToId: String?,
  val assignedAuthSource: String?,
  val assignedUserName: String?,
  val sentAuthSource: String,
  val sentUserName: String,
  val serviceUserFirstName: String?,
  val serviceUserLastName: String?,
  val serviceProviderId: String,
  val serviceProviderName: String,
  val interventionTitle: String,
  val concludedAt: OffsetDateTime?,
  val expectedReleaseDate: LocalDate?,
  val isReferralReleasingIn12Weeks: Boolean?,
  val referralType: String?,
  val prisonId: String?,
  val probationOffice: String?,
  val pdu: String?,
  val ndeliusPdu: String?,
  val contractId: UUID?,
  val endRequestedAt: OffsetDateTime?,
  val eosrId: UUID?,
  val appointmentId: UUID?,
)
