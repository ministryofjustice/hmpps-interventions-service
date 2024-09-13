package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.DeliverySessionRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ReferralRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.DeliverySessionFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ReferralFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.RepositoryTest
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

@RepositoryTest
class SarsDataServiceTest @Autowired constructor(
  val entityManager: TestEntityManager,
  val referralRepository: ReferralRepository,
  val deliverySessionRepository: DeliverySessionRepository,
) {
  private val referralFactory = ReferralFactory(entityManager)
  private val deliverySessionFactory = DeliverySessionFactory(entityManager)

  @AfterEach
  fun `clear referrals`() {
    referralRepository.deleteAll()
    deliverySessionRepository.deleteAll()
  }
  private val sarsDataService = SarsDataService(referralRepository, deliverySessionRepository)

  @Test
  fun `get sars data for the given service crn`() {
    val id = UUID.randomUUID()
    val referral = referralFactory.createSent(
      createdAt = OffsetDateTime.now(),
      id = id,
      sentAt = OffsetDateTime.now(),
      serviceUserCRN = "crn",
    )

    val session1 = deliverySessionFactory.createAttended(referral = referral, sessionNumber = 1)
    val session2 = deliverySessionFactory.createAttended(referral = referral, sessionNumber = 2)

    val sarsData = sarsDataService.getSarsReferralData("crn")

    assertThat(sarsData.size).isEqualTo(1)
    assertThat(sarsData[0].referral.id).isEqualTo(id)
    assertThat(sarsData[0].appointments.size).isEqualTo(2)
    assertThat(sarsData[0].appointments).containsExactlyInAnyOrder(session1.currentAppointment, session2.currentAppointment)
  }

  @Test
  fun `get sars data for the given service crn and created date range`() {
    val referral1Id = UUID.randomUUID()
    val referral2Id = UUID.randomUUID()
    val referral1 = referralFactory.createSent(
      createdAt = OffsetDateTime.of(2024, 5, 20, 1, 10, 10, 0, ZoneOffset.UTC),
      id = referral1Id,
      sentAt = OffsetDateTime.now(),
      serviceUserCRN = "crn",
    )

    val referral2 = referralFactory.createSent(
      createdAt = OffsetDateTime.of(2024, 2, 10, 1, 10, 10, 0, ZoneOffset.UTC),
      id = referral2Id,
      sentAt = OffsetDateTime.now(),
      serviceUserCRN = "crn",
    )

    val referral3 = referralFactory.createSent(
      createdAt = OffsetDateTime.of(2024, 8, 10, 1, 10, 10, 0, ZoneOffset.UTC),
      id = UUID.randomUUID(),
      sentAt = OffsetDateTime.now(),
      serviceUserCRN = "crn",
    )

    deliverySessionFactory.createAttended(referral = referral1, sessionNumber = 1)
    deliverySessionFactory.createAttended(referral = referral1, sessionNumber = 2)
    deliverySessionFactory.createAttended(referral = referral2, sessionNumber = 1)

    val sarsData = sarsDataService.getSarsReferralData("crn", "01-01-2024", "01-06-2024")

    assertThat(sarsData.size).isEqualTo(2)
    assertThat(sarsData.map { it.referral })
      .containsExactlyInAnyOrder(referral1, referral2)
      .doesNotContain(referral3)

    val firstReferralsAppointments = sarsData.filter { it.referral.id == referral1.id }.flatMap { it.appointments }
    assertThat(firstReferralsAppointments.size).isEqualTo(2)

    val secondReferralsAppointments = sarsData.filter { it.referral.id == referral2.id }.flatMap { it.appointments }
    assertThat(secondReferralsAppointments.size).isEqualTo(1)
  }

  @Test
  fun `get sars data for the given service crn and appointments not available`() {
    val id = UUID.randomUUID()
    referralFactory.createSent(
      createdAt = OffsetDateTime.now(),
      id = id,
      sentAt = OffsetDateTime.now(),
      serviceUserCRN = "crn",
    )

    val sarsData = sarsDataService.getSarsReferralData("crn")

    assertThat(sarsData.size).isEqualTo(1)
    assertThat(sarsData[0].referral.id).isEqualTo(id)
    assertThat(sarsData[0].appointments.size).isEqualTo(0)
  }

  @Test
  fun `returns no sars data when there is no matching referrals for the crn`() {
    val id = UUID.randomUUID()
    val referral = referralFactory.createSent(
      createdAt = OffsetDateTime.now(),
      id = id,
      sentAt = OffsetDateTime.now(),
      serviceUserCRN = "crn",
    )

    deliverySessionFactory.createAttended(referral = referral, sessionNumber = 1)
    deliverySessionFactory.createAttended(referral = referral, sessionNumber = 2)

    val sarsData = sarsDataService.getSarsReferralData("non-existence-crn")

    assertThat(sarsData.size).isEqualTo(0)
  }
}
