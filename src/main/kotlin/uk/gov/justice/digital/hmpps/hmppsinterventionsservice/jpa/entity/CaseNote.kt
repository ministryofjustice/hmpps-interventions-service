package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity

import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import jakarta.validation.constraints.NotNull
import org.hibernate.annotations.Fetch
import org.hibernate.annotations.FetchMode
import java.time.OffsetDateTime
import java.util.UUID

@Entity
class CaseNote(
  @Id
  val id: UUID,
  @NotNull
  @ManyToOne(fetch = FetchType.LAZY)
  var referral: Referral,
  @NotNull var subject: String,
  @NotNull var body: String,
  @NotNull val sentAt: OffsetDateTime,
  @NotNull
  @ManyToOne
  @Fetch(FetchMode.JOIN)
  val sentBy: AuthUser,
)
