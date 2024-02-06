package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util

import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Referral
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ReferralServiceUserData

class ReferralServiceUserFactory(em: TestEntityManager? = null) : EntityFactory(em) {
  private val referralFactory = ReferralFactory(em)
  fun create(
    firstName: String? = null,
    lastName: String? = null,
    referral: Referral = referralFactory.createSent(),
  ): ReferralServiceUserData {
    return save(
      ReferralServiceUserData(
        firstName = firstName,
        lastName = lastName,
        referral = referral,
        referralID = referral.id,
      ),
    )
  }
}
