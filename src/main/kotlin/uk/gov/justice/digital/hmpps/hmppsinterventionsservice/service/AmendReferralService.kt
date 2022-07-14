package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.AmendComplexityLevelDTO
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
}
