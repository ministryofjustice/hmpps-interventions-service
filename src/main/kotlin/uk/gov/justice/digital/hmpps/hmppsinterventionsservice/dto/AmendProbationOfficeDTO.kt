package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto

data class AmendProbationOfficeDTO(
  val probationOffice: String,
  val preventEmailNotification: Boolean? = null,
)
