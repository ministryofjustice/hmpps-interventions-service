package uk.gov.justice.digital.hmpps.hmppsinterventionsservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration
import org.springframework.boot.runApplication

@SpringBootApplication(exclude = [ DataSourceAutoConfiguration::class, DataSourceTransactionManagerAutoConfiguration::class ])
class HmppsInterventionsService

fun main(args: Array<String>) {
  runApplication<HmppsInterventionsService>(*args)
}
