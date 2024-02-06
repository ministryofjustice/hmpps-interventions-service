package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity

import jakarta.persistence.Embeddable
import jakarta.validation.constraints.NotNull
import java.time.OffsetDateTime
import java.util.UUID
import java.util.UUID.randomUUID

@Embeddable
data class ActionPlanActivity(

  // Attributes
  @NotNull var description: String,

  // For ordering
  @NotNull val createdAt: OffsetDateTime = OffsetDateTime.now(),

  // as an embedded collection entry each instance have no unique identifier
  // this uuid can be though of as a unique handle to each entry for future use
  @NotNull val id: UUID = randomUUID(),

)
