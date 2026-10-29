package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.event.ApplicationEventMulticaster
import org.springframework.context.event.SimpleApplicationEventMulticaster
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.ThreadPoolExecutor

@Configuration
class EventsConfiguration(
  @Value("\${spring.events.concurrency.core-pool-size}") private val corePoolSize: Int,
  @Value("\${spring.events.concurrency.max-pool-size}") private val maxPoolSize: Int,
  @Value("\${spring.events.concurrency.queue-size}") private val queueSize: Int,
) {
  @Bean(name = ["applicationEventMulticaster"])
  fun simpleApplicationEventMulticaster(): ApplicationEventMulticaster {
    // enables async processing of spring events
    val eventMulticaster = SimpleApplicationEventMulticaster()
    val taskExecutor = ThreadPoolTaskExecutor()
    taskExecutor.corePoolSize = corePoolSize
    taskExecutor.maxPoolSize = maxPoolSize
    taskExecutor.queueCapacity = queueSize
    taskExecutor.threadNamePrefix = "event-"
    taskExecutor.setRejectedExecutionHandler(ThreadPoolExecutor.CallerRunsPolicy())
    taskExecutor.afterPropertiesSet()
    eventMulticaster.setTaskExecutor(taskExecutor)
    return eventMulticaster
  }
}
