package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity

import org.hibernate.annotations.Type
import org.jetbrains.annotations.NotNull
import java.util.UUID
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id

@Entity
data class ReferralLocation(
  @Id val id: UUID,
  @NotNull @Column(name = "referral_id") val referralId: UUID,
  @Type(type = "person_current_location_type") @NotNull @Column(name = "type") val type: PersonCurrentLocationType,
  @Column(name = "prison_id") val prisonId: String?
)
