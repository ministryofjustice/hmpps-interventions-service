package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.config

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.orm.jpa.JpaTransactionManager
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter
import org.springframework.transaction.PlatformTransactionManager
import java.util.*
import javax.sql.DataSource
import kotlin.collections.HashMap

@EnableJpaRepositories("uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa")
@Configuration
class DataJpaConfiguration {
  @Bean
  @Primary
  @Autowired
  fun entityManagerFactory(@Qualifier("mainDataSource") dataSource: DataSource): LocalContainerEntityManagerFactoryBean {
    val vendorAdapter = HibernateJpaVendorAdapter()
    vendorAdapter.setDatabasePlatform("org.hibernate.dialect.PostgreSQLDialect")
    vendorAdapter.setGenerateDdl(false)
    vendorAdapter.setShowSql(true)
    val factory = LocalContainerEntityManagerFactoryBean()
    factory.jpaVendorAdapter = vendorAdapter
    factory.dataSource = dataSource
    factory.setPersistenceUnitName("main")
    factory.setPackagesToScan("uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa")
    factory.setJpaProperties(additionalJpaProperties())
    return factory
  }

  fun additionalJpaProperties(): Properties {
    val map: MutableMap<String, String> = HashMap()
    map["hibernate.hbm2ddl.auto"] = "validate"
    map["hibernate.dialect"] = "org.hibernate.dialect.PostgreSQL10Dialect"
    map["hibernate.show_sql"] = "false"
    map["hibernate.physical_naming_strategy"] = "org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy"
    return map.toProperties()
  }

  @Bean(name = ["reportingTransactionManager"])
  fun dbTransactionManager(@Qualifier("reportingEntityManagerFactory") entityManagerFactory: LocalContainerEntityManagerFactoryBean): PlatformTransactionManager {
    val transactionManager = JpaTransactionManager()
    transactionManager.entityManagerFactory = entityManagerFactory.getObject()
    return transactionManager
  }

  @Bean
  @Autowired
  fun reportingEntityManagerFactory(@Qualifier("reportingDataSource") dataSource: DataSource): LocalContainerEntityManagerFactoryBean {
    val vendorAdapter = HibernateJpaVendorAdapter()
    vendorAdapter.setDatabasePlatform("org.hibernate.dialect.PostgreSQLDialect")
    vendorAdapter.setGenerateDdl(false)
    vendorAdapter.setShowSql(true)
    val factory = LocalContainerEntityManagerFactoryBean()
    factory.jpaVendorAdapter = vendorAdapter
    factory.dataSource = dataSource
    factory.setPersistenceUnitName("reporting")
    factory.setPackagesToScan("uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa")
    factory.setJpaProperties(additionalJpaProperties())
    return factory
  }
}
