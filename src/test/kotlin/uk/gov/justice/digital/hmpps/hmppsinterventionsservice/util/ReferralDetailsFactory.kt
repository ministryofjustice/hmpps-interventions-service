package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util

import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AuthUser
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ReferralDetails
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class ReferralDetailsFactory(em: TestEntityManager? = null) : EntityFactory(em) {
  fun create(
    referralId: UUID,
    createdAt: OffsetDateTime = OffsetDateTime.now(),
    createdBy: AuthUser,
    id: UUID = UUID.randomUUID(),
    supersededById: UUID? = null,
    reasonForChange: String = "initial referral details",
    completionDeadline: LocalDate? = LocalDate.now(),
    maximumNumberOfEnforceableDays: Int? = 10,
    reasonForReferral: String? = "some reason",
    furtherInformation: String? = "further information",
    saved: Boolean = false,
  ): ReferralDetails {
    val referralDetails = ReferralDetails(
      id = id,
      supersededById = supersededById,
      referralId = referralId,
      createdAt = createdAt,
      createdByUserId = createdBy.id,
      reasonForChange = reasonForChange,
      completionDeadline = completionDeadline,
      maximumEnforceableDays = maximumNumberOfEnforceableDays,
      reasonForReferral = reasonForReferral,
      furtherInformation = furtherInformation,
    )
    if (saved) {
      return save(referralDetails)
    }
    return referralDetails
  }
}
