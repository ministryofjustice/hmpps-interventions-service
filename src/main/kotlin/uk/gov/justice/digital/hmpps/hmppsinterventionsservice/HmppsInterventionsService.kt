package uk.gov.justice.digital.hmpps.hmppsinterventionsservice


import org.springframework.batch.core.Job
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.ConfigurableApplicationContext
import java.time.Instant

@SpringBootApplication
class HmppsInterventionsService

fun main(args: Array<String>) {
    val context: ConfigurableApplicationContext = runApplication<HmppsInterventionsService>(*args)

    try {
        val jobLauncher = context.getBean("asyncJobLauncher", JobLauncher::class.java)
        val job = context.getBean("upsertContractsJob", Job::class.java) // 👈 hard-coded job name

        val jobParameters = JobParametersBuilder()
            .addLong("timestamp", Instant.now().epochSecond)
            .addString("outputPath", "/tmp")// unique run ID
            .toJobParameters()

        jobLauncher.run(job, jobParameters)

        println("✅ Batch job 'upsertContractsJob' started successfully!")
    } catch (ex: Exception) {
        ex.printStackTrace()
        println("❌ Failed to start batch job: ${ex.message}")
    }
}

