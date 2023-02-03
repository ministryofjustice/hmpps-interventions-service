package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity

import org.hibernate.annotations.Type
import org.jetbrains.annotations.NotNull
import java.time.LocalDate
import java.util.UUID
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.FetchType
import javax.persistence.Id
import javax.persistence.OneToOne

@Entity
data class ReferralLocation(
  @Id val id: UUID,
  @OneToOne(fetch = FetchType.LAZY) val referral: Referral,
  @Type(type = "person_current_location_type") @Enumerated(EnumType.STRING) @NotNull @Column(name = "type") val type: PersonCurrentLocationType,
  @Column(name = "prison_id") val prisonId: String?,
  @Column(name = "expected_release_date") val expectedReleaseDate: LocalDate?,
  @Column(name = "expected_release_date_missing_reason") val expectedReleaseDateMissingReason: String?
)
