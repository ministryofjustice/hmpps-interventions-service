package uk.gov.justice.digital.hmpps.hmppsinterventionsservice

import org.springframework.batch.core.job.Job
import org.springframework.batch.core.job.parameters.JobParametersBuilder
import org.springframework.batch.core.launch.JobOperator
import org.springframework.beans.factory.getBean
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.ConfigurableApplicationContext
import java.time.Instant

@SpringBootApplication
class HmppsInterventionsService

fun main(args: Array<String>) {
  val context: ConfigurableApplicationContext = runApplication<HmppsInterventionsService>(*args)

  try {
    val jobOperator = context.getBean<JobOperator>("asyncJobOperator")
    val job = context.getBean<Job>("upsertContractsJob") // 👈 hard-coded job name

    val jobParameters = JobParametersBuilder()
      .addLong("timestamp", Instant.now().epochSecond) // unique run ID
      .addString("outputPath", "/tmp")
      .toJobParameters()

    jobOperator.start(job, jobParameters)

    println("✅ Batch job 'upsertContractsJob' started successfully!")
  } catch (ex: Exception) {
    ex.printStackTrace()
    println("❌ Failed to start batch job: ${ex.message}")
  }
}
