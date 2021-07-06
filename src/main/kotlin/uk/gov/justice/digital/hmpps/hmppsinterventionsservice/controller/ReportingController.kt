package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.controller

import org.springframework.format.annotation.DateTimeFormat
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.authorization.UserMapper
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.ReferralReportDataDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.ReportingService
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset

@RestController
class ReportingController(
  private val reportingService: ReportingService,
  private val userMapper: UserMapper,
) {
  @GetMapping("/performance-report")
  fun getReportData(
    @RequestParam(name = "fromIncludingDate", required = true) @DateTimeFormat(pattern = "yyyy-MM-dd") fromIncludingDate: LocalDate,
    @RequestParam(name = "toIncludingDate", required = true) @DateTimeFormat(pattern = "yyyy-MM-dd") toIncludingDate: LocalDate,
    authentication: JwtAuthenticationToken,
  ): List<ReferralReportDataDTO> {
    val fromDateOffset = OffsetDateTime.of(fromIncludingDate.atStartOfDay(), ZoneOffset.UTC)
    val toDateOffset = OffsetDateTime.of(toIncludingDate.atStartOfDay(), ZoneOffset.UTC).plusDays(1)
    val user = userMapper.fromToken(authentication)

    return reportingService.getReportData(fromDateOffset, toDateOffset, user)
  }
}
