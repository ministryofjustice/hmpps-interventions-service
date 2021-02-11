package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util

import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AuthUser
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Referral
import java.time.OffsetDateTime
import java.util.UUID

class ReferralFactory(em: TestEntityManager): EntityFactory(em) {
//  private val authUserFactory = AuthUserFactory(em)
//  fun referral(id: UUID? = null, createdAt: OffsetDateTime? = null, createdBy: AuthUser? = null): Referral {
//    return save(Referral(
//      id = id ?: UUID.randomUUID(),
//      createdAt = createdAt ?: OffsetDateTime.now(),
//      createdBy = createdBy ?: authUserFactory.authUser(),
////      intervention = null,
//    ))
//  }
}
