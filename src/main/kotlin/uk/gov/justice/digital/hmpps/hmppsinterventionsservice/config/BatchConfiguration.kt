package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.config

import org.springframework.batch.core.configuration.annotation.JobBuilderFactory
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory
import org.springframework.batch.core.explore.JobExplorer
import org.springframework.batch.core.explore.support.JobExplorerFactoryBean
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.batch.core.launch.support.SimpleJobLauncher
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.repository.support.JobRepositoryFactoryBean
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.transaction.PlatformTransactionManager
import javax.sql.DataSource

@Configuration
class BatchConfiguration(
  @Value("\${spring.batch.concurrency.pool-size}") private val poolSize: Int,
  @Value("\${spring.batch.concurrency.queue-size}") private val queueSize: Int,
) {

  @Bean("asyncJobLauncher")
  fun asyncJobLauncher(@Qualifier("batchJobRepository") jobRepository: JobRepository): JobLauncher {
    val taskExecutor = ThreadPoolTaskExecutor()
    taskExecutor.corePoolSize = poolSize
    taskExecutor.queueCapacity = queueSize
    taskExecutor.afterPropertiesSet()

    val launcher = SimpleJobLauncher()
    launcher.setJobRepository(jobRepository)
    launcher.setTaskExecutor(taskExecutor)
    launcher.afterPropertiesSet()
    return launcher
  }

  @Bean("batchJobRepository")
  fun batchJobRepository(
    @Qualifier("batchDataSource") dataSource: DataSource,
    transactionManager: PlatformTransactionManager,
  ): JobRepository {
    val factory = JobRepositoryFactoryBean()
    factory.setDataSource(dataSource)
    factory.setDatabaseType("H2")
    factory.transactionManager = transactionManager
    factory.afterPropertiesSet()
    return factory.`object`
  }

  @Bean("batchDataSource")
  fun batchDataSource(): DataSource {
    return EmbeddedDatabaseBuilder()
      .setType(EmbeddedDatabaseType.H2)
      .addScript("/org/springframework/batch/core/schema-drop-h2.sql")
      .addScript("/org/springframework/batch/core/schema-h2.sql")
      .build()
  }

  @Bean("batchJobBuilderFactory")
  fun batchJobBuilderFactory(@Qualifier("batchJobRepository") jobRepository: JobRepository): JobBuilderFactory {
    return JobBuilderFactory(jobRepository)
  }

  @Bean("batchStepBuilderFactory")
  fun batchStepBuilderFactory(@Qualifier("batchJobRepository") jobRepository: JobRepository, transactionManager: PlatformTransactionManager): StepBuilderFactory {
    return StepBuilderFactory(jobRepository, transactionManager)
  }

  @Bean("batchJobExplorer")
  fun jobExplorer(
    @Qualifier("batchDataSource") dataSource: DataSource,
    transactionManager: PlatformTransactionManager,
  ): JobExplorer {
    val factory = JobExplorerFactoryBean()
    factory.setDataSource(dataSource)
    factory.setTransactionManager(transactionManager)
    factory.afterPropertiesSet()
    return factory.getObject()
  }

  @Bean("jobBuilderFactory")
  fun jobBuilderFactory(@Qualifier("jobRepository") jobRepository: JobRepository): JobBuilderFactory {
    return JobBuilderFactory(jobRepository)
  }

  @Bean("stepBuilderFactory")
  fun stepBuilderFactory(@Qualifier("jobRepository") jobRepository: JobRepository, transactionManager: PlatformTransactionManager): StepBuilderFactory {
    return StepBuilderFactory(jobRepository, transactionManager)
  }
}
