package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.jdbc.DataSourceBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import javax.sql.DataSource

@Configuration
class DataSourceConfig(
  @Value("\${spring.datasource.url}") private val dataSourceUrl: String,
  @Value("\${spring.datasource.username}") private val dataSourceUserName: String,
  @Value("\${spring.datasource.password}") private val dataSourcePassword: String,
) {

  @Bean(name=["mainDataSource","dataSource"])
  @Primary
  fun dataSource(): DataSource {
    return DataSourceBuilder.create()
      .driverClassName("org.postgresql.Driver")
      .url(dataSourceUrl)
      .username(dataSourceUserName)
      .password(dataSourcePassword)
      .build()
  }
}
