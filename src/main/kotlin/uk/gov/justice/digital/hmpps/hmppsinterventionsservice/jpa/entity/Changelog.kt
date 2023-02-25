package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity

import com.vladmihalcea.hibernate.type.basic.PostgreSQLEnumType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import jakarta.validation.constraints.NotNull
import org.hibernate.annotations.Fetch
import org.hibernate.annotations.FetchMode
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.annotations.Type
import org.hibernate.type.SqlTypes
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.ReferralAmendmentDetails
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.AmendTopic
import java.time.OffsetDateTime
import java.util.UUID

@Entity
data class Changelog(
  @Column(name = "referral_id") val referralId: UUID,

  @Id
  @Column(name = "changelog_id")
  val id: UUID,

  @Enumerated(EnumType.STRING)
  @Column(columnDefinition = "amendtypes")
  @Type(PostgreSQLEnumType::class)
  var topic: AmendTopic,

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "old_value")
  val oldVal: ReferralAmendmentDetails,

  @JdbcTypeCode(SqlTypes.JSON)
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
