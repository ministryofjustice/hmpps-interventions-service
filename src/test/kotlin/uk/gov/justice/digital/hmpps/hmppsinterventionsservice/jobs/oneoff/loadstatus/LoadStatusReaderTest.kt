package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jobs.oneoff.loadstatus

import jakarta.persistence.EntityManagerFactory
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.batch.item.ExecutionContext
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Referral
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Status

internal class LoadStatusReaderTest
@Autowired constructor(
  entityManagerFactory: EntityManagerFactory,
) : IntegrationTestBase() {
  private val reader = LoadStatusReader(entityManagerFactory)

  fun createReferralWithStatus(status: Status?): Referral {
    return setupAssistant.createSentReferral(status = status)
  }

  @Test
  fun `skips referral that has pre-ICA status`() {
    val targetReferral = createReferralWithStatus(Status.PRE_ICA)
    reader.open(ExecutionContext())
    val result = reader.read()
    assertThat(result).isNull()
  }

  @Test
  fun `skips referral that has a null status`() {
    val targetReferral = createReferralWithStatus(null)
    reader.open(ExecutionContext())
    assertThat(reader.read()?.id).isNull()
  }

  @Test
  fun `finds referral that has a post-ICA status`() {
    val targetReferral = createReferralWithStatus(Status.POST_ICA)
    reader.open(ExecutionContext())
    assertThat(reader.read()?.id).isEqualTo(targetReferral.id)
  }
}
