package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.data.domain.PageRequest
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AuthUser
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.AuthUserFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ReferralFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.RepositoryTest
import java.time.OffsetDateTime

@Transactional
@RepositoryTest
class ReferralRepositoryTest @Autowired constructor(
  val entityManager: TestEntityManager,
  val endOfServiceReportRepository: EndOfServiceReportRepository,
  val referralRepository: ReferralRepository,
  val interventionRepository: InterventionRepository,
  val authUserRepository: AuthUserRepository,
) {
  private val authUserFactory = AuthUserFactory(entityManager)
  private val referralFactory = ReferralFactory(entityManager)
  private lateinit var authUser: AuthUser

  @BeforeEach
  fun beforeEach() {
    authUser = authUserFactory.create()
  }

  @Test
  fun `service provider report sanity check`() {
    val referral = referralFactory.createSent()
    referralFactory.createSent(sentAt = OffsetDateTime.now().minusHours(2), intervention = referral.intervention)

    val referrals = referralRepository.serviceProviderReportReferrals(
      OffsetDateTime.now().minusHours(1),
      OffsetDateTime.now().plusHours(1),
      setOf(referral.intervention.dynamicFrameworkContract),
      PageRequest.of(0, 5),
    )

    assertThat(referrals.numberOfElements).isEqualTo(1)
  }
}
