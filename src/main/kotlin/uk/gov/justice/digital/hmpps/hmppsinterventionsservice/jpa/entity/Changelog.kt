package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity

import com.vladmihalcea.hibernate.type.basic.PostgreSQLEnumType
import com.vladmihalcea.hibernate.type.json.JsonBinaryType
import org.hibernate.annotations.Fetch
import org.hibernate.annotations.FetchMode
import org.hibernate.annotations.Type
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.ReferralAmendmentDetails
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.AmendTopic
import java.time.OffsetDateTime
import java.util.UUID
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.Id
import javax.persistence.ManyToOne
import javax.validation.constraints.NotNull

@Entity
data class Changelog(
  @Column(name = "referral_id") val referralId: UUID,

  @Id
  @Column(name = "changelog_id")
  val id: UUID,

  @Type(PostgreSQLEnumType::class)
  @Column(columnDefinition = "topic")
  @Enumerated(EnumType.STRING)
  var topic: AmendTopic,

  @Type(JsonBinaryType::class)
  @Column(name = "old_value", columnDefinition = "jsonb")
  val oldVal: ReferralAmendmentDetails,

  @Type(JsonBinaryType::class)
  @Column(name = "new_value", columnDefinition = "jsonb")
  val newVal: ReferralAmendmentDetails,

  @Column(name = "reason_for_change")
  var reasonForChange: String,

  @Column(name = "changed_at")
  val changedAt: OffsetDateTime,

  @NotNull @ManyToOne @Fetch(FetchMode.JOIN)
  val changedBy: AuthUser
)
