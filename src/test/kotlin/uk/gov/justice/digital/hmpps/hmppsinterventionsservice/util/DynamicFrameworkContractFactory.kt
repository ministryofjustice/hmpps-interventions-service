package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util

import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager

class DynamicFrameworkContractFactory(em: TestEntityManager): EntityFactory(em) {
}
