package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.CollectionTable
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.NamedAttributeNode
import jakarta.persistence.NamedEntityGraph
import jakarta.persistence.NamedSubgraph
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.PrimaryKeyJoinColumn
import jakarta.persistence.Table
import jakarta.validation.constraints.NotNull
import org.hibernate.annotations.Fetch
import org.hibernate.annotations.FetchMode
import org.hibernate.annotations.Where
import java.time.OffsetDateTime
import java.util.UUID
@NamedEntityGraph(
  name = "entity-referral-graph",
  attributeNodes =
  [
    NamedAttributeNode("sentBy"),
    NamedAttributeNode(value = "serviceUserData", subgraph = "serviceUserData"),
    NamedAttributeNode(value = "intervention", subgraph = "interventions"),
    NamedAttributeNode(value = "endOfServiceReport", subgraph = "endOfServiceReport"),
    NamedAttributeNode(value = "supplierAssessment", subgraph = "supplierAssessmentData"),
  ],
  subgraphs = [
    NamedSubgraph(
      name = "interventions",
      attributeNodes = [
        NamedAttributeNode("dynamicFrameworkContract"),
      ],
    ),
    NamedSubgraph(
      name = "endOfServiceReport",
      attributeNodes = [
        NamedAttributeNode("referral"),
        NamedAttributeNode("createdBy"),
        NamedAttributeNode("submittedBy"),
      ],
    ),
    NamedSubgraph(
      name = "serviceUserData",
      attributeNodes = [
        NamedAttributeNode("disabilities"),
        NamedAttributeNode("referral"),
      ],
    ),
    NamedSubgraph(
      name = "supplierAssessmentData",
      attributeNodes = [
        NamedAttributeNode("referral"),
        NamedAttributeNode("appointments"),
      ],
    ),
  ],
)
@Entity(name = "referral")
@Table(name = "referral", indexes = [Index(columnList = "created_by_id")])
class SentReferralSummary(
  @Id val id: UUID,
  @ElementCollection
  @CollectionTable(name = "referral_assignments")
  @Where(clause = "superseded = false")
  val assignments: MutableList<ReferralAssignment> = mutableListOf(),
  @ManyToOne
  @Fetch(FetchMode.JOIN)
  var sentBy: AuthUser,
  var sentAt: OffsetDateTime,
  var concludedAt: OffsetDateTime? = null,
  var referenceNumber: String,
  @OneToOne(mappedBy = "referral", cascade = [CascadeType.ALL]) @PrimaryKeyJoinColumn
  var serviceUserData: ReferralServiceUserData?,
  @NotNull
  @ManyToOne
  @Fetch(FetchMode.JOIN)
  val createdBy: AuthUser,
  var endRequestedAt: OffsetDateTime? = null,
  @NotNull
  @ManyToOne(fetch = FetchType.LAZY)
  val intervention: Intervention,
  @NotNull val serviceUserCRN: String,
  @OneToOne(mappedBy = "referral")
  @Fetch(FetchMode.JOIN)
  var endOfServiceReport: EndOfServiceReport? = null,
  @OneToOne(mappedBy = "referral")
  @Fetch(FetchMode.JOIN)
  var supplierAssessment: SupplierAssessment? = null,
  @OneToOne(mappedBy = "referral")
  @Fetch(FetchMode.JOIN)
  var referralLocation: ReferralLocation? = null,
  @OneToOne(mappedBy = "referral")
  @Fetch(FetchMode.JOIN)
  var probationPractitionerDetails: ProbationPractitionerDetails? = null,
  @OneToMany(fetch = FetchType.LAZY)
  @JoinColumn(name = "referral_id")
  var actionPlans: MutableList<ActionPlan>? = null,
) {
  private val currentAssignment: ReferralAssignment?
    get() = assignments.maxByOrNull { it.assignedAt }
  val currentAssignee: AuthUser?
    get() = currentAssignment?.assignedTo
}
