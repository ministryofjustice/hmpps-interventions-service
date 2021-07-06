package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.controller

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.authorization.UserMapper
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.ReportingService
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.JwtTokenFactory
import java.time.LocalDate

internal class ReportingControllerTest {
  private val reportingService = mock<ReportingService>()
  private val userMapper = UserMapper()
  private val reportingController = ReportingController(reportingService, userMapper)
  private val tokenFactory = JwtTokenFactory()

  @Test
  fun `get reporting data`() {
    val token = tokenFactory.create()
    val fromDate = LocalDate.parse("2021-06-01")
    val toDate = LocalDate.parse("2021-06-06")

    reportingController.getReportData(fromDate, toDate, token)
    whenever(reportingService.getReportData(any(), any(), any())).thenReturn(null)
    verify(reportingService).getReportData(any(), any(), any())
  }
}
