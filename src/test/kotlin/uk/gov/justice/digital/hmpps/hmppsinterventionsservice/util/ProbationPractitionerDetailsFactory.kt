package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util

import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ProbationPractitionerDetails
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Referral
import java.util.UUID

class ProbationPractitionerDetailsFactory(em: TestEntityManager? = null) : EntityFactory(em) {

  fun create(
    id: UUID? = UUID.randomUUID(),
    referral: Referral,
    nDeliusName: String? = "delius name",
    nDeliusEmailAddress: String? = "a.b@xyz.com",
    nDeliusPdu: String? = "pdu1",
    name: String? = "user name",
    emailAddress: String? = "c.d@xyz.com",
    pdu: String? = "pdu2",
    probationOffice: String? = "probation-office",
    roleOrJobTitle: String? = "Probabation Practitioner",
    ppEstablishment: String? = "aaa",
  ): ProbationPractitionerDetails {
    return save(
      ProbationPractitionerDetails(
        id = id ?: UUID.randomUUID(),
        referral = referral,
        nDeliusName = nDeliusName,
        nDeliusEmailAddress = nDeliusEmailAddress,
        nDeliusPDU = nDeliusPdu,
        name = name,
        emailAddress = emailAddress,
        pdu = pdu,
        probationOffice = probationOffice,
        roleOrJobTitle = roleOrJobTitle,
        establishment = ppEstablishment,
      ),
    )
  }
}
