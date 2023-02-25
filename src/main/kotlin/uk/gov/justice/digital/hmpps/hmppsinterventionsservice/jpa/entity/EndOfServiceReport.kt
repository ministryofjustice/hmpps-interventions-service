package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity

import jakarta.persistence.CollectionTable
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import jakarta.validation.constraints.NotNull
import org.hibernate.annotations.Fetch
import org.hibernate.annotations.FetchMode
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "end_of_service_report")
data class EndOfServiceReport(
  @Id val id: UUID,
  @OneToOne(fetch = FetchType.LAZY) val referral: Referral,

  @NotNull val createdAt: OffsetDateTime,
  @NotNull
  @ManyToOne
  @Fetch(FetchMode.JOIN)
  val createdBy: AuthUser,

  var submittedAt: OffsetDateTime? = null,
  @ManyToOne
  @Fetch(FetchMode.JOIN)
  var submittedBy: AuthUser? = null,

  var furtherInformation: String? = null,

  // Outcomes
  @ElementCollection
  @CollectionTable(name = "end_of_service_report_outcome", joinColumns = [JoinColumn(name = "end_of_service_report_id")])
  @NotNull
  val outcomes: MutableSet<EndOfServiceReportOutcome> = mutableSetOf(),

) {
  val achievementScore: Float
    get() = if (submittedAt != null) {
      outcomes.fold(0.0F) { score, outcome ->
        score + when (outcome.achievementLevel) {
          AchievementLevel.ACHIEVED -> 1F
          AchievementLevel.PARTIALLY_ACHIEVED -> 0.5F
          else -> 0F
        }
      }
    } else {
      0F
    }
}
