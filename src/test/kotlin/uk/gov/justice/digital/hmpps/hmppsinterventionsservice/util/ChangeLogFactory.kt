package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util

import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.ReferralAmendmentDetails
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AuthUser
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Changelog
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.AmendTopic
import java.time.OffsetDateTime
import java.util.UUID

class ChangeLogFactory(em: TestEntityManager? = null) : EntityFactory(em) {

  fun create(
    id: UUID = UUID.randomUUID(),
    topic: AmendTopic = AmendTopic.DESIRED_OUTCOMES,
    referralId: UUID = UUID.randomUUID(),
    oldVal: ReferralAmendmentDetails,
    newVal: ReferralAmendmentDetails,
    reasonForChange: String = "the value needs changing",
    changedAt: OffsetDateTime = OffsetDateTime.now(),
    changedBy: AuthUser = AuthUserFactory().create(),
  ): Changelog {
    return save(
      Changelog(
        id = id,
        topic = topic,
        referralId = referralId,
        oldVal = oldVal,
        newVal = newVal,
        reasonForChange = reasonForChange,
        changedAt = changedAt,
        changedBy = changedBy,
      ),
    )
  }
}
