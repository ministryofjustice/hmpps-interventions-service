package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto

data class AmendProbationPractitionerNameDTO(
  val ppName: String,
  val preventEmailNotification: Boolean? = null,
)
