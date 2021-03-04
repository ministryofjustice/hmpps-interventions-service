package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.integration.health

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.health.SchemaInfo
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.integration.IntegrationTestBase

class SchemaInfoTest @Autowired constructor(
  private val schemaInfo: SchemaInfo
) : IntegrationTestBase() {
  @Test
  fun `schema HTML page is generated which contains database schema details`() {
    val page = schemaInfo.schemaDocument()
    assertThat(page).contains("dynamic_framework_contract")
    assertThat(page).contains("allows_female")
  }
}
