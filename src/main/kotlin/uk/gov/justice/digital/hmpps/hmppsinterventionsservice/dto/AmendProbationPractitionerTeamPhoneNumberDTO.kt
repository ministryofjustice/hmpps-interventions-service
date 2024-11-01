package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto

data class AmendProbationPractitionerTeamPhoneNumberDTO(
  val ppTeamPhoneNumber: String,
  val preventEmailNotification: Boolean? = null,
)
