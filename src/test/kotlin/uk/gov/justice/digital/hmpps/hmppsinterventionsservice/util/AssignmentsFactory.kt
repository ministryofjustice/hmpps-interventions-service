package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util

import org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AuthUser
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ReferralAssignment
import java.time.OffsetDateTime

class AssignmentsFactory(em: TestEntityManager? = null) : EntityFactory(em) {
  private val authUserFactory = AuthUserFactory(em)

  fun create(numberOfAssignments: Int): List<ReferralAssignment> {
    val assignedUsers = mutableListOf<AuthUser>().apply {
      repeat(numberOfAssignments) {
        this.add(authUserFactory.create(randomAlphanumeric(6), randomAlphanumeric(5), randomAlphanumeric(12)))
      }
    }
    return assignedUsers.mapIndexed { i, u ->
      val latest = numberOfAssignments - 1
      ReferralAssignment(
        OffsetDateTime.now().minusHours(latest.toLong()).plusHours(i.toLong()),
        assignedBy = u,
        assignedTo = u,
        superseded = i != latest
      )
    }
  }
}
