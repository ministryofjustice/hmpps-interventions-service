package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.config

import org.springframework.batch.core.launch.JobLauncher
import org.springframework.batch.core.launch.support.SimpleJobLauncher
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.repository.support.JobRepositoryFactoryBean
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.transaction.PlatformTransactionManager
import javax.sql.DataSource

@Configuration
class BatchConfiguration(
  private val dataSource: DataSource,
  private val transactionManager: PlatformTransactionManager,
  @Value("\${spring.batch.concurrency.pool-size}") private val poolSize: Int,
  @Value("\${spring.batch.concurrency.queue-size}") private val queueSize: Int,
) {
  @Throws(Exception::class)
  protected fun createJobRepository(): JobRepository? {
    val factory = JobRepositoryFactoryBean()
    factory.setDataSource(dataSource)
    factory.setTransactionManager(transactionManager)
    factory.setIsolationLevelForCreate("ISOLATION_REPEATABLE_READ")
    return factory.getObject()
  }

  @Bean
  fun asyncJobLauncher(jobRepository: JobRepository): JobLauncher {
    val taskExecutor = ThreadPoolTaskExecutor()
    taskExecutor.corePoolSize = poolSize
    taskExecutor.queueCapacity = queueSize
    taskExecutor.afterPropertiesSet()

    val launcher = SimpleJobLauncher()
    launcher.setJobRepository(createJobRepository())
    launcher.setTaskExecutor(taskExecutor)
    launcher.afterPropertiesSet()
    return launcher
  }
}
