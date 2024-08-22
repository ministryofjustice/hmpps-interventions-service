package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jobs.oneoff.loadstatus

import org.assertj.core.api.Assertions.assertThat
import org.hibernate.SessionFactory
import org.junit.jupiter.api.Test
import org.springframework.batch.item.ExecutionContext
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Referral
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Status

internal class LoadStatusReaderTest
@Autowired constructor(
  sessionFactory: SessionFactory,
) : IntegrationTestBase() {
  private val reader = LoadStatusReader(sessionFactory)

  fun createPopulatedReferral(): Referral {
    return setupAssistant.createSentReferral(status = Status.PRE_ICA)
  }

  fun createUnpopulatedReferral(): Referral {
    return setupAssistant.createSentReferral(status = null)
  }

  @Test
  fun `skips referral that has a status`() {
    val targetReferral = createPopulatedReferral()
    reader.open(ExecutionContext())
    val result = reader.read()
    assertThat(result).isNull()
  }

  @Test
  fun `finds referral that has a null status`() {
    val targetReferral = createUnpopulatedReferral()
    reader.open(ExecutionContext())
    assertThat(reader.read()?.id).isEqualTo(targetReferral.id)
  }
}