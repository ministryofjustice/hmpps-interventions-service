package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.config

import jakarta.persistence.EntityManagerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.jdbc.DataSourceBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Scope
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType
import org.springframework.orm.jpa.JpaTransactionManager
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.EnableTransactionManagement
import javax.sql.DataSource

@Configuration
@EnableTransactionManagement
class DataSourceConfig(
  @Value("\${spring.datasource.url}") private val dataSourceUrl: String,
  @Value("\${spring.datasource.username}") private val dataSourceUserName: String,
  @Value("\${spring.datasource.password}") private val dataSourcePassword: String,
) {

  @Bean(name = ["mainDataSource", "dataSource"])
  @Primary
  fun dataSource(): DataSource {
    return DataSourceBuilder.create()
      .driverClassName("org.postgresql.Driver")
      .url(dataSourceUrl)
      .username(dataSourceUserName)
      .password(dataSourcePassword)
      .build()
  }

  @Bean("memoryDataSource")
  fun memoryDataSource(): DataSource {
    return EmbeddedDatabaseBuilder()
      .setType(EmbeddedDatabaseType.H2)
      .addScript("/org/springframework/batch/core/schema-drop-h2.sql")
      .addScript("/org/springframework/batch/core/schema-h2.sql")
      .build()
  }

  @Bean("batchTransactionManager")
  fun batchTransactionManager(@Qualifier("memoryDataSource") dataSource: DataSource): PlatformTransactionManager {
    return JpaTransactionManager()
  }

  @Bean("transactionManager")
  @Primary
  fun transactionManager(entityManagerFactory: EntityManagerFactory): PlatformTransactionManager {
    return JpaTransactionManager(entityManagerFactory)
  }
}
