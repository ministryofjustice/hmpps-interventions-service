package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util

import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager

class ServiceProviderFactory(em: TestEntityManager): EntityFactory(em) {
}
