package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jobs.oneoff.loadendofsentence

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.AdditionalAnswers
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jobs.oneoff.transferreferrals.LoadEndOfSentenceProcessor
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Referral
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ReferralRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.RelevantEndOfSentenceDataloadRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ReferralFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.RelevantEndOfSentenceFactory
import java.time.LocalDate

internal class LoadEndOfSentenceProcessorTest {

  private val referralRepository = mock<ReferralRepository>()
  private val endOfSentenceDataloadRepository = mock<RelevantEndOfSentenceDataloadRepository>()
  private val processor = LoadEndOfSentenceProcessor(
    endOfSentenceDataloadRepository,
    referralRepository,
  )

  private val referralFactory = ReferralFactory()
  private val endOfSentenceDataloadFactory = RelevantEndOfSentenceFactory()

  private val startReferral = referralFactory.createSent(relevantSentenceId = 11111111L)
  private val endOfSentenceDataload = endOfSentenceDataloadFactory.create()

  @BeforeEach
  fun setup() {
    whenever(endOfSentenceDataloadRepository.findByRelevantSentenceId(11111111L)).thenReturn(endOfSentenceDataload)
    whenever(endOfSentenceDataloadRepository.findByRelevantSentenceId(123456789L)).thenReturn(null)
    whenever(referralRepository.save(any())).thenAnswer(AdditionalAnswers.returnsFirstArg<Referral>())
  }

  @Test
  fun `referral is updated if it has target sentence id`() {
    val result = processor.process(startReferral)
    assertThat(result.relevantSentenceEndDate).isEqualTo(LocalDate.now())
    verify(referralRepository).save(startReferral)
  }

  @Test
  fun `referral isn't updated if it has another sentence id`() {
    val otherReferral = referralFactory.createSent(relevantSentenceId = 123456789L)
    val result = processor.process(otherReferral)
    assertThat(result.relevantSentenceEndDate).isNull()
    verify(referralRepository, never()).save(startReferral)
  }
}
