package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.AuthUserDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.SASReferralDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.SASReferralDetailsDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.SASReferralStatus
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.ServiceProviderDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ReferralRepository
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId

@Service
class SASReferralService(
  val referralRepository: ReferralRepository,
) {

  fun getReferralsByCrn(crn: String): List<SASReferralDTO> {
    val rows = referralRepository.getSASReferralsByCrn(crn)

    // Group rows by referralId to aggregate subcontractor service providers
    return rows.groupBy { it.referralId }.map { (_, rowGroup) ->
      val first = rowGroup.first()

      // Collect unique service providers: prime provider + any subcontractors
      val serviceProviders = linkedMapOf<String, ServiceProviderDTO>()
      rowGroup.forEach { row ->
        serviceProviders[row.primeProviderId] = ServiceProviderDTO(name = row.primeProviderName, id = row.primeProviderId)
        row.subProviderId?.let { subId ->
          serviceProviders[subId] = ServiceProviderDTO(name = row.subProviderName!!, id = subId)
        }
      }

      val status = when {
        first.withdrawalReasonCode != null && first.withdrawalComments != null -> SASReferralStatus.WITHDRAWN
        first.concludedAt != null && first.endOfServiceReportId != null -> SASReferralStatus.COMPLETED
        else -> SASReferralStatus.LIVE
      }

      val sentBy = if (first.sentByUserId != null) {
        AuthUserDTO(
          username = first.sentByUserName ?: "",
          authSource = first.sentByAuthSource ?: "",
          userId = first.sentByUserId,
        )
      } else {
        null
      }

      SASReferralDTO(
        status = status,
        sentAt = first.sentAt?.toOffsetDateTime(),
        sentBy = sentBy,
        referral = SASReferralDetailsDTO(
          createdAt = first.createdAt.toOffsetDateTime(),
          serviceProviders = serviceProviders.values.toList(),
        ),
      )
    }
  }

  private fun Instant.toOffsetDateTime(): OffsetDateTime = OffsetDateTime.ofInstant(this, ZoneId.systemDefault())
}
