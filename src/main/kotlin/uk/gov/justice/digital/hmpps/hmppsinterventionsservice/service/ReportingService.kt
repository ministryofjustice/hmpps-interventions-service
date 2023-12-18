package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service

import org.springframework.batch.core.Job
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.authorization.ServiceProviderAccessScopeMapper
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AuthUser
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.reporting.BatchUtils
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.reporting.serviceprovider.performance.PerformanceReportHandler
import java.nio.file.Files.createTempDirectory
import java.time.Instant
import java.time.LocalDate
import java.util.stream.Collectors
import kotlin.io.path.pathString

@Service
class ReportingService(
  private val asyncJobLauncher: JobLauncher,
  private val reportingTaskExecutor: ThreadPoolTaskExecutor,
  private val performanceReportHandler: PerformanceReportHandler,
  private val ndmisPerformanceReportJob: Job,
  private val serviceProviderAccessScopeMapper: ServiceProviderAccessScopeMapper,
  private val batchUtils: BatchUtils,
  private val hmppsAuthService: HMPPSAuthService,
) {
  fun generateServiceProviderPerformanceReport(from: LocalDate, to: LocalDate, user: AuthUser) {
    val contracts = serviceProviderAccessScopeMapper.fromUser(user).contracts
    val userDetail = hmppsAuthService.getUserDetail(user)

    val contractReferences = contracts.stream().map { it.contractReference }.collect(Collectors.toList())

    reportingTaskExecutor.submit {
      performanceReportHandler.handleReport(
        contractReferences = contractReferences,
        userId = user.id,
        userFirstname = userDetail.firstName,
        userEmail = userDetail.email,
        from = batchUtils.parseLocalDateToOffsetDateTime(from),
        to = batchUtils.parseLocalDateToOffsetDateTime(to.plusDays(1)), // 'to' is inclusive
        timestamp = Instant.now().toEpochMilli().toString(),
      )
    }
  }

  fun generateNdmisPerformanceReport() {
    val outputDir = createTempDirectory("test")
    asyncJobLauncher.run(
      ndmisPerformanceReportJob,
      JobParametersBuilder()
        .addString("outputPath", outputDir.pathString)
        .addString("timestamp", Instant.now().toEpochMilli().toString())
        .toJobParameters(),
    )
  }
}
