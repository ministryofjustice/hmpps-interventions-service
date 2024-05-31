package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util

import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.WithdrawalReason

class WithdrawReasonFactory(em: TestEntityManager? = null) : EntityFactory(em) {
  fun create(
    id: String = "MIS",
    description: String = "Referral was made by mistake",
    grouping: String = "AAA",
  ): WithdrawalReason {
    return save(
      WithdrawalReason(
        code = id,
        description = description,
        grouping = grouping,
      ),
    )
  }
}
