package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity

import jakarta.persistence.Column
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.validation.constraints.NotNull
import org.apache.commons.codec.language.bm.RuleType
import org.hibernate.annotations.JdbcType
import org.hibernate.dialect.PostgreSQLEnumJdbcType
import java.util.UUID

@Entity
data class DesiredOutcomeFilterRule(
  @Id var id: UUID,
  @Column(name = "desired_outcome_id") @NotNull val desiredOutcomeId: UUID,
  @Enumerated(EnumType.STRING)
  @JdbcType(PostgreSQLEnumJdbcType::class)
  @NotNull val ruleType: RuleType,
  @NotNull val matchType: String,
  @NotNull val matchData: String,
)

enum class RuleType {
  EXCLUDE,
  INCLUDE,
}
