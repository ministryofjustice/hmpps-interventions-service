package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jobs.oneoff.loadstatus

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.AdditionalAnswers
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Referral
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Status
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.DeliverySessionRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ReferralRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ReferralFactory

internal class LoadStatusProcessorTest {

  private val referralRepository = mock<ReferralRepository>()
  private val deliverySessionRepository = mock<DeliverySessionRepository>()
  private val processor = LoadStatusProcessor(
    deliverySessionRepository,
    referralRepository,
  )

  private val referralFactory = ReferralFactory()

  private val referral = referralFactory.createSent(status = null)

  @BeforeEach
  fun setup() {
    whenever(referralRepository.save(any())).thenAnswer(AdditionalAnswers.returnsFirstArg<Referral>())
  }

  @Test
  fun `referral status is set to PRE_ICA if first substantive appointment not delivered`() {
    whenever(deliverySessionRepository.countNumberOfAttendedSessions(referral.id)).thenReturn(0)
    val result = processor.process(referral)
    assertThat(result.status).isEqualTo(Status.PRE_ICA)
    verify(referralRepository).save(referral)
  }

  @Test
  fun `referral status is set to POST_ICA if first substantive appointment is delivered`() {
    whenever(deliverySessionRepository.countNumberOfAttendedSessions(referral.id)).thenReturn(1)
    val result = processor.process(referral)
    assertThat(result.status).isEqualTo(Status.POST_ICA)
    verify(referralRepository).save(referral)
  }
}
