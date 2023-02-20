package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.DraftOasysRiskInformationDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AuthUser
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.DraftOasysRiskInformation
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.DraftReferral
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.AuthUserRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.DraftOasysRiskInformationRepository
import java.time.OffsetDateTime
import java.util.UUID

@Service
@Transactional
class DraftOasysRiskInformationService(
  val draftOasysRiskInformationRepository: DraftOasysRiskInformationRepository,
  val authUserRepository: AuthUserRepository,
) {
  fun updateDraftOasysRiskInformation(referral: DraftReferral, draftOasysRiskInformationDTO: DraftOasysRiskInformationDTO, user: AuthUser): DraftOasysRiskInformation {
    val draftOasysRiskInformation = DraftOasysRiskInformation(
      referralId = referral.id,
      updatedAt = OffsetDateTime.now(),
      updatedBy = authUserRepository.save(user),
      riskSummaryWhoIsAtRisk = draftOasysRiskInformationDTO.riskSummaryWhoIsAtRisk,
      riskSummaryNatureOfRisk = draftOasysRiskInformationDTO.riskSummaryNatureOfRisk,
      riskSummaryRiskImminence = draftOasysRiskInformationDTO.riskSummaryRiskImminence,
      riskToSelfSuicide = draftOasysRiskInformationDTO.riskToSelfSuicide,
      riskToSelfSelfHarm = draftOasysRiskInformationDTO.riskToSelfSelfHarm,
      riskToSelfHostelSetting = draftOasysRiskInformationDTO.riskToSelfHostelSetting,
      riskToSelfVulnerability = draftOasysRiskInformationDTO.riskToSelfVulnerability,
      additionalInformation = draftOasysRiskInformationDTO.additionalInformation,
    )
    return draftOasysRiskInformationRepository.save(draftOasysRiskInformation)
  }

  fun getDraftOasysRiskInformation(id: UUID): DraftOasysRiskInformation? {
    return draftOasysRiskInformationRepository.findByIdOrNull(id)
  }

  fun deleteDraftOasysRiskInformation(id: UUID) {
    if (draftOasysRiskInformationRepository.existsById(id)) {
      draftOasysRiskInformationRepository.deleteById(id)
    }
  }
}
