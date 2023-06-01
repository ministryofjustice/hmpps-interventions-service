package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity

import java.util.UUID
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.Id
import javax.persistence.OneToOne
import javax.persistence.Table

@Entity
@Table(name = "probation_practitioner_details")
data class ProbationPractitionerDetails(
  @Id val id: UUID,
  @OneToOne(fetch = FetchType.LAZY) val referral: Referral,
  @Column(name = "ndelius_name") var nDeliusName: String?,
  @Column(name = "ndelius_email_address") var nDeliusEmailAddress: String?,
  @Column(name = "ndelius_pdu") var nDeliusPDU: String?,
  @Column(name = "name") var name: String?,
  @Column(name = "email_address") var emailAddress: String?,
  @Column(name = "pdu") var pdu: String?,
  @Column(name = "probation_office") var probationOffice: String?,
)
