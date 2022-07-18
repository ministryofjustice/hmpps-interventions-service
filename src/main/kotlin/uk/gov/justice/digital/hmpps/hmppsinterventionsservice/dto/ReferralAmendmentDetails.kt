package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto

data class ReferralAmendmentDetails(
  var values: List<String>,
  var furtherInformation: String? = null,
  var additionalNeedsInformation: String? = null,
  var accessibilityNeeds: String? = null,
  var needsInterpreter: Boolean? = null,
  var interpreterLanguage: String? = null,
  var hasAdditionalResponsibilities: Boolean? = null,
  var whenUnavailable: String? = null,
)
