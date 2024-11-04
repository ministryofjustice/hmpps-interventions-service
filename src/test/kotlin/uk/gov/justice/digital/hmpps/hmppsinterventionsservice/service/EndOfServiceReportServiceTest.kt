package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service

import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.AdditionalAnswers
import org.mockito.ArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.firstValue
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.EndOfServiceReportEventPublisher
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AchievementLevel
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AuthUser
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.EndOfServiceReport
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.SampleData
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.AuthUserRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.EndOfServiceReportRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ReferralRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.EndOfServiceReportFactory
import java.util.Optional.empty
import java.util.Optional.of
import java.util.UUID

class EndOfServiceReportServiceTest {
  private val authUserRepository: AuthUserRepository = mock()
  private val referralRepository: ReferralRepository = mock()
  private val endOfServiceReportRepository: EndOfServiceReportRepository = mock()
  private val endOfServiceReportEventPublisher: EndOfServiceReportEventPublisher = mock()
  private val referralConcluder: ReferralConcluder = mock()

  private val endOfServiceReportFactory = EndOfServiceReportFactory()

  private val endOfServiceReportService = EndOfServiceReportService(
    authUserRepository,
    referralRepository,
    endOfServiceReportRepository,
    endOfServiceReportEventPublisher,
    referralConcluder,
  )

  @Test
  fun `create end of service report`() {
    val authUser = AuthUser("CRN123", "auth", "user")
    val referral = SampleData.sampleReferral(crn = "CRN123", serviceProviderName = "Service Provider")
    val endOfServiceReport = SampleData.sampleEndOfServiceReport(outcomes = mutableSetOf(), referral = referral)

    whenever(authUserRepository.save(authUser)).thenReturn(authUser)
    whenever(referralRepository.getReferenceById(referral.id)).thenReturn(referral)
    whenever(endOfServiceReportRepository.save(any())).thenReturn(endOfServiceReport)
    whenever(endOfServiceReportRepository.findByReferralId(referralId = referral.id)).thenReturn(null)
    whenever(referralRepository.save(any())).thenReturn(referral)

    val savedEndOfServiceReport = endOfServiceReportService.createEndOfServiceReport(referral.id, authUser)

    assertThat(savedEndOfServiceReport).isNotNull
  }

  @Test
  fun `return existing end of service report`() {
    val authUser = AuthUser("CRN123", "auth", "user")
    val referral = SampleData.sampleReferral(crn = "CRN123", serviceProviderName = "Service Provider")
    val endOfServiceReport = SampleData.sampleEndOfServiceReport(outcomes = mutableSetOf(), referral = referral)

    whenever(authUserRepository.save(authUser)).thenReturn(authUser)
    whenever(referralRepository.getReferenceById(referral.id)).thenReturn(referral)
    whenever(endOfServiceReportRepository.findByReferralId(referralId = referral.id)).thenReturn(endOfServiceReport)
    whenever(referralRepository.save(any())).thenReturn(referral)

    val savedEndOfServiceReport = endOfServiceReportService.createEndOfServiceReport(referral.id, authUser)

    verify(endOfServiceReportRepository, never()).save(any())
    assertThat(savedEndOfServiceReport).isNotNull
  }

  @Test
  fun `referral not found when creating end of service report`() {
    val authUser = AuthUser("CRN123", "auth", "user")
    val referralId = UUID.randomUUID()
    whenever(referralRepository.getReferenceById(referralId)).thenThrow(EntityNotFoundException::class.java)

    Assertions.assertThrows(EntityNotFoundException::class.java) {
      endOfServiceReportService.createEndOfServiceReport(referralId, authUser)
    }
  }

  @Test
  fun `get end of service report`() {
    val referral = SampleData.sampleReferral(crn = "CRN123", serviceProviderName = "Service Provider")
    val endOfServiceReport = SampleData.sampleEndOfServiceReport(outcomes = mutableSetOf(), referral = referral)
    whenever(endOfServiceReportRepository.findById(any())).thenReturn(of(endOfServiceReport))
    val retrievedEndOfServiceReport = endOfServiceReportService.getEndOfServiceReport(UUID.randomUUID())
    assertThat(retrievedEndOfServiceReport).isNotNull
  }

  @Test
  fun `get end of service report not found`() {
    val endOfServiceReportId = UUID.randomUUID()
    whenever(endOfServiceReportRepository.findById(any())).thenReturn(empty())

    val exception = Assertions.assertThrows(EntityNotFoundException::class.java) {
      endOfServiceReportService.getEndOfServiceReport(endOfServiceReportId)
    }
    assertThat(exception.message).isEqualTo("End of service report not found [id=$endOfServiceReportId]")
  }

  @Test
  fun `get end of service report by referral Id`() {
    val referral = SampleData.sampleReferral(crn = "CRN123", serviceProviderName = "Service Provider")
    val endOfServiceReport = SampleData.sampleEndOfServiceReport(outcomes = mutableSetOf(), referral = referral)
    referral.endOfServiceReport = endOfServiceReport

    whenever(referralRepository.findById(referral.id)).thenReturn(of(referral))
    val retrievedEndOfServiceReport = endOfServiceReportService.getEndOfServiceReportByReferralId(referral.id)
    assertThat(retrievedEndOfServiceReport).isEqualTo(endOfServiceReport)
  }

  @Test
  fun `referral not found for get end of service report by referral id`() {
    val referral = SampleData.sampleReferral(crn = "CRN123", serviceProviderName = "Service Provider")
    val endOfServiceReport = SampleData.sampleEndOfServiceReport(outcomes = mutableSetOf(), referral = referral)
    referral.endOfServiceReport = endOfServiceReport

    whenever(referralRepository.findById(referral.id)).thenReturn(empty())

    val exception = Assertions.assertThrows(EntityNotFoundException::class.java) {
      endOfServiceReportService.getEndOfServiceReportByReferralId(referral.id)
    }
    assertThat(exception.message).isEqualTo("Referral not found [id=${referral.id}]")
  }

  @Test
  fun `referral has no end of service report for get end of service report by referral id`() {
    val referral = SampleData.sampleReferral(crn = "CRN123", serviceProviderName = "Service Provider")

    whenever(referralRepository.findById(referral.id)).thenReturn(of(referral))

    val exception = Assertions.assertThrows(EntityNotFoundException::class.java) {
      endOfServiceReportService.getEndOfServiceReportByReferralId(referral.id)
    }
    assertThat(exception.message).isEqualTo("End of service report not found for referral [id=${referral.id}]")
  }

  @Test
  fun `successfully update end of service report`() {
    val referral = SampleData.sampleReferral(crn = "CRN123", serviceProviderName = "Service Provider")
    val endOfServiceReport = SampleData.sampleEndOfServiceReport(outcomes = mutableSetOf(), referral = referral)
    val endOfServiceReportId = UUID.randomUUID()
    val furtherInformation = "info"
    val outcome = SampleData.sampleEndOfServiceReportOutcome()

    whenever(endOfServiceReportRepository.findById(any())).thenReturn(of(endOfServiceReport))
    whenever(endOfServiceReportRepository.save(any())).thenReturn(endOfServiceReport)

    val savedEndOfServiceReport =
      endOfServiceReportService.updateEndOfServiceReport(endOfServiceReportId, furtherInformation, outcome)
    val argumentCaptor: ArgumentCaptor<EndOfServiceReport> = ArgumentCaptor.forClass(EndOfServiceReport::class.java)
    verify(endOfServiceReportRepository).save(argumentCaptor.capture())
    assertThat(argumentCaptor.firstValue.furtherInformation).isEqualTo(furtherInformation)
    assertThat(argumentCaptor.firstValue.outcomes.elementAt(0)).isEqualTo(outcome)
    assertThat(savedEndOfServiceReport).isNotNull
  }

  @Test
  fun `successfully update end of service report where desired outcome already exists`() {
    val referral = SampleData.sampleReferral(crn = "CRN123", serviceProviderName = "Service Provider")
    val endOfServiceReport = SampleData.sampleEndOfServiceReport(referral = referral)
    val desiredOutcome = endOfServiceReport.outcomes.elementAt(0).desiredOutcome
    val endOfServiceReportId = UUID.randomUUID()
    val furtherInformation = "info"
    val outcome = SampleData.sampleEndOfServiceReportOutcome(
      desiredOutcome = desiredOutcome,
      achievementLevel = AchievementLevel.PARTIALLY_ACHIEVED,
    )

    whenever(endOfServiceReportRepository.findById(any())).thenReturn(of(endOfServiceReport))
    whenever(endOfServiceReportRepository.save(any())).thenReturn(endOfServiceReport)

    val savedEndOfServiceReport =
      endOfServiceReportService.updateEndOfServiceReport(endOfServiceReportId, furtherInformation, outcome)
    val argumentCaptor: ArgumentCaptor<EndOfServiceReport> = ArgumentCaptor.forClass(EndOfServiceReport::class.java)

    verify(endOfServiceReportRepository).save(argumentCaptor.capture())
    assertThat(argumentCaptor.firstValue.furtherInformation).isEqualTo(furtherInformation)
    assertThat(argumentCaptor.firstValue.outcomes.elementAt(0).achievementLevel).isEqualTo(AchievementLevel.PARTIALLY_ACHIEVED)
    assertThat(savedEndOfServiceReport.outcomes.size).isEqualTo(1)
    assertThat(savedEndOfServiceReport).isNotNull
  }

  @Test
  fun `submit an end of service report`() {
    val endOfServiceReportId = UUID.randomUUID()
    val authUser = AuthUser("CRN123", "auth", "user")

    val endOfServiceReport = endOfServiceReportFactory.create(id = endOfServiceReportId)

    whenever(endOfServiceReportRepository.findById(any())).thenReturn(of(endOfServiceReport))
    whenever(endOfServiceReportRepository.save(any())).thenAnswer(AdditionalAnswers.returnsFirstArg<EndOfServiceReport>())
    whenever(authUserRepository.save(any())).thenAnswer(AdditionalAnswers.returnsFirstArg<AuthUser>())

    val argumentCaptor: ArgumentCaptor<EndOfServiceReport> = ArgumentCaptor.forClass(EndOfServiceReport::class.java)
    endOfServiceReportService.submitEndOfServiceReport(endOfServiceReportId, authUser)

    verify(endOfServiceReportRepository).save(argumentCaptor.capture())
    verify(referralConcluder).concludeIfEligible(endOfServiceReport.referral)
    assertThat(argumentCaptor.firstValue.furtherInformation).isEqualTo(endOfServiceReport.furtherInformation)
    assertThat(argumentCaptor.firstValue.submittedAt).isNotNull
    assertThat(argumentCaptor.firstValue.submittedBy).isEqualTo(authUser)
  }

  @Test
  fun `submit an end of service report copes with error without notifying nDelius`() {
    val endOfServiceReportId = UUID.randomUUID()
    val authUser = AuthUser("CRN123", "auth", "user")

    val endOfServiceReport = endOfServiceReportFactory.create(id = endOfServiceReportId)

    whenever(endOfServiceReportRepository.findById(any())).thenReturn(of(endOfServiceReport))
    whenever(endOfServiceReportRepository.save(any())).thenThrow(RuntimeException("error saving"))
    whenever(authUserRepository.save(any())).thenAnswer(AdditionalAnswers.returnsFirstArg<AuthUser>())

    val argumentCaptor: ArgumentCaptor<EndOfServiceReport> = ArgumentCaptor.forClass(EndOfServiceReport::class.java)

    try {
      endOfServiceReportService.submitEndOfServiceReport(endOfServiceReportId, authUser)
    } catch (e: Exception) {
    }

    verify(endOfServiceReportRepository).save(argumentCaptor.capture())
    verify(referralConcluder, never()).concludeIfEligible(endOfServiceReport.referral)
  }
}
