package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.controller

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.authorization.UserMapper
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.ReferralReportDataDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.ReportingService
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.*
import javax.transaction.Transactional

@RestController
class ReportingController(
  private val reportingService: ReportingService,
  private val objectMapper: ObjectMapper,
  private val userMapper: UserMapper,
) {
  @GetMapping("/performance-report")
    fun getReportData(
    @RequestParam(name = "fromIncludingDate", required = true) @DateTimeFormat(pattern = "yyyy-MM-dd") fromIncludingDate: Date,
    @RequestParam(name = "toIncludingDate", required = true)  @DateTimeFormat(pattern = "yyyy-MM-dd") toIncludingDate: Date,
    authentication: JwtAuthenticationToken,
    ) : List<ReferralReportDataDTO> {
    val toDateOffset = OffsetDateTime.from(toIncludingDate.toInstant().atOffset(ZoneOffset.UTC))
    val fromDateOffset = OffsetDateTime.from(fromIncludingDate.toInstant().atOffset(ZoneOffset.UTC))
    val user = userMapper.fromToken(authentication)

    return reportingService.getReportData(toDateOffset, fromDateOffset, user)
  }
}
