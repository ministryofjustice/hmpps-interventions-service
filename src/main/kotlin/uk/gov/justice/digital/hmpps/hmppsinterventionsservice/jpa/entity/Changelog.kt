package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity

import java.time.OffsetDateTime
import java.util.UUID
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import org.hibernate.annotations.Type
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.ReferralAmendmentDetails

@Entity
data class Changelog(
  @Column(name = "referral_id") val referralId: UUID,

  @Id
  @Column(name = "changelog_id")
  val id: UUID,

  var topic: String,

  @Type(type = "jsonb")
  @Column(name = "old_value", columnDefinition = "jsonb")
  val oldVal: ReferralAmendmentDetails,

  @Type(type = "jsonb")
  @Column(name = "new_value", columnDefinition = "jsonb")
  val newVal: ReferralAmendmentDetails,

  @Column(name = "reason_for_change")
  var reasonForChange: String,

  @Column(name = "changed_at")
  val changedAt: OffsetDateTime,

  @Column(name = "changed_by_id") val changedById: String
)
