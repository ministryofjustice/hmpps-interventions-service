package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util

import jakarta.persistence.EntityManager
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class TestEntityManagerConfiguration {
  @Bean
  fun testEntityManager(entityManager: EntityManager) = TestEntityManager(entityManager)
}
