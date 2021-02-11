package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util

import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager

class InterventionFactory(em: TestEntityManager): EntityFactory(em) {
}
