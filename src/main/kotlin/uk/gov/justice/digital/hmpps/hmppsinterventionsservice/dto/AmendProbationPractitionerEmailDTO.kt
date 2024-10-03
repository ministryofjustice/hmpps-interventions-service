package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto

data class AmendProbationPractitionerEmailDTO(
  val ppEmail: String,
  val preventEmailNotification: Boolean? = null,
)
