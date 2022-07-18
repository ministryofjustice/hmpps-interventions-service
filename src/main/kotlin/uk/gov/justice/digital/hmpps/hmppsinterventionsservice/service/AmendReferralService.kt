package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.AmendComplexityLevelDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.AmendNeedAndRequirementDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.ReferralAmendmentDetails
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.ReferralEventPublisher
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AuthUser
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Changelog
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Referral
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ChangelogRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ReferralRepository
import java.time.OffsetDateTime
import java.util.UUID
import javax.transaction.Transactional

@Service
@Transactional
class AmendReferralService(
  private val referralEventPublisher: ReferralEventPublisher,
  val changelogRepository: ChangelogRepository,
  val referralRepository: ReferralRepository
) {

  fun updateComplexityLevel(referral: Referral, update: AmendComplexityLevelDTO, actor: AuthUser): AmendComplexityLevelDTO? {
    val complexityLevel = referral.complexityLevelIds?.getValue(update.serviceCategoryId)

    referral.complexityLevelIds?.put(update.serviceCategoryId, update.complexityLevelId)

    val oldValue = ReferralAmendmentDetails(listOf(complexityLevel.toString()))
    val newValue = ReferralAmendmentDetails(listOf(update.complexityLevelId.toString()))

    val changelog = Changelog(
      referral.id,
      UUID.randomUUID(),
      "COMPLEXITY_LEVEL",
      oldValue,
      newValue,
      update.reasonForChange,
      OffsetDateTime.now(),
      actor.id
    )
    changelogRepository.save(changelog)
    referralRepository.save(referral)
    referralEventPublisher.referralComplexityChangedEvent(referral)

    return update
  }

  fun updateNeedAndRequirement(referral: Referral, update: AmendNeedAndRequirementDTO, actor: AuthUser): AmendNeedAndRequirementDTO? {

    val furtherInformation = update.furtherInformation
    val additionalNeedsInformation = update.additionalNeedsInformation
    val accessibilityNeeds = update.accessibilityNeeds
    val needsInterpreter = update.needsInterpreter
    val interpreterLanguage = update.interpreterLanguage
    val hasAdditionalResponsibilities = update.hasAdditionalResponsibilities
    val whenUnavailable = update.whenUnavailable

    val oldValue = ReferralAmendmentDetails(
      listOf(
        furtherInformation.toString(), additionalNeedsInformation.toString(), accessibilityNeeds.toString(), needsInterpreter.toString(),
        interpreterLanguage.toString(), hasAdditionalResponsibilities.toString(), whenUnavailable.toString()
      )
    )
    val newValue = ReferralAmendmentDetails(
      listOf(
        update.furtherInformation.toString(), update.additionalNeedsInformation.toString(), update.accessibilityNeeds.toString(), update.needsInterpreter.toString(),
        update.interpreterLanguage.toString(), update.hasAdditionalResponsibilities.toString(), update.whenUnavailable.toString()
      )
    )

    val changelog = Changelog(
      referral.id,
      UUID.randomUUID(),
      "NEED_AND_REQUIREMENT",
      oldValue,
      newValue,
      update.reasonForChange,
      OffsetDateTime.now(),
      actor.id
    )
    changelogRepository.save(changelog)
    referralRepository.save(referral)
    referralEventPublisher.referralNeedAndRequirementsChangedEvent(referral)

    return update
  }
}
