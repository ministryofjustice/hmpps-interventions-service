package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.data.repository.findByIdOrNull
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Referral

@DataJpaTest
@ActiveProfiles("test")
class ReferralRepositoryTest @Autowired constructor(
  val entityManager: TestEntityManager,
  val referralRepository: ReferralRepository
) {

  @Test
  fun `when findByIdOrNull then return referral`() {
    val referrals = listOf(Referral(), Referral())
    referrals.forEach { entityManager.persist(it) }
    entityManager.flush()

    val found = referralRepository.findByIdOrNull(referrals[0].id!!)
    Assertions.assertThat(found).isEqualTo(referrals[0])
  }

  @Test
  fun `when findByCreatedByUsername then return list of referrals`() {
    val referrals = listOf(
      Referral(createdByUserId = "123"),
      Referral(createdByUserId = "123"),
      Referral(createdByUserId = "456"),
    )
    referrals.forEach { entityManager.persist(it) }
    entityManager.flush()

    val single = referralRepository.findByCreatedByUserId("456")
    Assertions.assertThat(single).hasSize(1)

    val multiple = referralRepository.findByCreatedByUserId("123")
    Assertions.assertThat(multiple).hasSize(2)

    val none = referralRepository.findByCreatedByUserId("789")
    Assertions.assertThat(none).hasSize(0)
  }
}
