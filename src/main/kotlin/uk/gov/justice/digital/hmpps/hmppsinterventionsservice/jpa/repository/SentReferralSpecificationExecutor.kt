package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.SentReferralSummary
import java.util.UUID

interface SentReferralSpecificationExecutor {
  fun findReferralIds(spec: Specification<SentReferralSummary>, pageable: Pageable): Page<UUID>
}
