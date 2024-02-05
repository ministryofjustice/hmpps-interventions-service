package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity

import org.hibernate.annotations.Fetch
import org.hibernate.annotations.FetchMode
import java.time.OffsetDateTime
import java.util.UUID
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import jakarta.validation.constraints.NotNull

@Entity
class DraftOasysRiskInformation(
  @Id
  val referralId: UUID,
  @NotNull val updatedAt: OffsetDateTime,
  @NotNull
  @ManyToOne
  @Fetch(FetchMode.JOIN)
  val updatedBy: AuthUser,
  var riskSummaryWhoIsAtRisk: String?,
  var riskSummaryNatureOfRisk: String?,
  var riskSummaryRiskImminence: String?,
  var riskToSelfSuicide: String?,
  var riskToSelfSelfHarm: String?,
  var riskToSelfHostelSetting: String?,
  var riskToSelfVulnerability: String?,
  var additionalInformation: String?,
)
