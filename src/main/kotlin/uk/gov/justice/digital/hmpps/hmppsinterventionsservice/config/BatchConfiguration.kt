package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.config

import org.springframework.batch.core.configuration.JobRegistry
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing
import org.springframework.batch.core.configuration.support.JobRegistryBeanPostProcessor
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.batch.core.launch.support.SimpleJobLauncher
import org.springframework.batch.core.repository.JobRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor

@Configuration
@EnableBatchProcessing
class BatchConfiguration(
  @Value("\${spring.batch.concurrency.pool-size}") private val poolSize: Int,
  @Value("\${spring.batch.concurrency.queue-size}") private val queueSize: Int,
  private val jobRegistry: JobRegistry,
) {
  @Bean
  fun asyncJobLauncher(jobRepository: JobRepository): JobLauncher {
    val taskExecutor = ThreadPoolTaskExecutor()
    taskExecutor.corePoolSize = poolSize
    taskExecutor.setQueueCapacity(queueSize)
    taskExecutor.afterPropertiesSet()

    val launcher = SimpleJobLauncher()
    launcher.setJobRepository(jobRepository)
    launcher.setTaskExecutor(taskExecutor)
    launcher.afterPropertiesSet()
    return launcher
  }

  @Bean
  fun jobRegistryBeanPostProcessor(): JobRegistryBeanPostProcessor? {
    val postProcessor = JobRegistryBeanPostProcessor()
    postProcessor.setJobRegistry(jobRegistry)
    return postProcessor
  }
}
