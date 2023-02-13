package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jobs.scheduled.onboarding

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

data class RegionDefinition(
  @JsonProperty("nps_region_code") val npsCode: Char?,
  @JsonProperty("pcc_region_code") val pccCode: String?,
)

data class EligibilityDefinition(
  @JsonProperty("allows_female") val allowsFemale: Boolean,
  @JsonProperty("allows_male") val allowsMale: Boolean,
  @JsonProperty("minimum_age") val minimumAge: Int,
  @JsonProperty("maximum_age") val maximumAge: Int?,
)

data class SchedulingDefinition(
  @JsonProperty("hide_before") val hideBefore: LocalDate,
  @JsonProperty("hide_after") val hideAfter: OffsetDateTime?,
)

data class ActivityDefinition(
  @JsonProperty("need_area") val needArea: String,
  @JsonProperty("activity") val activity: String,
  @JsonProperty("delivery_method") val deliveryMethod: String,
  @JsonProperty("delivery_location") val deliveryLocation: String,
)

data class ProviderDefinition(
  @JsonProperty("prime_provider_code") val primeProviderId: String,
  @JsonProperty("subcontractor_codes") val subcontractorIds: List<String>,
)

data class ContractDefinition(
  @JsonProperty("internal_contract_id") val internalContractId: UUID,
  @JsonProperty("contract_reference") val contractReference: String,
  @JsonProperty("contract_type_id") val contractTypeCode: String,
  @JsonProperty("region") val region: RegionDefinition,
  @JsonProperty("eligibility") val eligibility: EligibilityDefinition,
  @JsonProperty("scheduling") val scheduling: SchedulingDefinition,
  @JsonProperty("providers") val providers: ProviderDefinition,

  @JsonProperty("internal_intervention_id") val internalInterventionId: UUID,
  @JsonProperty("intervention_title") val interventionTitle: String,
  @JsonProperty("short_description") val shortDescription: String?,
  @JsonProperty("activities") val activities: List<ActivityDefinition>?,
)
