package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.reporting.ndmis.performance

import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AchievementLevel
import java.util.UUID

data class OutcomeData(
  val referralReference: String,
  val referralId: UUID,
  val desiredOutcomeDescription: String,
  val achievementLevel: AchievementLevel,
) {
  companion object {
    val fields = listOf(
      "referralReference",
      "referralId",
      "desiredOutcomeDescription",
      "achievementLevel",
    )
    val headers = listOf(
      "referral_ref",
      "referral_id",
      "desired_outcome",
      "achievement_level",
    )
  }
}
