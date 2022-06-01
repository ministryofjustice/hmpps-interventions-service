package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity

import org.hibernate.annotations.Fetch
import org.hibernate.annotations.FetchMode
import org.hibernate.annotations.Where
import java.time.OffsetDateTime
import java.util.UUID
import javax.persistence.CascadeType
import javax.persistence.CollectionTable
import javax.persistence.ElementCollection
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.Id
import javax.persistence.Index
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.NamedAttributeNode
import javax.persistence.NamedEntityGraph
import javax.persistence.NamedSubgraph
import javax.persistence.OneToMany
import javax.persistence.OneToOne
import javax.persistence.PrimaryKeyJoinColumn
import javax.persistence.Table
import javax.validation.constraints.NotNull
@NamedEntityGraph(
  name = "entity-referral-graph",
  attributeNodes =
  [
    NamedAttributeNode("sentBy"),
    NamedAttributeNode(value = "serviceUserData", subgraph = "serviceUserData"),
    NamedAttributeNode(value = "intervention", subgraph = "interventions"),
    NamedAttributeNode(value = "endOfServiceReport", subgraph = "endOfServiceReport"),
    NamedAttributeNode(value = "supplierAssessment", subgraph = "supplierAssessmentData")
  ],
  subgraphs = [
    NamedSubgraph(
      name = "interventions",
      attributeNodes = [
        NamedAttributeNode("dynamicFrameworkContract"),
      ]
    ),
    NamedSubgraph(
      name = "endOfServiceReport",
      attributeNodes = [
        NamedAttributeNode("referral"),
        NamedAttributeNode("createdBy"),
        NamedAttributeNode("submittedBy")
      ]
    ),
    NamedSubgraph(
      name = "serviceUserData",
      attributeNodes = [
        NamedAttributeNode("disabilities"),
        NamedAttributeNode("draftReferral")
      ]
    ),
    NamedSubgraph(
      name = "supplierAssessmentData",
      attributeNodes = [
        NamedAttributeNode("referral"),
        NamedAttributeNode("appointments")
      ]
    )
  ]
)
@Entity(name = "referral")
@Table(name = "referral", indexes = [Index(columnList = "created_by_id")])
class SentReferralSummary(
  @Id val id: UUID,
  @ElementCollection
  @CollectionTable(name = "referral_assignments")
  @Where(clause = "superseded = false")
  val assignments: MutableList<ReferralAssignment> = mutableListOf(),
  @ManyToOne @Fetch(FetchMode.JOIN) var sentBy: AuthUser,
  var sentAt: OffsetDateTime,
  var concludedAt: OffsetDateTime? = null,
  var referenceNumber: String,
  @OneToOne(mappedBy = "draftReferral", cascade = [CascadeType.ALL]) @PrimaryKeyJoinColumn var serviceUserData: ServiceUserData?,
  @NotNull @ManyToOne @Fetch(FetchMode.JOIN) val createdBy: AuthUser,
  var endRequestedAt: OffsetDateTime? = null,
  @NotNull @ManyToOne(fetch = FetchType.LAZY) val intervention: Intervention,
  @NotNull val serviceUserCRN: String,
  @OneToOne(mappedBy = "referral") @Fetch(FetchMode.JOIN) var endOfServiceReport: EndOfServiceReport? = null,
  @OneToOne(mappedBy = "referral") @Fetch(FetchMode.JOIN) var supplierAssessment: SupplierAssessment? = null,
  @OneToMany(fetch = FetchType.LAZY) @JoinColumn(name = "referral_id") var actionPlans: MutableList<ActionPlan>? = null,
) {
  private val currentAssignment: ReferralAssignment?
    get() = assignments.maxByOrNull { it.assignedAt }
  val currentAssignee: AuthUser?
    get() = currentAssignment?.assignedTo
}
