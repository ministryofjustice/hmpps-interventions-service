package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto

import java.time.OffsetDateTime

data class PersonIdentifier(
  val type: String,
  val value: String,
)

data class PersonReference(
  val identifiers: List<PersonIdentifier> = listOf(),
) {
  companion object {
    fun crn(crn: String): PersonReference = PersonReference(listOf(PersonIdentifier(type = "CRN", value = crn)))
  }
}

data class EventDTO(
  val eventType: String,
  val description: String,
  val detailUrl: String,
  val occurredAt: OffsetDateTime,
  val additionalInformation: Map<String, Any>,
  val personReference: PersonReference? = null,
) {
  val version: Int = 1
}
