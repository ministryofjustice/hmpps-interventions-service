package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.reporting.serviceprovider

import ch.qos.logback.classic.Logger
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.batch.core.BatchStatus
import org.springframework.batch.core.JobExecution
import org.springframework.batch.core.StepExecution
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.reporting.serviceprovider.performance.PerformanceReportSqlTimings
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.LoggingMemoryAppender
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class PerformanceReportSqlTimingsTest {
  @Test
  fun `aggregates calls without per-row logs and isolates report executions`() {
    val logger = LoggerFactory.getLogger(PerformanceReportSqlTimings::class.java) as Logger
    val appender = LoggingMemoryAppender().apply { start() }
    logger.addAppender(appender)
    try {
      val firstReport = PerformanceReportSqlTimings()
      val secondReport = PerformanceReportSqlTimings()
      firstReport.record(
        "performance_report_first_attendance",
        2.hours + 8.minutes + 32.seconds + 529.milliseconds,
      )
      secondReport.record("performance_report_first_attendance", 10.milliseconds)
      firstReport.record("performance_report_first_attendance", 1.milliseconds)
      firstReport.record("performance_report_attendances_count", 1.hours + 2.minutes + 3.seconds + 4.milliseconds)

      assertThat(appender.logEvents).isEmpty()

      val failedStep = StepExecution("writeToCsvStep", JobExecution(1L), 2L)
      failedStep.status = BatchStatus.FAILED
      assertThat(firstReport.afterStep(failedStep)).isNull()
      assertThat(appender.logEvents.map { it.formattedMessage }).containsExactly(
        "Performance report SQL summary: jobExecutionId=1, stepExecutionId=2, sqlFunction=performance_report_first_attendance, calls=2, totalElapsed=2h8m32s530ms",
        "Performance report SQL summary: jobExecutionId=1, stepExecutionId=2, sqlFunction=performance_report_attendances_count, calls=1, totalElapsed=1h2m3s4ms",
      )

      appender.reset()
      secondReport.afterStep(StepExecution("writeToCsvStep", JobExecution(3L), 4L))
      assertThat(appender.logEvents.map { it.formattedMessage }).containsExactly(
        "Performance report SQL summary: jobExecutionId=3, stepExecutionId=4, sqlFunction=performance_report_first_attendance, calls=1, totalElapsed=10ms",
      )
    } finally {
      logger.detachAppender(appender)
      appender.stop()
    }
  }
}
