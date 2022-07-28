package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity

import com.vladmihalcea.hibernate.type.json.JsonBinaryType
import org.hibernate.annotations.Fetch
import org.hibernate.annotations.FetchMode
import org.hibernate.annotations.Type
import org.hibernate.annotations.TypeDef
import org.hibernate.annotations.TypeDefs
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.ReferralAmendmentDetails
import java.time.OffsetDateTime
import java.util.UUID
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.ManyToOne
import javax.validation.constraints.NotNull

@Entity
@TypeDefs(
  value = [
    TypeDef(name = "jsonb", typeClass = JsonBinaryType::class)
  ]
)
data class Changelog(
  @Column(name = "referral_id") val referralId: UUID,

  @Id
  @Column(name = "changelog_id")
  val id: UUID,

  var topic: String,

  @Type(type = "jsonb")
  @Column(name = "old_value")
  val oldVal: ReferralAmendmentDetails,

  @Type(type = "jsonb")
  @Column(name = "new_value")
  val newVal: ReferralAmendmentDetails,

  @Column(name = "reason_for_change")
  var reasonForChange: String,

  @Column(name = "changed_at")
  val changedAt: OffsetDateTime,

  @NotNull @ManyToOne @Fetch(FetchMode.JOIN)
  val changedBy: AuthUser
)
