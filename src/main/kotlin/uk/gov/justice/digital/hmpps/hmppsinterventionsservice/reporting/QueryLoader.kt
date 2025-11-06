package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.reporting

import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component

@Component
class QueryLoader {
  fun loadQuery(queryFileName: String): String = try {
    val resource = ClassPathResource("queries/$queryFileName")
    resource.inputStream.bufferedReader().use { it.readText() }
  } catch (e: Exception) {
    throw IllegalArgumentException("Failed to load query file: queries/$queryFileName", e)
  }
}
