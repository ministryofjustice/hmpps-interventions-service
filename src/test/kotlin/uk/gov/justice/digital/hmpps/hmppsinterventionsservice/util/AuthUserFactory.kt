package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util

import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AuthUser

class AuthUserFactory(em: TestEntityManager): EntityFactory(em) {
  fun create(id: String = "123456", authSource: String = "delius"): AuthUser {
    return save(AuthUser(id, authSource))
  }
}
