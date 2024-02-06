package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity

import jakarta.persistence.Embeddable
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.ManyToOne
import jakarta.validation.constraints.NotNull
import org.hibernate.annotations.Fetch
import org.hibernate.annotations.FetchMode

@Embeddable
data class EndOfServiceReportOutcome(

  @NotNull
  @ManyToOne
  @Fetch(FetchMode.JOIN)
  val desiredOutcome: DesiredOutcome,
  @Enumerated(EnumType.STRING)
  @NotNull
  val achievementLevel: AchievementLevel,
  val progressionComments: String? = null,
  val additionalTaskComments: String? = null,
)

enum class AchievementLevel {
  ACHIEVED,
  PARTIALLY_ACHIEVED,
  NOT_ACHIEVED,
}
