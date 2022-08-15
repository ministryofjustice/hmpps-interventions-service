package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util

import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.DraftReferral
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ServiceUserData

class ServiceUserFactory(em: TestEntityManager? = null) : EntityFactory(em) {
  private val referralFactory = ReferralFactory(em)
  fun create(
    firstName: String? = null,
    lastName: String? = null,
    referral: DraftReferral = referralFactory.createDraft()
  ): ServiceUserData {
    return save(
      ServiceUserData(
        firstName = firstName,
        lastName = lastName,
        draftReferral = referral,
        referralID = referral.id
      )
    )
  }
}
