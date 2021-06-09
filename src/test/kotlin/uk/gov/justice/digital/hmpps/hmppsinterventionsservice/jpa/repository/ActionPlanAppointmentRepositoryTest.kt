package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ActionPlanAppointmentFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ActionPlanFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.AuthUserFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ReferralFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.RepositoryTest

@RepositoryTest
class ActionPlanAppointmentRepositoryTest @Autowired constructor(
  val entityManager: TestEntityManager,
  val actionPlanRepository: ActionPlanRepository,
  val actionPlanAppointmentRepository: ActionPlanAppointmentRepository,
  val interventionRepository: InterventionRepository,
  val referralRepository: ReferralRepository,
  val authUserRepository: AuthUserRepository,
  val endOfServiceReportRepository: EndOfServiceReportRepository,
  val appointmentRepository: AppointmentRepository,
) {
  private val authUserFactory = AuthUserFactory(entityManager)
  private val referralFactory = ReferralFactory(entityManager)
  private val actionPlanFactory = ActionPlanFactory(entityManager)
  private val actionPlanAppointmentFactory = ActionPlanAppointmentFactory(entityManager)

  @BeforeEach
  fun setup() {
    actionPlanAppointmentRepository.deleteAll()
    actionPlanRepository.deleteAll()
    endOfServiceReportRepository.deleteAll()
    referralRepository.deleteAll()
    interventionRepository.deleteAll()
    appointmentRepository.deleteAll()
    authUserRepository.deleteAll()
  }

  @Test
  fun `can retrieve an action plan appointment`() {
    val user = authUserFactory.create(id = "referral_repository_test_user_id")
    val referral = referralFactory.createDraft(createdBy = user)
    val actionPlan = actionPlanFactory.create(referral = referral)
    val actionPlanAppointment = actionPlanAppointmentFactory.create(actionPlan = actionPlan)

    entityManager.flush()
    entityManager.clear()

    val savedAppointment = actionPlanAppointmentRepository.findById(actionPlanAppointment.id).get()

    assertThat(savedAppointment.id).isEqualTo(actionPlanAppointment.id)
  }

  @Test
  fun `count number of attended appointments`() {
    val actionPlan1 = actionPlanFactory.create()
    (1..4).forEach {
      actionPlanAppointmentFactory.createAttended(actionPlan = actionPlan1, sessionNumber = it)
    }
    val actionPlan2 = actionPlanFactory.create()
    actionPlanAppointmentFactory.createAttended(actionPlan = actionPlan2)

    assertThat(actionPlanAppointmentRepository.countByActionPlanIdAndAppointmentAttendedIsNotNull(actionPlan1.id)).isEqualTo(4)
    assertThat(actionPlanAppointmentRepository.countByActionPlanIdAndAppointmentAttendedIsNotNull(actionPlan2.id)).isEqualTo(1)
  }
}
