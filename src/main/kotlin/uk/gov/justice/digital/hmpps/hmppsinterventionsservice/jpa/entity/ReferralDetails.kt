package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@Entity
data class ReferralDetails(
  @Id val id: UUID,
  var supersededById: UUID?,
  @Column(name = "referral_id") val referralId: UUID,
  val createdAt: OffsetDateTime,
  @Column(name = "created_by") val createdByUserId: String,
  val reasonForChange: String,

  // actual referral details fields:
  var completionDeadline: LocalDate? = null,
  var furtherInformation: String? = null,
  var maximumEnforceableDays: Int? = null,
)
