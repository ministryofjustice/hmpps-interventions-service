package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.config

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import javax.sql.DataSource

@Configuration
class ReportingConfiguration {

  @Bean("reportingDataSourceProperties")
  @ConfigurationProperties("spring.datasource.reporting")
  fun reportingDataSourceProperties(): DataSourceProperties {
    return DataSourceProperties()
  }

  @Bean("reportingDataSource")
  @ConfigurationProperties("spring.datasource.reporting")
  fun reportingDataSource(@Qualifier("reportingDataSourceProperties") reportingDataSourceProperties: DataSourceProperties?): DataSource {
    return reportingDataSourceProperties()
      .initializeDataSourceBuilder()
      .build()
  }
}
