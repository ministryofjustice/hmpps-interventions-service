package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Attended
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.AuthUserFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.DeliverySessionFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ReferralFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.RepositoryTest

@RepositoryTest
class DeliverySessionRepositoryTest @Autowired constructor(
  val entityManager: TestEntityManager,
  val deliverySessionRepository: DeliverySessionRepository,
  val interventionRepository: InterventionRepository,
  val referralRepository: ReferralRepository,
  val authUserRepository: AuthUserRepository,
  val endOfServiceReportRepository: EndOfServiceReportRepository,
  val supplierAssessmentRepository: SupplierAssessmentRepository,
  val appointmentRepository: AppointmentRepository,
  val dynamicFrameworkContractRepository: DynamicFrameworkContractRepository,
) {
  private val authUserFactory = AuthUserFactory(entityManager)
  private val referralFactory = ReferralFactory(entityManager)
  private val deliverySessionFactory = DeliverySessionFactory(entityManager)

  @BeforeEach
  fun setup() {
    deliverySessionRepository.deleteAll()
    supplierAssessmentRepository.deleteAll()
    appointmentRepository.deleteAll()
    endOfServiceReportRepository.deleteAll()

    entityManager.flush()
    entityManager.clear()

    referralRepository.deleteAll()
    interventionRepository.deleteAll()
    dynamicFrameworkContractRepository.deleteAll()
    authUserRepository.deleteAll()
    entityManager.flush()
  }

  @Test
  fun `can retrieve an action plan session`() {
    val user = authUserFactory.create(id = "referral_repository_test_user_id")
    val referral = referralFactory.createSent(createdBy = user)
    val deliverySession = deliverySessionFactory.createScheduled(referral = referral)

    entityManager.flush()
    entityManager.clear()

    val savedSession = deliverySessionRepository.findById(deliverySession.id).get()

    assertThat(savedSession.id).isEqualTo(deliverySession.id)
  }

  @Test
  fun `count number of appointments with recorded attendances`() {
    val referral1 = referralFactory.createSent()
    (1..4).forEach {
      deliverySessionFactory.createAttended(referral = referral1, sessionNumber = it)
    }
    val referral2 = referralFactory.createSent()
    deliverySessionFactory.createAttended(referral = referral2)

    assertThat(deliverySessionRepository.countNumberOfSessionsWithAttendanceRecord(referral1.id)).isEqualTo(4)
    assertThat(deliverySessionRepository.countNumberOfSessionsWithAttendanceRecord(referral2.id)).isEqualTo(1)
  }

  @Test
  fun `count number of attended sessions`() {
    val referral1 = referralFactory.createSent()
    (1..4).forEach {
      deliverySessionFactory.createAttended(referral = referral1, sessionNumber = it)
    }
    val referral2 = referralFactory.createSent()
    deliverySessionFactory.createAttended(referral = referral2)

    assertThat(deliverySessionRepository.countNumberOfAttendedSessions(referral1.id)).isEqualTo(4)
    assertThat(deliverySessionRepository.countNumberOfAttendedSessions(referral2.id)).isEqualTo(1)
  }

  @Test
  fun `only sessions that were attended as yes or late are included in attended count`() {
    val referral1 = referralFactory.createSent()

    deliverySessionFactory.createAttended(referral = referral1, attended = Attended.YES, sessionNumber = 1)
    deliverySessionFactory.createAttended(referral = referral1, attended = Attended.LATE, sessionNumber = 2)
    deliverySessionFactory.createAttended(referral = referral1, attended = Attended.NO, sessionNumber = 3)

    assertThat(deliverySessionRepository.countNumberOfAttendedSessions(referral1.id)).isEqualTo(2)
  }

  @Test
  fun `scheduled sessions that are not yet attended are not included in attended count`() {
    val referral1 = referralFactory.createSent()

    deliverySessionFactory.createAttended(referral = referral1, attended = Attended.YES, sessionNumber = 1)
    deliverySessionFactory.createScheduled(referral = referral1, sessionNumber = 2)

    assertThat(deliverySessionRepository.countNumberOfAttendedSessions(referral1.id)).isEqualTo(1)
  }
}
