package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jobs.scheduled

import mu.KLogging
import net.logstash.logback.argument.StructuredArguments
import org.springframework.batch.core.converter.DefaultJobParametersConverter
import org.springframework.batch.core.job.Job
import org.springframework.batch.core.job.parameters.JobParametersIncrementer
import org.springframework.batch.core.launch.JobOperator
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
  @Qualifier("asyncJobOperator")
  private val jobOperator: JobOperator,
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

  // Spring Batch 6 ignores the parameters passed to JobOperator.start when the job itself defines an incrementer,
  // so jobs pass their incrementer here instead, to fill in defaults (e.g. a timestamp) around the command line parameters
  fun makeBatchLauncher(job: Job, defaultParameters: JobParametersIncrementer? = null): ApplicationRunner = makeLauncher(job.name, buildEntryPoint(job, jobOperator, defaultParameters))

  private fun buildEntryPoint(job: Job, jobOperator: JobOperator, defaultParameters: JobParametersIncrementer?): (args: ApplicationArguments) -> Int {
    val entryPoint = fun(args: ApplicationArguments): Int {
      val rawParams = jobParametersConverter.getJobParameters(
        StringUtils.splitArrayElementsIntoProperties(args.nonOptionArgs.toTypedArray(), "=") ?: Properties(),
      )

      val nextParams = defaultParameters?.getNext(rawParams) ?: rawParams

      val execution = jobOperator.start(job, nextParams)
      return exitCodeMapper.intValue(execution.exitStatus.exitCode)
    }

    return entryPoint
  }
}
