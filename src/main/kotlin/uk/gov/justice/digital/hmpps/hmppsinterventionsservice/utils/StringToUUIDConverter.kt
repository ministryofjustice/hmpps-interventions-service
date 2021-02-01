package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.utils

import org.springframework.web.server.ServerWebInputException
import java.util.UUID

class StringToUUIDConverter {
  companion object {
    fun parseID(id: String?): UUID {
      return try {
        UUID.fromString(id)
      } catch (e: IllegalArgumentException) {
        throw ServerWebInputException("could not parse id [id=$id]")
      }
    }
  }
}
