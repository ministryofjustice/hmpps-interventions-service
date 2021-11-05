package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service

import org.springframework.batch.core.JobExecution
import org.springframework.batch.core.JobParameters
import org.springframework.batch.core.configuration.JobRegistry
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.stereotype.Service

@Service
class JobLauncherService(
  private val jobLauncher: JobLauncher,
  private val jobRegistry: JobRegistry,
) {
  fun launchJobByName(jobName: String, jobParameters: JobParameters? = JobParameters()): JobExecution =
    jobLauncher.run(jobRegistry.getJob(jobName), jobParameters)
}
