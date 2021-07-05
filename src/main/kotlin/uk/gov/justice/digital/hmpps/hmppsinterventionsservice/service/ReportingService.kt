package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.util.UriComponentsBuilder
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.authorization.ServiceProviderAccessScopeMapper
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.ReferralReportDataDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AuthUser
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ReferralRepository
import java.time.OffsetDateTime
import java.util.*
import javax.transaction.Transactional

@Service
@Transactional
class ReportingService(
  val referralRepository: ReferralRepository,
  val serviceProviderUserAccessScopeMapper: ServiceProviderAccessScopeMapper,
  @Value("\${interventions-ui.baseurl}") private val interventionsUIBaseURL: String,
  @Value("\${interventions-ui.locations.referral-details}") private val interventionsUIReferralDetailsLocation: String,
) {
  fun getReportData(to: OffsetDateTime, from: OffsetDateTime, user: AuthUser): List<ReferralReportDataDTO> {
    val contracts = serviceProviderUserAccessScopeMapper.fromUser(user).contracts
    val referrals = referralRepository.reportingData(to, from, contracts)
    return referrals.map {ReferralReportDataDTO.from(it, buildUrl(it.referralId))}
  }

  private fun buildUrl(referralId: UUID): String {
    return UriComponentsBuilder.fromHttpUrl(interventionsUIBaseURL)
      .path(interventionsUIReferralDetailsLocation)
      .buildAndExpand(referralId)
      .toString()
  }
}