package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.SentReferralSummary
import java.util.UUID

interface SentReferralSummariesRepository :
  JpaRepository<SentReferralSummary, UUID>,
  JpaSpecificationExecutor<SentReferralSummary>
