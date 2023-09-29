package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jobs.scheduled.onboarding

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import mu.KLogging
import org.springframework.batch.item.ItemReader
import org.springframework.stereotype.Component
import org.springframework.util.ResourceUtils
import java.io.File

@Component
class ContractDefinitionReader : ItemReader<ContractDefinition> {
  companion object : KLogging()

  private var files: Array<File> = ResourceUtils.getFile("classpath:contracts/").listFiles()
  private var index = 0
  private val objectMapper = ObjectMapper().registerModule(JavaTimeModule())

  override fun read(): ContractDefinition? {
    if (files.size <= index) return null

    val currentIndex = index
    index++

    val file = files[currentIndex]
    logger.info("Reading file $index/${this.files.size}: $file")
    return objectMapper.readValue(file, ContractDefinition::class.java)
  }
}
