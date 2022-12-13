package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jobs.oneoff.movereferrals

import mu.KLogging
import net.logstash.logback.argument.StructuredArguments.kv
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.item.ItemProcessor
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AuthUser
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.CaseNote
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Referral
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.CaseNoteRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.InterventionRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ReferralRepository
import java.time.OffsetDateTime
import java.util.UUID

@Component
@JobScope
class MoveReferralsProcessor(
  private val interventionRepository: InterventionRepository,
  private val referralRepository: ReferralRepository,
  private val caseNoteRepository: CaseNoteRepository,
  @Value("#{jobParameters['toContract']}") val toContract: String,
  @Value("#{jobParameters['fromContract']}") val fromContract: String,
) : ItemProcessor<Referral, Referral> {
  companion object : KLogging()

  override fun process(referral: Referral): Referral {
    logger.info("processing referral {} for transfer", kv("referralId", referral.id))
    addCaseNoteEntry(referral)
    return updateReferralIntervention(referral)
  }

  fun updateReferralIntervention(referral: Referral): Referral {
    val newIntervention = interventionRepository.findByDynamicFrameworkContractContractReference(toContract)
    referral.intervention = newIntervention
    return referralRepository.save(referral)
  }

  fun addCaseNoteEntry(referral: Referral) {
    val caseNote = CaseNote(
      id = UUID.randomUUID(),
      referral = referral,
      subject = "Case transferred",
      body = "Automated transfer from contract $fromContract to contract $toContract",
      sentBy = AuthUser.interventionsServiceUser,
      sentAt = OffsetDateTime.now(),
    )
    caseNoteRepository.save(caseNote)
  }
}
