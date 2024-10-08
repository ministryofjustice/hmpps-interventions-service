package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository

import org.springframework.data.domain.Page
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.ReferralSummaryQuery

interface ReferralSummaryRepository {
  fun getReferralSummaries(referralSummaryQuery: ReferralSummaryQuery): Page<ReferralSummary>
}
