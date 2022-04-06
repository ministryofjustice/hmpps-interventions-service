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
    createdAt: OffsetDateTime,
    createdBy: AuthUser,
    id: UUID = UUID.randomUUID(),
    supersededById: UUID? = null,
    reasonForChange: String = "initial referral details",
    completionDeadline: LocalDate? = LocalDate.now(),
    maximumNumberOfEnforceableDays: Int? = 10,
    furtherInformation: String? = "further information",
  ): ReferralDetails {
    return ReferralDetails(
      id = id,
      supersededById = supersededById,
      referralId = referralId,
      createdAt = createdAt,
      createdByUserId = createdBy.id,
      reasonForChange = reasonForChange,
      completionDeadline = completionDeadline,
      maximumEnforceableDays = maximumNumberOfEnforceableDays,
      furtherInformation = furtherInformation,
    )
  }
}
