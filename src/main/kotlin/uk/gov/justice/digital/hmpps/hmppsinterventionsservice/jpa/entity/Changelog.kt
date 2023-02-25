package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity

import com.vladmihalcea.hibernate.type.basic.PostgreSQLEnumType
import com.vladmihalcea.hibernate.type.json.JsonBinaryType
import org.hibernate.annotations.Fetch
import org.hibernate.annotations.FetchMode
import org.hibernate.type.YesNoConverter
import jakarta.persistence.Convert
import org.hibernate.type.YesNoConverter
import jakarta.persistence.ConvertDef
import org.hibernate.type.YesNoConverter
import jakarta.persistence.ConvertDefs
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.ReferralAmendmentDetails
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.AmendTopic
import java.time.OffsetDateTime
import java.util.UUID
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import jakarta.validation.constraints.NotNull

@Entity
@TypeDefs(
  value = [
    TypeDef(name = "jsonb", typeClass = JsonBinaryType::class),
    TypeDef(name = "topic", typeClass = PostgreSQLEnumType::class),
  ],
)
data class Changelog(
  @Column(name = "referral_id") val referralId: UUID,

  @Id
  @Column(name = "changelog_id")
  val id: UUID,

  @Type(type = "topic")
  @Enumerated(EnumType.STRING)
  var topic: AmendTopic,

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

  @NotNull
  @ManyToOne
  @Fetch(FetchMode.JOIN)
  val changedBy: AuthUser,
)
