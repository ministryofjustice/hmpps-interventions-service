package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity

import org.hibernate.annotations.Fetch
import org.hibernate.annotations.FetchMode.JOIN
import java.time.OffsetDateTime
import java.util.UUID
import javax.persistence.CascadeType
import javax.persistence.CollectionTable
import javax.persistence.Column
import javax.persistence.ElementCollection
import javax.persistence.Embeddable
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.Id
import javax.persistence.Index
import javax.persistence.JoinColumn
import javax.persistence.JoinTable
import javax.persistence.ManyToMany
import javax.persistence.ManyToOne
import javax.persistence.OneToMany
import javax.persistence.OneToOne
import javax.persistence.OrderBy
import javax.persistence.PrePersist
import javax.persistence.PrimaryKeyJoinColumn
import javax.persistence.Table
import javax.validation.constraints.NotNull

@Embeddable
class SelectedDesiredOutcomesMapping(
  @Column(name = "service_category_id")
  var serviceCategoryId: UUID,
  @Column(name = "desired_outcome_id")
  var desiredOutcomeId: UUID
)

@Entity
@Table(name = "referral", indexes = [Index(columnList = "created_by_id")])
class Referral(
  @ElementCollection
  val assignments: MutableList<ReferralAssignment> = mutableListOf(),
  // sent referral fields
  var sentAt: OffsetDateTime? = null,
  @ManyToOne @Fetch(JOIN) var sentBy: AuthUser? = null,
  var referenceNumber: String? = null,
  var supplementaryRiskId: UUID? = null,

  var endRequestedAt: OffsetDateTime? = null,
  @ManyToOne @Fetch(JOIN) var endRequestedBy: AuthUser? = null,
  @ManyToOne @JoinColumn(name = "end_requested_reason_code") var endRequestedReason: CancellationReason? = null,
  var endRequestedComments: String? = null,
  var concludedAt: OffsetDateTime? = null,

  // draft referral fields
  @OneToOne(mappedBy = "draftReferral", cascade = [CascadeType.ALL]) @PrimaryKeyJoinColumn var serviceUserData: ServiceUserData? = null,
  @Column(name = "draft_supplementary_risk") var additionalRiskInformation: String? = null,
  @Column(name = "draft_supplementary_risk_updated_at") var additionalRiskInformationUpdatedAt: OffsetDateTime? = null,
  var furtherInformation: String? = null,
  var additionalNeedsInformation: String? = null,
  var accessibilityNeeds: String? = null,
  var needsInterpreter: Boolean? = null,
  var interpreterLanguage: String? = null,
  var hasAdditionalResponsibilities: Boolean? = null,
  var whenUnavailable: String? = null,
  @ElementCollection
  @CollectionTable(name = "referral_desired_outcome", joinColumns = [JoinColumn(name = "referral_id")])
  var selectedDesiredOutcomes: MutableList<SelectedDesiredOutcomesMapping>? = null,

  var relevantSentenceId: Long? = null,

  @OneToMany(fetch = FetchType.LAZY) @OrderBy("createdAt")
  @JoinColumn(name = "referral_id") var actionPlans: MutableList<ActionPlan>? = null,

  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(
    name = "referral_selected_service_category",
    joinColumns = [JoinColumn(name = "referral_id")],
    inverseJoinColumns = [JoinColumn(name = "service_category_id")]
  )
  var selectedServiceCategories: MutableSet<ServiceCategory>? = mutableSetOf(),

  @ElementCollection
  @CollectionTable(name = "referral_complexity_level_ids", joinColumns = [JoinColumn(name = "referral_id")])
  var complexityLevelIds: MutableMap<UUID, UUID>? = mutableMapOf(),

  // required fields
  @NotNull @ManyToOne(fetch = FetchType.LAZY) var intervention: Intervention,
  @NotNull val serviceUserCRN: String,
  @NotNull @ManyToOne @Fetch(JOIN) val createdBy: AuthUser,
  @NotNull val createdAt: OffsetDateTime,
  @Id val id: UUID,

  @OneToOne(mappedBy = "referral") @Fetch(JOIN) var endOfServiceReport: EndOfServiceReport? = null,
  @OneToOne(mappedBy = "referral") @Fetch(JOIN) var supplierAssessment: SupplierAssessment? = null,

  @OneToMany(fetch = FetchType.LAZY) @JoinColumn(name = "referral_id")
  private val referralDetailsHistory: Set<ReferralDetails>? = null,
) {
  val approvedActionPlan: ActionPlan?
    get() = actionPlans?.filter { it.approvedAt != null }?.maxByOrNull { it.approvedAt!! }

  val currentActionPlan: ActionPlan?
    get() = actionPlans?.maxByOrNull { it.createdAt }

  val currentAssignment: ReferralAssignment?
    get() = assignments.maxByOrNull { it.assignedAt }

  val currentAssignee: AuthUser?
    get() = currentAssignment?.assignedTo

  val endState: String? get() {
    if (concludedAt == null) {
      return null
    }

    if (endRequestedAt == null) {
      return "completed"
    }

    return if (endOfServiceReport == null) "cancelled" else "ended"
  }

  val referralDetails: ReferralDetails? get() {
    return referralDetailsHistory?.firstOrNull { it.supersededById == null }
  }

  // A better way to ensure invariants, perhaps, would be to remove the `assignment` collection
  // and use a @Service to add/remove assignments and validate invariants during those operations
  @PrePersist
  fun validateInvariants() {
    // despite the type guarantee, the persistence layer can create this class with a `null` assignments field
    if (assignments.isNullOrEmpty()) {
      return
    }

    val expectedSuperseded = assignments.size - 1
    val totalSuperseded = assignments.count { it.superseded }
    val latestIsNotSuperseded = currentAssignment!!.superseded.not()
    val valid = (totalSuperseded == expectedSuperseded) && latestIsNotSuperseded

    if (!valid) {
      throw IllegalStateException("Invariant violation: referral assignments must have all assignments superseded, except the latest one")
    }
  }
}
