package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.data.domain.PageRequest
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.SampleData
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.AuthUserFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ReferralFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.RepositoryTest
import java.time.OffsetDateTime

@RepositoryTest
class ReferralRepositoryTest @Autowired constructor(
  val entityManager: TestEntityManager,
  val referralRepository: ReferralRepository,
  val authUserRepository: AuthUserRepository,
) {
  private val authUserFactory = AuthUserFactory(entityManager)
  private val referralFactory = ReferralFactory(entityManager)

  @Test
  fun `removing a referral does not remove associated auth_user`() {
    val user = authUserFactory.create(id = "referral_repository_test_user_id")
    val referral = SampleData.sampleReferral("X123456", "Harmony Living", createdBy = user)
    SampleData.persistReferral(entityManager, referral)

    // check that when the referral is deleted, the user is not
    entityManager.remove(referral)
    Assertions.assertThat(referralRepository.findByIdOrNull(referral.id)).isNull()
    val userAfterDelete = authUserRepository.findByIdOrNull(user.id)
    Assertions.assertThat(userAfterDelete).isNotNull
    Assertions.assertThat(userAfterDelete).isEqualTo(user)
  }

  @Test
  fun `referral report sanity check`() {
    val referral = referralFactory.createSent()
    referralFactory.createSent(sentAt = OffsetDateTime.now().minusHours(2), intervention = referral.intervention)

    val referrals = referralRepository.serviceProviderReportReferralIds(
      OffsetDateTime.now().minusHours(1),
      OffsetDateTime.now().plusHours(1),
      setOf(referral.intervention.dynamicFrameworkContract),
      PageRequest.of(0, 5),
    )

    Assertions.assertThat(referrals.numberOfElements).isEqualTo(1)
  }
}
