package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.config

import com.github.marschall.spring.batch.inmemory.NullJobRepository
import org.springframework.batch.core.launch.support.SimpleJobLauncher
import org.springframework.batch.core.repository.JobRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor

@Configuration
class BatchConfiguration(
  @Value("\${spring.batch.concurrency.pool-size}") private val poolSize: Int,
  @Value("\${spring.batch.concurrency.queue-size}") private val queueSize: Int,
) {
  @Bean
  fun asyncJobLauncher(jobRepository: JobRepository): SimpleJobLauncher {
    val taskExecutor = ThreadPoolTaskExecutor()
    taskExecutor.corePoolSize = poolSize
    taskExecutor.queueCapacity = queueSize
    taskExecutor.afterPropertiesSet()

    val launcher = SimpleJobLauncher()
    launcher.setJobRepository(NullJobRepository()) // override job repo with a read only version
    launcher.setTaskExecutor(taskExecutor)
    launcher.afterPropertiesSet()
    return launcher
  }
}
