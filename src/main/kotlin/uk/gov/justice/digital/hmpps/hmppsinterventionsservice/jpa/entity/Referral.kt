package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity

import org.hibernate.annotations.Fetch
import org.hibernate.annotations.FetchMode.JOIN
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.reporting.serviceprovider.performance.model.PerformanceReportReferral
import java.time.OffsetDateTime
import java.util.UUID
import javax.persistence.CascadeType
import javax.persistence.CollectionTable
import javax.persistence.Column
import javax.persistence.ColumnResult
import javax.persistence.ConstructorResult
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
import javax.persistence.NamedNativeQuery
import javax.persistence.OneToMany
import javax.persistence.OneToOne
import javax.persistence.OrderBy
import javax.persistence.PrePersist
import javax.persistence.PrimaryKeyJoinColumn
import javax.persistence.SqlResultSetMapping
import javax.persistence.Table
import javax.validation.constraints.NotNull

@Embeddable
class SelectedDesiredOutcomesMapping(
  @Column(name = "service_category_id")
  var serviceCategoryId: UUID,
  @Column(name = "desired_outcome_id")
  var desiredOutcomeId: UUID,
)

@Entity
@Table(name = "referral", indexes = [Index(columnList = "created_by_id")])
@NamedNativeQuery(
  name = "find_performance_report_referral",
  query =
  """
        with cte 
        as
        (
        select referral_id, assigned_to_id, RANK () OVER ( 
            PARTITION BY referral_id
            ORDER BY assigned_at DESC
          ) current_assignment_rank  from referral_assignments
        ),
        saa
        as
        (
           select a.referral_id, a.created_at, a.appointment_time as appointmentTime, RANK () OVER ( 
            PARTITION BY a.referral_id
            ORDER BY a.created_at DESC
          ) supplier_assessment_appointment_rank from appointment a
            join supplier_assessment_appointment saa on a.id = saa.appointment_id
            join referral r on r.id = a.referral_id
        ),
        saa_not_attended
        as
        (
           select a.referral_id, a.appointment_time as notAttendedAppointmentTime, RANK () OVER ( 
            PARTITION BY a.referral_id
            ORDER BY a.appointment_time ASC
          ) saa_not_attended_rank from appointment a
            join supplier_assessment_appointment saa on a.id = saa.appointment_id
            join referral r on r.id = a.referral_id
            where a.attended = 'NO'
        ),
        saa_attended_late
        as
        (
           select a.referral_id, a.appointment_time as attendedOrLateAppointmentTime,a.attended, RANK () OVER ( 
            PARTITION BY a.referral_id
            ORDER BY a.appointment_time ASC
          ) saa_attended_late_rank from appointment a
            join supplier_assessment_appointment saa on a.id = saa.appointment_id
            join referral r on r.id = a.referral_id
            where a.attended = 'YES' or a.attended = 'LATE'
        ),
        action_plan_first_submitted_at
        as
        (
             select a.submitted_at, a.referral_id, RANK () OVER ( 
            PARTITION BY a.referral_id
            ORDER BY a.submitted_at ASC
          ) action_plan_submitted_rank from action_plan a join referral r on a.referral_id = r.id
         where a.submitted_at is not null
        ),
        action_plan_first_approved_at
        as
        (
             select a.approved_at, a.referral_id, a.number_of_sessions, RANK () OVER ( 
            PARTITION BY a.referral_id
            ORDER BY a.approved_At ASC
          ) action_plan_approved_rank from action_plan a join referral r on a.referral_id = r.id
         where a.approved_at is not null
        ),
        action_plan_latest_approved_at
        as
        (
             select a.id, a.approved_at, a.referral_id, RANK () OVER ( 
            PARTITION BY a.referral_id
            ORDER BY a.approved_At DESC
          ) action_plan_latest_approved_rank from action_plan a join referral r on a.referral_id = r.id
         where a.approved_at is not null
        ),
        desired_outcomes_size
        as
        (
            select distinct referral_id, count(1) over (PARTITION BY referral_id) AS numberOfOutcomes from referral_desired_outcome
  
        )
        select r.id as referralId, r.reference_number as referralReference, d.contract_reference as contractReference, d.prime_provider_id as organisationId, au.user_name as currentAssigneeEmail , 
        r.service_usercrn as serviceUserCRN, r.sent_at as dateReferralReceived, saa.created_at as dateSupplierAssessmentFirstArranged, saa.appointmentTime as dateSupplierAssessmentFirstScheduledFor, 
        saa_not_attended.notAttendedAppointmentTime as dateSupplierAssessmentFirstNotAttended,saa_attended_late.attendedOrLateAppointmentTime as dateSupplierAssessmentFirstAttended, 
        (saa_attended_late.attended = 'YES') as supplierAssessmentAttendedOnTime, action_plan_first_submitted_at.submitted_at as firstActionPlanSubmittedAt, action_plan_first_approved_at.approved_at as firstActionPlanApprovedAt,
        action_plan_latest_approved_at.id as approvedActionPlanId, rd.numberOfOutcomes as numberOfOutcomes, es.id as endOfServiceReportId,
        action_plan_first_approved_at.number_of_sessions as numberOfSessions, r.end_requested_at as endRequestedAt, cr.description as endRequestedReason,
        es.submitted_at as eosrSubmittedAt, r.concluded_at as concludedAt
  
        from referral r 
        left outer join intervention i on r.intervention_id = i.id
        left outer join dynamic_framework_contract d on d.id = i.dynamic_framework_contract_id
        left outer join cte rass on rass.referral_id = r.id and rass.current_assignment_rank=1
        left outer join saa on saa.referral_id = r.id and saa.supplier_assessment_appointment_rank = 1
        left outer join saa_not_attended on saa_not_attended.referral_id = r.id and saa_not_attended.saa_not_attended_rank = 1
        left outer join saa_attended_late on saa_attended_late.referral_id = r.id and saa_attended_late.saa_attended_late_rank = 1
        left outer join action_plan_first_submitted_at on action_plan_first_submitted_at.referral_id = r.id and action_plan_first_submitted_at.action_plan_submitted_rank = 1
        left outer join action_plan_first_approved_at on action_plan_first_approved_at.referral_id = r.id and action_plan_first_approved_at.action_plan_approved_rank = 1
        left outer join action_plan_latest_approved_at on action_plan_latest_approved_at.referral_id = r.id and action_plan_latest_approved_at.action_plan_latest_approved_rank = 1
        left outer join auth_user au on au.id = rass.assigned_to_id
        left outer join desired_outcomes_size rd on rd.referral_id = r.id
        left outer join end_of_service_report es on es.referral_id = r.id
        left outer join cancellation_reason cr on cr.code = r.end_requested_reason_code
        where r.sent_at > :from and r.sent_at < :to and d.contract_reference in :contractReferences
      """,
  resultSetMapping = "performanceReportReferralMapping",
)
@SqlResultSetMapping(
  name = "performanceReportReferralMapping",
  classes = [
    ConstructorResult(
      targetClass = PerformanceReportReferral::class,
      columns = arrayOf(
        ColumnResult(name = "referralId", type = UUID::class),
        ColumnResult(name = "referralReference", type = String::class),
        ColumnResult(name = "contractReference", type = String::class),
        ColumnResult(name = "organisationId", type = String::class),
        ColumnResult(name = "currentAssigneeEmail", type = String::class),
        ColumnResult(name = "serviceUserCRN", type = String::class),
        ColumnResult(name = "dateReferralReceived", type = OffsetDateTime::class),
        ColumnResult(name = "dateSupplierAssessmentFirstArranged", type = OffsetDateTime::class),
        ColumnResult(name = "dateSupplierAssessmentFirstScheduledFor", type = OffsetDateTime::class),
        ColumnResult(name = "dateSupplierAssessmentFirstNotAttended", type = OffsetDateTime::class),
        ColumnResult(name = "dateSupplierAssessmentFirstAttended", type = OffsetDateTime::class),
        ColumnResult(name = "supplierAssessmentAttendedOnTime", type = Boolean::class),
        ColumnResult(name = "firstActionPlanSubmittedAt", type = OffsetDateTime::class),
        ColumnResult(name = "firstActionPlanApprovedAt", type = OffsetDateTime::class),
        ColumnResult(name = "approvedActionPlanId", type = UUID::class),
        ColumnResult(name = "numberOfOutcomes", type = Int::class),
        ColumnResult(name = "endOfServiceReportId", type = UUID::class),
        ColumnResult(name = "numberOfSessions", type = Int::class),
        ColumnResult(name = "endRequestedAt", type = OffsetDateTime::class),
        ColumnResult(name = "endRequestedReason", type = String::class),
        ColumnResult(name = "eosrSubmittedAt", type = OffsetDateTime::class),
        ColumnResult(name = "concludedAt", type = OffsetDateTime::class),
      ),
    ),
  ],
)
class Referral(
  @ElementCollection
  val assignments: MutableList<ReferralAssignment> = mutableListOf(),
  // sent referral fields
  var sentAt: OffsetDateTime? = null,
  @ManyToOne
  @Fetch(JOIN)
  var sentBy: AuthUser? = null,
  var referenceNumber: String? = null,
  var supplementaryRiskId: UUID? = null,

  var endRequestedAt: OffsetDateTime? = null,
  @ManyToOne
  @Fetch(JOIN)
  var endRequestedBy: AuthUser? = null,
  @ManyToOne
  @JoinColumn(name = "end_requested_reason_code")
  var endRequestedReason: CancellationReason? = null,
  var endRequestedComments: String? = null,
  var concludedAt: OffsetDateTime? = null,

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
  @ElementCollection
  @CollectionTable(name = "referral_desired_outcome", joinColumns = [JoinColumn(name = "referral_id")])
  var selectedDesiredOutcomes: MutableList<SelectedDesiredOutcomesMapping>? = null,

  var relevantSentenceId: Long? = null,

  @OneToMany(fetch = FetchType.LAZY)
  @OrderBy("createdAt")
  @JoinColumn(name = "referral_id")
  var actionPlans: MutableList<ActionPlan>? = null,

  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(
    name = "referral_selected_service_category",
    joinColumns = [JoinColumn(name = "referral_id")],
    inverseJoinColumns = [JoinColumn(name = "service_category_id")],
  )
  var selectedServiceCategories: MutableSet<ServiceCategory>? = mutableSetOf(),

  @ElementCollection
  @CollectionTable(name = "referral_complexity_level_ids", joinColumns = [JoinColumn(name = "referral_id")])
  var complexityLevelIds: MutableMap<UUID, UUID>? = mutableMapOf(),

  // required fields
  @NotNull
  @ManyToOne(fetch = FetchType.LAZY)
  var intervention: Intervention,
  @NotNull val serviceUserCRN: String,
  @NotNull
  @ManyToOne
  @Fetch(JOIN)
  val createdBy: AuthUser,
  @NotNull val createdAt: OffsetDateTime,
  @Id val id: UUID,

  @OneToOne(mappedBy = "referral")
  @Fetch(JOIN)
  var endOfServiceReport: EndOfServiceReport? = null,
  @OneToOne(mappedBy = "referral")
  @Fetch(JOIN)
  var supplierAssessment: SupplierAssessment? = null,
  @OneToOne(mappedBy = "referral")
  @Fetch(JOIN)
  var referralLocation: ReferralLocation? = null,

  @OneToMany(fetch = FetchType.LAZY)
  @JoinColumn(name = "referral_id")
  private val referralDetailsHistory: Set<ReferralDetails>? = null,

  @OneToOne(mappedBy = "referral")
  @Fetch(JOIN)
  var probationPractitionerDetails: ProbationPractitionerDetails? = null,
) {
  val urn: String
    get() = "urn:hmpps:interventions-referral:$id"

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
