package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entities.Referral
import java.time.LocalDateTime
import java.util.UUID

class ReferralDto(

  @Schema(description = "Referral primary key", example = "1234")
  val referralId: Long? = null,

  @Schema(description = "Referral primary key", example = "1234")
  val referralUuid: UUID? = null,

  @Schema(description = "Referral to be Completed by Date", example = "2020-01-02T16:00:00")
  val completeByDate: LocalDateTime? = null,

  @Schema(description = "Created Date", example = "2020-01-02T16:00:00")
  val createdDate: LocalDateTime? = null

) {

  companion object {

    fun from(referral: Referral): ReferralDto {
      return ReferralDto(
        referral.referralId,
        referral.referralUuid,
        referral.completeByDate,
        referral.createdDate
      )
    }
  }
}
