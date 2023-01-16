package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jobs.oneoff.concludereferrals

import org.assertj.core.api.Assertions.assertThat
import org.hibernate.SessionFactory
import org.junit.jupiter.api.Test
import org.springframework.batch.item.ExecutionContext
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.integration.IntegrationTestBase

internal class ConcludeReferralsReaderTest @Autowired constructor(
  sessionFactory: SessionFactory,
) : IntegrationTestBase() {
  private val reader = ConcludeReferralsReader(sessionFactory)

  @Test
  fun `finds referrals that have a cancellation request`() {
    val endedReferral = setupAssistant.createEndedReferral()
//    setupAssistant.createDeliverySession(1, 120, OffsetDateTime.parse("2021-05-13T13:30:00+01:00"), referral = endedReferral)

    reader.open(ExecutionContext())
    assertThat(reader.read()?.id).isEqualTo(endedReferral.id)
  }

  @Test
  fun `skips referrals that have attended sessions and have a cancellation request`() {
    setupAssistant.createCancelledReferral()

    reader.open(ExecutionContext())
    assertThat(reader.read()).isNull()
  }
}
