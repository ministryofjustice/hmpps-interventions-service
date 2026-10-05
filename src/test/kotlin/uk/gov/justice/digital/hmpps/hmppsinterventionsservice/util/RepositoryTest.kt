package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util

import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource

// run JPA tests against the real database to avoid missing bugs arising from SQL syntax
@SpringBootTest
@ActiveProfiles("local")
@TestPropertySource(locations = ["classpath:application-test.properties"])
@Import(TestEntityManagerConfiguration::class)
annotation class RepositoryTest
