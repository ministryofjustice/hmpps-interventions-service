package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jobs.movereferrals

import mu.KLogging
import net.logstash.logback.argument.StructuredArguments.kv
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.item.ItemProcessor
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.*
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.*
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.ActionPlanService
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.CaseNoteService
import java.time.OffsetDateTime
import java.util.*

@Component
@JobScope
class MoveReferralsProcessor(
  private val interventionRepository: InterventionRepository,
  private val referralRepository: ReferralRepository,
  private val caseNoteRepository: CaseNoteRepository,
  @Value("#{jobParameters['toContract']}")  val toContract: String,
  @Value("#{jobParameters['fromContract']}") val fromContract: String,
) : ItemProcessor<Referral, Referral> {
  companion object : KLogging()

  override fun process(referral: Referral): Referral {
    logger.debug("processing referral {}", kv("referralId", referral.id))
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
      subject = "Authority to transfer case",
      body = "On behalf of Advance Charity Ltd, I request and give permission for this referral to be transferred to Women in Prison, as of December 2022.",
      sentBy = AuthUser.interventionsServiceUser,
      sentAt = OffsetDateTime.now(),
    )
    caseNoteRepository.save(caseNote)
  }
}
