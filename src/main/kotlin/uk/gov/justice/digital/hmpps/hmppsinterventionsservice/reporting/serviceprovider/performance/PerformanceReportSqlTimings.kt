package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.reporting.serviceprovider.performance

import mu.KLogging
import org.springframework.batch.core.ExitStatus
import org.springframework.batch.core.StepExecution
import org.springframework.batch.core.StepExecutionListener
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.stereotype.Component
import kotlin.time.Duration

@Component
@StepScope
class PerformanceReportSqlTimings : StepExecutionListener {
  companion object : KLogging()

  private data class Timing(var calls: Long = 0, var totalDuration: Duration = Duration.ZERO)

  private val timings = linkedMapOf<String, Timing>()

  fun record(functionName: String, duration: Duration) {
    val timing = timings.getOrPut(functionName) { Timing() }
    timing.calls++
    timing.totalDuration += duration
  }

  override fun afterStep(stepExecution: StepExecution): ExitStatus? {
    timings.forEach { (functionName, timing) ->
      logger.info(
        "Performance report SQL summary: jobExecutionId={}, stepExecutionId={}, sqlFunction={}, calls={}, totalElapsed={}",
        stepExecution.jobExecution.id,
        stepExecution.id,
        functionName,
        timing.calls,
        timing.totalDuration.toPerformanceReportString(),
      )
    }
    return null
  }
}
