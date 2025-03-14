package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.validation.constraints.NotNull

typealias AuthGroupID = String

@Entity
data class ServiceProvider(
  // Service Provider id maps to the hmpps-auth field Group#groupCode
  @NotNull @Id
  val id: AuthGroupID,
  @NotNull val name: String,
) {
  override fun hashCode(): Int = id.hashCode()

  override fun equals(other: Any?): Boolean {
    if (other == null || other !is ServiceProvider) {
      return false
    }

    return id == other.id
  }
}
