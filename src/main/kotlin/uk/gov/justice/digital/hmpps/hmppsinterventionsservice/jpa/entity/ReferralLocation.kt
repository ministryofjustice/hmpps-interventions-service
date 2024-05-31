package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.OneToOne
import org.hibernate.annotations.JdbcType
import org.hibernate.dialect.PostgreSQLEnumJdbcType
import org.jetbrains.annotations.NotNull
import java.time.LocalDate
import java.util.UUID

@Entity
data class ReferralLocation(
  @Id val id: UUID,
  @OneToOne(fetch = FetchType.LAZY)
  val referral: Referral,

  @Enumerated(EnumType.STRING)
  @Column(name = "type", columnDefinition = "person_current_location_type")
  @JdbcType(PostgreSQLEnumJdbcType::class)
  @NotNull
  val type: PersonCurrentLocationType,

  @Column(name = "prison_id") val prisonId: String?,
  @Column(name = "expected_release_date") val expectedReleaseDate: LocalDate?,
  @Column(name = "expected_release_date_missing_reason") val expectedReleaseDateMissingReason: String?,
  @Column(name = "expected_probation_office") val expectedProbationOffice: String?,
  @Column(name = "expected_probation_office_unknown_reason") val expectedProbationOfficeUnknownReason: String?,
  @Column(name = "nomis_prison_id") var nomisPrisonId: String? = null,
  @Column(name = "nomis_release_date") var nomisReleaseDate: LocalDate? = null,
  @Column(name = "nomis_confirmed_release_date") var nomisConfirmedReleaseDate: LocalDate? = null,
  @Column(name = "nomis_non_dto_release_date") var nomisNonDtoReleaseDate: LocalDate? = null,
  @Column(name = "nomis_automatic_release_date") var nomisAutomaticReleaseDate: LocalDate? = null,
  @Column(name = "nomis_post_recall_release_date") var nomisPostRecallReleaseDate: LocalDate? = null,
  @Column(name = "nomis_conditional_release_date") var nomisConditionalReleaseDate: LocalDate? = null,
  @Column(name = "nomis_actual_parole_date") var nomisActualParoleDate: LocalDate? = null,
  @Column(name = "nomis_discharge_date") var nomisDischargeDate: LocalDate? = null,
  @Column(name = "referral_releasing_12_weeks") var isReferralReleasingIn12Weeks: Boolean? = null,
)
