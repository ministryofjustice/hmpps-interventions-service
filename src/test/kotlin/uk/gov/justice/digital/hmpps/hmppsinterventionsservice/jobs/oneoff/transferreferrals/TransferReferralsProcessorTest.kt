package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jobs.oneoff.transferreferrals

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.TemporalUnitWithinOffset
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.AdditionalAnswers
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AuthUser
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.CaseNote
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Referral
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.CaseNoteRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.InterventionRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ReferralRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.InterventionFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ReferralFactory
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit

internal class TransferReferralsProcessorTest {
  private val interventionRepository = mock<InterventionRepository>()
  private val referralRepository = mock<ReferralRepository>()
  private val caseNoteRepository = mock<CaseNoteRepository>()

  private val originalContractCode = "FROM"
  private val targetContractCode = "TO"

  private val processor = TransferReferralsProcessor(
    interventionRepository,
    referralRepository,
    caseNoteRepository,
    originalContractCode,
    targetContractCode,
  )

  private val referralFactory = ReferralFactory()
  private val interventionFactory = InterventionFactory()

  private val fromIntervention = interventionFactory.createWithContractCode(originalContractCode, title = "Source Intervention")
  private val toIntervention = interventionFactory.createWithContractCode(targetContractCode, title = "Target Intervention")

  @BeforeEach
  fun setup() {
    whenever(interventionRepository.findByDynamicFrameworkContractContractReference(originalContractCode)).thenReturn(fromIntervention)
    whenever(interventionRepository.findByDynamicFrameworkContractContractReference(targetContractCode)).thenReturn(toIntervention)
    whenever(referralRepository.save(any())).thenAnswer(AdditionalAnswers.returnsFirstArg<Referral>())
    whenever(caseNoteRepository.save(any())).thenAnswer(AdditionalAnswers.returnsFirstArg<CaseNote>())
  }

  @Test
  fun `referral is transferred to the configure target contract`() {
    val referral = referralFactory.createSent(intervention = fromIntervention)

    val result = processor.process(referral)
    assertThat(result.intervention).isEqualTo(toIntervention)
    assertThat(referral.intervention).isEqualTo(toIntervention)
    verify(referralRepository).save(referral)
  }

  @Test
  fun `referral gets a new case note auditing the transfer`() {
    val referral = referralFactory.createSent(intervention = fromIntervention)

    processor.process(referral)

    val caseNoteCaptor = argumentCaptor<CaseNote>()
    verify(caseNoteRepository).save(caseNoteCaptor.capture())
    assertThat(caseNoteCaptor.firstValue.subject).isEqualTo("Case transferred")
    assertThat(caseNoteCaptor.firstValue.body).isEqualTo("Automated transfer from Source Intervention to Target Intervention")
    assertThat(caseNoteCaptor.firstValue.sentBy).isEqualTo(AuthUser.interventionsServiceUser)
    assertThat(caseNoteCaptor.firstValue.sentAt).isCloseTo(
      OffsetDateTime.now(),
      TemporalUnitWithinOffset(1, ChronoUnit.MINUTES),
    )
  }
}
