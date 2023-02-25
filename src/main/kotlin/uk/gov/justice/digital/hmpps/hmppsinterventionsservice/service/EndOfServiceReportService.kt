package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.EndOfServiceReportEventPublisher
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AuthUser
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.EndOfServiceReport
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.EndOfServiceReportOutcome
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Referral
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.AuthUserRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.EndOfServiceReportRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ReferralRepository
import java.time.OffsetDateTime
import java.util.UUID

@Service
@Transactional
class EndOfServiceReportService(
  val authUserRepository: AuthUserRepository,
  val referralRepository: ReferralRepository,
  val endOfServiceReportRepository: EndOfServiceReportRepository,
  val endOfServiceReportEventPublisher: EndOfServiceReportEventPublisher,
  val referralConcluder: ReferralConcluder,
) {

  fun createEndOfServiceReport(
    referralId: UUID,
    createdByUser: AuthUser,
  ): EndOfServiceReport {
    val endOfServiceReport = EndOfServiceReport(
      id = UUID.randomUUID(),
      referral = referralRepository.getReferenceById(referralId),
      createdBy = authUserRepository.save(createdByUser),
      createdAt = OffsetDateTime.now(),
    )
    return endOfServiceReportRepository.findByReferralId(referralId)
      ?: endOfServiceReportRepository.save(endOfServiceReport)
  }

  fun getEndOfServiceReport(endOfServiceReportId: UUID): EndOfServiceReport {
    return endOfServiceReportRepository.findById(endOfServiceReportId).orElseThrow {
      throw EntityNotFoundException("End of service report not found [id=$endOfServiceReportId]")
    }
  }

  fun getEndOfServiceReportByReferralId(referralId: UUID): EndOfServiceReport {
    val referral = getReferral(referralId)

    return referral.endOfServiceReport
      ?: throw EntityNotFoundException("End of service report not found for referral [id=$referralId]")
  }

  fun updateEndOfServiceReport(endOfServiceReportId: UUID, furtherInformation: String?, outcome: EndOfServiceReportOutcome?):
    EndOfServiceReport {
    val endOfServiceReport = getEndOfServiceReport(endOfServiceReportId)

    updateEndOfServiceReport(endOfServiceReport, furtherInformation)
    updateEndOfServiceReportOutcomes(endOfServiceReport, outcome)

    return endOfServiceReportRepository.save(endOfServiceReport)
  }

  fun submitEndOfServiceReport(endOfServiceReportId: UUID, submittedByUser: AuthUser): EndOfServiceReport {
    val draftEndOfServiceReport = getEndOfServiceReport(endOfServiceReportId)
    updateDraftEndOfServiceReportAsSubmitted(draftEndOfServiceReport, submittedByUser)

    endOfServiceReportEventPublisher.endOfServiceReportSubmittedEvent(draftEndOfServiceReport)

    return endOfServiceReportRepository.save(draftEndOfServiceReport)
      .also { referralConcluder.concludeIfEligible(it.referral) }
  }

  private fun updateDraftEndOfServiceReportAsSubmitted(endOfServiceReport: EndOfServiceReport, submittedByUser: AuthUser) {
    endOfServiceReport.submittedBy = authUserRepository.save(submittedByUser)
    endOfServiceReport.submittedAt = OffsetDateTime.now()
  }

  private fun updateEndOfServiceReport(endOfServiceReport: EndOfServiceReport, furtherInformation: String?) {
    furtherInformation?.let { endOfServiceReport.furtherInformation = it }
  }

  private fun updateEndOfServiceReportOutcomes(endOfServiceReport: EndOfServiceReport, outcome: EndOfServiceReportOutcome?) {
    outcome?.let {
      val draftOutcome = endOfServiceReport.outcomes.find { it.desiredOutcome == outcome.desiredOutcome }
      endOfServiceReport.outcomes.remove(draftOutcome)
      endOfServiceReport.outcomes.add(outcome)
    }
  }

  private fun getReferral(referralId: UUID): Referral {
    return referralRepository.findById(referralId).orElseThrow {
      throw EntityNotFoundException("Referral not found [id=$referralId]")
    }
  }
}
