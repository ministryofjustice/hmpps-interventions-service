package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.config.TestDataSourceConfig

// run JPA tests against the real database to avoid missing bugs arising from SQL syntax
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("local")
@Import(TestEntityManagerConfiguration::class, TestDataSourceConfig::class)
annotation class RepositoryTest
