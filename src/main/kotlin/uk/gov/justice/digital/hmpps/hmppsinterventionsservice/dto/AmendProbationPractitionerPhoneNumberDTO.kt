package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto

data class AmendProbationPractitionerPhoneNumberDTO(
  val ppPhoneNumber: String,
  val preventEmailNotification: Boolean? = null,
)
