package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util

import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.test.context.ActiveProfiles

// run JPA tests against the real database to avoid missing bugs arising from SQL syntax
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("local")
annotation class RepositoryTest
