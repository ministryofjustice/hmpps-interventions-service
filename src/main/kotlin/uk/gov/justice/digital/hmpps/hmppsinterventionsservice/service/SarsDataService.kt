package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Appointment
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.CaseNote
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.DraftReferral
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.DynamicFrameworkContract
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Referral
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.CaseNoteRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.DeliverySessionRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.DraftReferralRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.InterventionRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ReferralRepository
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@Service
class SarsDataService(
  val referralRepository: ReferralRepository,
  val deliverySessionRepository: DeliverySessionRepository,
  val interventionRepository: InterventionRepository,
  val caseNoteRepository: CaseNoteRepository,
  val draftReferralRepository: DraftReferralRepository,
) {

  fun getSarsReferralData(crn: String, fromDateString: String? = null, toDateString: String? = null): List<SarsReferralData> {
    var fromDate: OffsetDateTime? = null
    var toDate: OffsetDateTime? = null

    when {
      fromDateString != null && toDateString != null -> {
        fromDate = OffsetDateTime.of(LocalDate.parse(fromDateString, DateTimeFormatter.ISO_DATE), LocalTime.MIDNIGHT, ZoneOffset.UTC)
        toDate = OffsetDateTime.of(LocalDate.parse(toDateString, DateTimeFormatter.ISO_DATE), LocalTime.MIDNIGHT, ZoneOffset.UTC)
      }
      fromDateString == null && toDateString != null -> {
        fromDate = OffsetDateTime.of(LocalDate.parse("1970-01-01", DateTimeFormatter.ISO_DATE), LocalTime.MIDNIGHT, ZoneOffset.UTC)
        toDate = OffsetDateTime.of(LocalDate.parse(toDateString, DateTimeFormatter.ISO_DATE), LocalTime.MIDNIGHT, ZoneOffset.UTC)
      }
      fromDateString != null -> {
        fromDate = OffsetDateTime.of(LocalDate.parse(fromDateString, DateTimeFormatter.ISO_DATE), LocalTime.MIDNIGHT, ZoneOffset.UTC)
        toDate = OffsetDateTime.of(LocalDate.now(), LocalTime.MIDNIGHT, ZoneOffset.UTC)
      }
    }

    val referrals: List<Referral> = if (fromDateString != null || toDateString != null) {
      referralRepository.referralForSar(crn, fromDate!!, toDate!!)
    } else {
      referralRepository.findByServiceUserCRN(crn)
    }

    val draftReferrals: List<DraftReferral> = if (fromDateString != null || toDateString != null) {
      draftReferralRepository.draftReferralForSar(crn, fromDate!!, toDate!!)
    } else {
      draftReferralRepository.findDraftOnlyByServiceUserCRN(crn)
    }

    return referrals.map { referral ->
      SarsReferralData(
        referral,
        deliverySessionRepository.findAllByReferralId(referral.id)
          .flatMap { it.appointments },
        dynamicFrameworkContract = interventionRepository.findById(referral.intervention.id).map { it.dynamicFrameworkContract }.get(),
        caseNotes = caseNoteRepository.findAllByReferralId(referral.id, null).toList(),
        draftReferrals = draftReferrals,
      )
    }.ifEmpty {
      if (draftReferrals.isNotEmpty()) {
        listOf(
          SarsReferralData(
            referral = null,
            appointments = emptyList(),
            dynamicFrameworkContract = null,
            caseNotes = emptyList(),
            draftReferrals = draftReferrals,
          ),
        )
      } else {
        emptyList()
      }
    }
  }
}

data class SarsReferralData(
  val referral: Referral?,
  val appointments: List<Appointment>,
  val dynamicFrameworkContract: DynamicFrameworkContract?,
  val caseNotes: List<CaseNote>,
  val draftReferrals: List<DraftReferral> = emptyList(),
)
