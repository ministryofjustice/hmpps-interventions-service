package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.DraftReferral
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Referral
import java.time.OffsetDateTime

@Schema(description = "Status of a referral as used by SAS")
enum class SASReferralStatus {
  DRAFT,
  LIVE,
  COMPLETED,
  WITHDRAWN,
}

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Summary of a referral returned for SAS RM users")
data class SASReferralDTO(
  @Schema(description = "The current status of the referral. Values: DRAFT (not yet sent), LIVE (sent but not concluded), COMPLETED (concluded with end-of-service report), WITHDRAWN (cancelled by probation)")
  val status: SASReferralStatus,

  @Schema(description = "The date and time the referral was sent to a service provider", example = "2026-04-30T12:43:15.693Z")
  val sentAt: OffsetDateTime?,

  @Schema(description = "The user who sent the referral to the service provider")
  val sentBy: AuthUserDTO?,

  @Schema(description = "Core details of the accomodation referral")
  val referral: SASReferralDetailsDTO,
) {
  companion object {
    fun from(referral: Referral, status: SASReferralStatus): SASReferralDTO {
      val contract = referral.intervention.dynamicFrameworkContract
      val serviceProviders = mutableListOf(ServiceProviderDTO.from(contract.primeProvider))
      serviceProviders.addAll(contract.subcontractorProviders.map { ServiceProviderDTO.from(it) })

      return SASReferralDTO(
        status = status,
        sentAt = referral.sentAt,
        sentBy = referral.sentBy?.let { AuthUserDTO.from(it) },
        referral = SASReferralDetailsDTO(
          createdAt = referral.createdAt,
          serviceProviders = serviceProviders,
        ),
      )
    }

    fun fromDraft(draftReferral: DraftReferral): SASReferralDTO {
      val contract = draftReferral.intervention.dynamicFrameworkContract
      val serviceProviders = mutableListOf(ServiceProviderDTO.from(contract.primeProvider))
      serviceProviders.addAll(contract.subcontractorProviders.map { ServiceProviderDTO.from(it) })

      return SASReferralDTO(
        status = SASReferralStatus.DRAFT,
        sentAt = null,
        sentBy = null,
        referral = SASReferralDetailsDTO(
          createdAt = draftReferral.createdAt,
          serviceProviders = serviceProviders,
        ),
      )
    }
  }
}

@Schema(description = "Core referral details for SAS")
data class SASReferralDetailsDTO(
  @Schema(description = "The date and time the referral was created", example = "2026-04-30T12:43:15.694Z")
  val createdAt: OffsetDateTime,

  @Schema(description = "The service providers associated with this referral (prime provider and any subcontractors)")
  val serviceProviders: List<ServiceProviderDTO>,
)
