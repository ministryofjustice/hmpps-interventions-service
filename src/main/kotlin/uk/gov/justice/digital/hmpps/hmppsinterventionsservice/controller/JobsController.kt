package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.controller

import org.springframework.batch.core.ExitStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.JobLauncherService

data class JobConfiguration(
  val jobName: String,
)

@RestController
class JobsController(
  private val jobLauncherService: JobLauncherService,
) {
  @PostMapping("/jobs/one-off")
  @PreAuthorize("hasRole('ROLE_INTERVENTIONS_BATCH_USER')")
  fun runOneOffJob(
    @RequestBody jobConfiguration: JobConfiguration,
  ): ResponseEntity<Any> {
    val execution = jobLauncherService.launchJobByName(jobConfiguration.jobName)
    return when (execution.exitStatus) {
      ExitStatus.COMPLETED -> {
        ResponseEntity.ok().build()
      }
      else -> {
        ResponseEntity.internalServerError().body(execution.exitStatus)
      }
    }
  }
}
