package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.model.ReferralSummary
import java.util.UUID

interface ReferralSummariesRepository : JpaRepository<ReferralSummary, UUID>, JpaSpecificationExecutor<ReferralSummary>
