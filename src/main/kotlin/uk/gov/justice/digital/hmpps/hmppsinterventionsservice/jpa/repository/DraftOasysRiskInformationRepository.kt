package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.DraftOasysRiskInformation
import java.util.UUID

interface DraftOasysRiskInformationRepository : JpaRepository<DraftOasysRiskInformation, UUID> {
  fun deleteByReferralId(referralId: UUID)
}
