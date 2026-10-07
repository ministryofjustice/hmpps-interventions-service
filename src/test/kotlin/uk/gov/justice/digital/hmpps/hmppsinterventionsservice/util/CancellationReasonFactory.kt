package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util

import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.CancellationReason

class CancellationReasonFactory(em: TestEntityManager? = null) : EntityFactory(em) {
  fun create(
    id: String = "MIS",
    description: String = "Referral was made by mistake",
  ): CancellationReason = save(
    CancellationReason(
      code = id,
      description = description,
    ),
  )
}
