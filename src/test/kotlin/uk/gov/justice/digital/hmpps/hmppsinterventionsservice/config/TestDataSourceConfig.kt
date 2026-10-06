package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.util.StdDateFormat
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType
import org.springframework.orm.jpa.JpaTransactionManager
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.web.reactive.function.client.WebClient
import javax.sql.DataSource

@TestConfiguration
class TestDataSourceConfig {

  @Bean(name = ["mainDataSource", "dataSource"])
  @Primary
  fun testDataSource(): DataSource = EmbeddedDatabaseBuilder()
    .setType(EmbeddedDatabaseType.H2)
    .setName("testdb")
    .addScript("/org/springframework/batch/core/schema-drop-h2.sql")
    .addScript("/org/springframework/batch/core/schema-h2.sql")
    .build()

  @Bean("memoryDataSource")
  fun memoryDataSource(): DataSource = EmbeddedDatabaseBuilder()
    .setType(EmbeddedDatabaseType.H2)
    .setName("batchdb")
    .addScript("/org/springframework/batch/core/schema-drop-h2.sql")
    .addScript("/org/springframework/batch/core/schema-h2.sql")
    .build()

  @Bean("transactionManager")
  @Primary
  fun testTransactionManager(): PlatformTransactionManager = JpaTransactionManager()

  @Bean("batchTransactionManager")
  fun testBatchTransactionManager(): PlatformTransactionManager = JpaTransactionManager()

  @Bean
  @Primary
  fun webClientBuilder(): WebClient.Builder = WebClient.builder()

  @Bean
  @Primary
  fun objectMapper(): ObjectMapper = ObjectMapper().apply {
    dateFormat = StdDateFormat()
    disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
  }
}
