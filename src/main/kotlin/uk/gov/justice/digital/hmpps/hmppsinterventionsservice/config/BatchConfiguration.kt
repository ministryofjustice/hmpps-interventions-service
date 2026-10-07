package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.config

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing
import org.springframework.batch.core.configuration.annotation.EnableJdbcJobRepository
import org.springframework.batch.core.configuration.support.MapJobRegistry
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.launch.JobOperator
import org.springframework.batch.core.launch.support.TaskExecutorJobOperator
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.transaction.annotation.EnableTransactionManagement

@Configuration
@EnableBatchProcessing
@EnableJdbcJobRepository(
  dataSourceRef = "memoryDataSource",
  transactionManagerRef = "batchTransactionManager",
  databaseType = "H2",
)
@EnableTransactionManagement
class BatchConfiguration(
  @Value("\${spring.batch.concurrency.pool-size}") private val poolSize: Int,
  @Value("\${spring.batch.concurrency.queue-size}") private val queueSize: Int,
) {

  @Bean("asyncJobOperator")
  fun asyncJobOperator(jobRepository: JobRepository): JobOperator {
    val taskExecutor = ThreadPoolTaskExecutor()
    taskExecutor.corePoolSize = poolSize
    taskExecutor.queueCapacity = queueSize
    taskExecutor.afterPropertiesSet()

    val operator = TaskExecutorJobOperator()
    operator.setJobRepository(jobRepository)
    // jobs are started by passing the Job itself, so the registry is only needed to satisfy the operator (as Batch does for its default operator)
    operator.setJobRegistry(MapJobRegistry())
    operator.setTaskExecutor(taskExecutor)
    operator.afterPropertiesSet()
    return operator
  }

  @Bean("batchJobBuilder")
  fun batchJobBuilder(jobRepository: JobRepository): JobBuilder = JobBuilder("batchJobBuilder", jobRepository)

  @Bean("batchStepBuilder")
  fun batchStepBuilder(jobRepository: JobRepository): StepBuilder = StepBuilder("batchStepBuilder", jobRepository)
}
