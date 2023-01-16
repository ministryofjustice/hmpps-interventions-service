package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jobs.oneoff.concludereferrals

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.ReferralEventPublisher
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Attended
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.ReferralConcluder
import java.time.OffsetDateTime

class ConcludeReferralsProcessorTest : IntegrationTestBase() {
  private val referralEventPublisher = mock<ReferralEventPublisher>()
  private lateinit var referralConcluder: ReferralConcluder

  private lateinit var processor: ConcludeReferralsProcessor

  @BeforeEach
  fun beforeEach() {
    referralConcluder = ReferralConcluder(
      referralRepository,
      deliverySessionRepository,
      actionPlanRepository,
      referralEventPublisher
    )
    processor = ConcludeReferralsProcessor(referralConcluder, deliverySessionRepository)
  }

  @Test
  fun `concludes referrals that have a cancellation request and have no attended delivery sessions`() {
    val endedReferral = setupAssistant.createEndedReferral()
    setupAssistant.createDeliverySession(1, 120, OffsetDateTime.parse("2021-05-13T13:30:00+01:00"), Attended.NO, referral = endedReferral, appointmentFeedbackSubmittedAt = OffsetDateTime.parse("2021-05-13T13:30:00+01:00"))

    val result = processor.process(endedReferral)
    assertThat(result.id).isEqualTo(endedReferral.id)
    assertThat(result.concludedAt).isNotNull
  }

  @Test
  fun `skips referrals that have a cancellation request and have some attended delivery sessions`() {
    val endedReferral = setupAssistant.createEndedReferral()
    setupAssistant.createDeliverySession(1, 120, OffsetDateTime.parse("2021-05-13T13:30:00+01:00"), Attended.YES, referral = endedReferral, appointmentFeedbackSubmittedAt = OffsetDateTime.parse("2021-05-13T13:30:00+01:00"))

    val result = processor.process(endedReferral)
    assertThat(result.id).isEqualTo(endedReferral.id)
    assertThat(result.concludedAt).isNull()
  }
}
