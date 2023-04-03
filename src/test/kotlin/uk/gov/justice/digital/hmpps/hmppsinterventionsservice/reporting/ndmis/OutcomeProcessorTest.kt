package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.reporting.ndmis

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AchievementLevel
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.DesiredOutcome
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.EndOfServiceReportOutcome
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.reporting.ndmis.performance.OutcomeProcessor
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.EndOfServiceReportFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ReferralFactory
import java.util.UUID

internal class OutcomeProcessorTest {
  private val processor = OutcomeProcessor()
  private val referralFactory = ReferralFactory()
  private val endOfServiceReportFactory = EndOfServiceReportFactory()

  @Test
  fun `referral with single selected desired outcome correctly returns a outcomeData object`() {
    val desiredOutcome = DesiredOutcome(UUID.randomUUID(), "Outcome 1", UUID.randomUUID())
    val endOfServiceReport = endOfServiceReportFactory.create(
      outcomes = mutableSetOf(
        EndOfServiceReportOutcome(desiredOutcome, AchievementLevel.ACHIEVED),
      ),
    )

    val referral = referralFactory.createEnded(
      endOfServiceReport = endOfServiceReport,
    )
    val result = processor.process(referral)!!

    assertThat(result.size).isEqualTo(1)
    assertThat(result[0].referralReference).isEqualTo("JS18726AC")
    assertThat(result[0].referralId).isEqualTo(referral.id)
    assertThat(result[0].desiredOutcomeDescription).isEqualTo("Outcome 1")
    assertThat(result[0].achievementLevel).isEqualTo(AchievementLevel.ACHIEVED)
  }

  @Test
  fun `referral with multiple selected service category correctly returns a outcomeData object`() {
    val desiredOutcome1 = DesiredOutcome(UUID.randomUUID(), "Outcome 1", UUID.randomUUID())
    val desiredOutcome2 = DesiredOutcome(UUID.randomUUID(), "Outcome 2", UUID.randomUUID())
    val endOfServiceReport = endOfServiceReportFactory.create(
      outcomes = mutableSetOf(
        EndOfServiceReportOutcome(desiredOutcome1, AchievementLevel.ACHIEVED),
        EndOfServiceReportOutcome(desiredOutcome2, AchievementLevel.NOT_ACHIEVED),
      ),
    )

    val referral = referralFactory.createEnded(
      endOfServiceReport = endOfServiceReport,
    )
    val result = processor.process(referral)!!

    assertThat(result.size).isEqualTo(2)
    assertThat(result[0].referralReference).isEqualTo("JS18726AC")
    assertThat(result[0].referralId).isEqualTo(referral.id)
    assertThat(result[0].desiredOutcomeDescription).isEqualTo("Outcome 1")
    assertThat(result[0].achievementLevel).isEqualTo(AchievementLevel.ACHIEVED)

    assertThat(result[1].referralReference).isEqualTo("JS18726AC")
    assertThat(result[1].referralId).isEqualTo(referral.id)
    assertThat(result[1].desiredOutcomeDescription).isEqualTo("Outcome 2")
    assertThat(result[1].achievementLevel).isEqualTo(AchievementLevel.NOT_ACHIEVED)
  }

  @Test
  fun `referral with no end of service report returns no outcomeData object`() {
    val referral = referralFactory.createEnded()
    val result = processor.process(referral)
    assertThat(result).isNull()
  }
}
