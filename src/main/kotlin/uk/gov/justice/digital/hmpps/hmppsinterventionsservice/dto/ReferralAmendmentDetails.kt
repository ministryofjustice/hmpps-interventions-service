package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class ReferralAmendmentDetails(
  @JsonProperty("values")
  var values: List<String>,
)
