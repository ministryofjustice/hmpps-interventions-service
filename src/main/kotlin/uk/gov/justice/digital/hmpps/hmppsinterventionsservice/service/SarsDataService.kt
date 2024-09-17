package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Appointment
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Referral
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.DeliverySessionRepository
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
) {

  fun getSarsReferralData(crn: String, fromDateString: String? = null, toDateString: String? = null): List<SarsReferralData> {
    val referrals: List<Referral> = if (fromDateString != null && toDateString != null) {
      val fromDate = OffsetDateTime.of(
        LocalDate.parse(fromDateString, DateTimeFormatter.ISO_DATE),
        LocalTime.MIDNIGHT,
        ZoneOffset.UTC,
      )
      val toDate =
        OffsetDateTime.of(LocalDate.parse(toDateString, DateTimeFormatter.ISO_DATE), LocalTime.MIDNIGHT, ZoneOffset.UTC)
      referralRepository.referralForSar(crn, fromDate, toDate)
    } else if (fromDateString == null && toDateString != null) {
      val fromDate = OffsetDateTime.of(
        LocalDate.parse("1970-01-01", DateTimeFormatter.ISO_DATE),
        LocalTime.MIDNIGHT,
        ZoneOffset.UTC,
      )
      val toDate =
        OffsetDateTime.of(LocalDate.parse(toDateString, DateTimeFormatter.ISO_DATE), LocalTime.MIDNIGHT, ZoneOffset.UTC)
      referralRepository.referralForSar(crn, fromDate, toDate)
    } else if (fromDateString != null && toDateString == null) {
      val fromDate = OffsetDateTime.of(
        LocalDate.parse(fromDateString, DateTimeFormatter.ISO_DATE),
        LocalTime.MIDNIGHT,
        ZoneOffset.UTC,
      )
      val toDate =
        OffsetDateTime.of(LocalDate.now(), LocalTime.MIDNIGHT, ZoneOffset.UTC)
      referralRepository.referralForSar(crn, fromDate, toDate)
    } else {
      referralRepository.findByServiceUserCRN(crn)
    }
    return referrals.map { referral ->
      SarsReferralData(
        referral,
        deliverySessionRepository.findAllByReferralId(referral.id)
          .flatMap { it.appointments },
      )
    }
  }
}
data class SarsReferralData(
  val referral: Referral,
  val appointments: List<Appointment>,
)
