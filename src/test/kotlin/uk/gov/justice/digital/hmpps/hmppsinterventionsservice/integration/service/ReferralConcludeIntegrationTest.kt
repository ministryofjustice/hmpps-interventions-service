package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.integration.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.ReferralEventPublisher
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Attended
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.AppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.AuthUserRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.DeliverySessionRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ReferralRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.ReferralConcluder
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ActionPlanFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.AppointmentFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.DeliverySessionFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.RepositoryTest
import java.time.OffsetDateTime

@RepositoryTest
class ReferralConcludeIntegrationTest @Autowired constructor(
  val entityManager: TestEntityManager,
  val referralRepository: ReferralRepository,
  val deliverySessionRepository: DeliverySessionRepository,
  val authUserRepository: AuthUserRepository,
  val appointmentRepository: AppointmentRepository,

) {
  private val actionPlanFactory = ActionPlanFactory(entityManager)
  private val appointmentFactory = AppointmentFactory(entityManager)
  private val deliverySessionFactory = DeliverySessionFactory(entityManager)
  private val referralEventPublisher: ReferralEventPublisher = mock()

  @AfterEach
  fun `clear referrals`() {
    referralRepository.deleteAll()
    deliverySessionRepository.deleteAll()
  }

  @Test
  fun `an action plan number of sessions should be the same even with more than one appointment`() {
    val referralConcludeCheck = ReferralConcluder(referralRepository, deliverySessionRepository, referralEventPublisher)
    val actionPlan = actionPlanFactory.createApproved(numberOfSessions = 1)
    actionPlan.referral.actionPlans = mutableListOf(actionPlan)

    // then make a bunch of appointments
    val unattendedAppointments = (1..5).map {
      appointmentFactory.create(referral = actionPlan.referral, attended = Attended.YES, appointmentFeedbackSubmittedAt = OffsetDateTime.now(), superseded = true)
    }
    val attendedAppointments = (1..5).map {
      appointmentFactory.create(referral = actionPlan.referral, attended = Attended.YES, appointmentFeedbackSubmittedAt = OffsetDateTime.now(), superseded = false)
    }
    val lateAppointments = (1..5).map {
      appointmentFactory.create(referral = actionPlan.referral, attended = Attended.LATE, appointmentFeedbackSubmittedAt = OffsetDateTime.now(), superseded = true)
    }

    val session = deliverySessionFactory.createAttendedDuplicateReferral(
      referral = actionPlan.referral,
      appointment = mutableSetOf(
        unattendedAppointments[0],
        attendedAppointments[1],
        lateAppointments[0]
      )
    )
    // now we'll make a bunch of sessions with various unattended and attended appointments on them
    val referralConclude = referralConcludeCheck.requiresEndOfServiceReportCreation(actionPlan.referral)

    assertThat(session.appointments.size).isEqualTo(3)
    assertThat(referralConclude).isTrue
  }
}
