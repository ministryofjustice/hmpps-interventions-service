package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity

import com.vladmihalcea.hibernate.type.basic.PostgreSQLEnumType
import org.hibernate.annotations.Fetch
import org.hibernate.annotations.FetchMode.JOIN
import org.hibernate.annotations.Type
import org.hibernate.annotations.TypeDef
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID
import javax.persistence.CascadeType
import javax.persistence.CollectionTable
import javax.persistence.Column
import javax.persistence.ElementCollection
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.FetchType
import javax.persistence.Id
import javax.persistence.Index
import javax.persistence.JoinColumn
import javax.persistence.JoinTable
import javax.persistence.ManyToMany
import javax.persistence.ManyToOne
import javax.persistence.OneToMany
import javax.persistence.OneToOne
import javax.persistence.PrimaryKeyJoinColumn
import javax.persistence.Table
import javax.validation.constraints.NotNull

enum class PersonCurrentLocationType {
  CUSTODY, COMMUNITY
}

@Entity
@TypeDef(name = "person_current_location_type", typeClass = PostgreSQLEnumType::class)
@Table(name = "draft_referral", indexes = [Index(columnList = "created_by_id")])
class DraftReferral(
  // draft referral fields
  @OneToOne(mappedBy = "draftReferral", cascade = [CascadeType.ALL]) @PrimaryKeyJoinColumn var serviceUserData: ServiceUserData? = null,
  @Column(name = "draft_supplementary_risk") var additionalRiskInformation: String? = null,
  @Column(name = "draft_supplementary_risk_updated_at") var additionalRiskInformationUpdatedAt: OffsetDateTime? = null,
  var additionalNeedsInformation: String? = null,
  var accessibilityNeeds: String? = null,
  var needsInterpreter: Boolean? = null,
  var interpreterLanguage: String? = null,
  var hasAdditionalResponsibilities: Boolean? = null,
  var whenUnavailable: String? = null,

  var relevantSentenceId: Long? = null,

  @Type(type = "person_current_location_type")
  @Enumerated(EnumType.STRING)
  var personCurrentLocationType: PersonCurrentLocationType? = null,
  var personCustodyPrisonId: String? = null,

  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(
    name = "referral_selected_service_category",
    joinColumns = [JoinColumn(name = "referral_id")],
    inverseJoinColumns = [JoinColumn(name = "service_category_id")],
  )
  var selectedServiceCategories: MutableSet<ServiceCategory>? = null,

  @ElementCollection
  @CollectionTable(name = "referral_desired_outcome", joinColumns = [JoinColumn(name = "referral_id")])
  var selectedDesiredOutcomes: MutableList<SelectedDesiredOutcomesMapping>? = null,

  @ElementCollection
  @CollectionTable(name = "referral_complexity_level_ids", joinColumns = [JoinColumn(name = "referral_id")])
  var complexityLevelIds: MutableMap<UUID, UUID>? = null,

  @Column(name = "person_expected_release_date") var expectedReleaseDate: LocalDate? = null,
  @Column(name = "person_expected_release_date_missing_reason") var expectedReleaseDateMissingReason: String? = null,

  // required fields
  @NotNull
  @ManyToOne(fetch = FetchType.LAZY)
  val intervention: Intervention,
  @NotNull val serviceUserCRN: String,
  @NotNull
  @ManyToOne
  @Fetch(JOIN)
  val createdBy: AuthUser,
  @NotNull val createdAt: OffsetDateTime,
  @Id val id: UUID,

  @OneToMany(fetch = FetchType.LAZY)
  @JoinColumn(name = "referral_id")
  val referralDetailsHistory: Set<ReferralDetails>? = null,

  @Column(name = "ndelius_pp_name") var nDeliusPPName: String? = null,
  @Column(name = "ndelius_pp_email_address") var nDeliusPPEmailAddress: String? = null,
  @Column(name = "ndelius_pp_pdu") var nDeliusPPPDU: String? = null,
  @Column(name = "pp_name") var ppName: String? = null,
  @Column(name = "pp_email_address") var ppEmailAddress: String? = null,
  @Column(name = "pp_pdu") var ppPdu: String? = null,
  @Column(name = "pp_probation_office") var ppProbationOffice: String? = null,
  @Column(name = "valid_delius_pp_details") var hasValidDeliusPPDetails: Boolean? = null,
  @Column(name = "role_job_title") var roleOrJobTitle: String? = null,
  @Column(name = "referral_releasing_12_weeks") var isReferralReleasingIn12Weeks: Boolean? = null,
  @Column(name = "has_main_point_of_contact_details") var hasMainPointOfContactDetails: Boolean? = null,
  @Column(name = "pp_establishment") var ppEstablishment: String? = null,
) {
  val referralDetails: ReferralDetails? get() {
    return referralDetailsHistory?.firstOrNull { it.supersededById == null }
  }
}
