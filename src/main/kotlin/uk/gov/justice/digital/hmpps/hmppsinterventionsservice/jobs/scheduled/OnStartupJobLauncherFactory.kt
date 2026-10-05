package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jobs.scheduled

import mu.KLogging
import net.logstash.logback.argument.StructuredArguments
import org.springframework.batch.core.converter.DefaultJobParametersConverter
import org.springframework.batch.core.job.Job
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.batch.core.launch.support.SimpleJvmExitCodeMapper
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component
import org.springframework.util.StringUtils
import java.util.Properties
import kotlin.system.exitProcess

@Component
class OnStartupJobLauncherFactory(
  @Qualifier("asyncJobLauncher")
  private val jobLauncher: JobLauncher,
) {
  companion object : KLogging()

  private val exitCodeMapper = SimpleJvmExitCodeMapper()
  private val jobParametersConverter = DefaultJobParametersConverter()

  fun makeLauncher(jobName: String, entryPoint: (args: ApplicationArguments) -> Int): ApplicationRunner = ApplicationRunner { args ->
    if (args.getOptionValues("jobName")?.contains(jobName) == true) {
      logger.info("running one off job {}", StructuredArguments.kv("jobName", jobName))
      exitProcess(entryPoint(args))
    }
  }

  fun makeBatchLauncher(job: Job): ApplicationRunner = makeLauncher(job.name, buildEntryPoint(job, jobLauncher))
  private fun buildEntryPoint(job: Job, jobLauncher: JobLauncher): (args: ApplicationArguments) -> Int {
    val entryPoint = fun(args: ApplicationArguments): Int {
      val rawParams = jobParametersConverter.getJobParameters(
        StringUtils.splitArrayElementsIntoProperties(args.nonOptionArgs.toTypedArray(), "=") ?: Properties(),
      )

      val nextParams = job.jobParametersIncrementer?.getNext(rawParams) ?: rawParams

      val execution = jobLauncher.run(job, nextParams)
      return exitCodeMapper.intValue(execution.exitStatus.exitCode)
    }

    return entryPoint
  }
}
