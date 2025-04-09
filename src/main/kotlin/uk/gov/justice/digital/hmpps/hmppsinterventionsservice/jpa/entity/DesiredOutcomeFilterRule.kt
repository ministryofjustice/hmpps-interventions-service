package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity

import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.validation.constraints.NotNull
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

  @ElementCollection
  @CollectionTable(name = "desired_outcome_filter_rule_match_data", joinColumns = [JoinColumn(name = "desired_outcome_filter_rule_id")])
  @NotNull
  val matchData: MutableList<String>,
)

enum class RuleType {
  EXCLUDE,
  INCLUDE,
}
