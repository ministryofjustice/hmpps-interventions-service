package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository

import org.springframework.data.domain.Page
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.DashboardType
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AuthUser
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ServiceProviderSentReferralSummary
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.ReferralSummaryQuery

interface ReferralSummaryRepository {
  fun getSentReferralSummaries(authUser: AuthUser, serviceProviders: List<String>, dashboardType: DashboardType? = null): List<ServiceProviderSentReferralSummary>
  fun getReferralSummaries(referralSummaryQuery: ReferralSummaryQuery): Page<ReferralSummary>
}
