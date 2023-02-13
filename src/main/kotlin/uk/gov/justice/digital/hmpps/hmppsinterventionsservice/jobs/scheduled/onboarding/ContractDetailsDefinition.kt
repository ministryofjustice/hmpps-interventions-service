package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jobs.scheduled.onboarding

import com.fasterxml.jackson.annotation.JsonProperty

data class Complexity(
  @JsonProperty("severity") val severity: String,
  @JsonProperty("description") val description: String,
)

data class ContractDetailsDefinition(
  @JsonProperty("contract_type") val contractType: String,
  @JsonProperty("contract_code") val contractCode: String,
  @JsonProperty("service_category") val serviceCategory: String,
  @JsonProperty("desired_outcomes") val desiredOutcomes: List<String>,
  @JsonProperty("complexity_levels") val complexity: List<Complexity>,
)
