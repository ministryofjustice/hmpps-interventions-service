package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.config

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import javax.sql.DataSource

@Configuration
class MainConfiguration {

  @Bean("mainDataSourceProperties")
  @Primary
  @ConfigurationProperties("spring.datasource.main")
  fun mainDataSourceProperties(): DataSourceProperties {
    return DataSourceProperties()
  }

  @Bean("mainDataSource")
  @Primary
  @ConfigurationProperties("spring.datasource.main")
  fun mainDataSource(@Qualifier("mainDataSourceProperties") mainDataSourceProperties: DataSourceProperties?): DataSource {
    return mainDataSourceProperties()
      .initializeDataSourceBuilder()
      .build()
  }
}
