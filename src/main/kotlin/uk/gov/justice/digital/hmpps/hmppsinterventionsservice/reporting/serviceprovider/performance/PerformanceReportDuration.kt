package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.reporting.serviceprovider.performance

import kotlin.time.Duration

internal fun Duration.toPerformanceReportString(): String {
  var remainingMillis = inWholeMilliseconds
  val hours = remainingMillis / 3_600_000
  remainingMillis %= 3_600_000
  val minutes = remainingMillis / 60_000
  remainingMillis %= 60_000
  val seconds = remainingMillis / 1_000
  val milliseconds = remainingMillis % 1_000

  return buildString {
    if (hours > 0) append("${hours}h")
    if (minutes > 0) append("${minutes}m")
    if (seconds > 0) append("${seconds}s")
    if (milliseconds > 0 || isEmpty()) append("${milliseconds}ms")
  }
}
