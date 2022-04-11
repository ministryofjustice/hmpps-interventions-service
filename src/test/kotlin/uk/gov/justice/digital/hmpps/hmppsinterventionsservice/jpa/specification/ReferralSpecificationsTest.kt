package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.specification

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ReferralAssignment
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ActionPlanRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.AppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.AuthUserRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.CancellationReasonRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.DynamicFrameworkContractRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.EndOfServiceReportRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.InterventionRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ReferralRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.SentReferralSummariesRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.SupplierAssessmentRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.AuthUserFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.DynamicFrameworkContractFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.EndOfServiceReportFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.InterventionFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ReferralFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.RepositoryTest
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.SentReferralSummariesFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ServiceProviderFactory
import java.time.OffsetDateTime

@RepositoryTest
class ReferralSpecificationsTest @Autowired constructor(
  val entityManager: TestEntityManager,
  val referralRepository: ReferralRepository,
  val sentReferralSummariesRepository: SentReferralSummariesRepository,
  val authUserRepository: AuthUserRepository,
  val interventionRepository: InterventionRepository,
  val endOfServiceReportRepository: EndOfServiceReportRepository,
  val actionPlanRepository: ActionPlanRepository,
  val appointmentRepository: AppointmentRepository,
  val cancellationReasonRepository: CancellationReasonRepository,
  val supplierAssessmentRepository: SupplierAssessmentRepository,
  val dynamicFrameworkContractRepository: DynamicFrameworkContractRepository,
) {

  private val referralFactory = ReferralFactory(entityManager)
  private val referralSumariesFactory = SentReferralSummariesFactory(entityManager)
  private val authUserFactory = AuthUserFactory(entityManager)
  private val endOfServiceReportFactory = EndOfServiceReportFactory(entityManager)
  private val interventionFactory = InterventionFactory(entityManager)
  private val dynamicFrameworkContractFactory = DynamicFrameworkContractFactory(entityManager)
  private val serviceProviderFactory = ServiceProviderFactory(entityManager)

  @BeforeEach
  fun setup() {
    cancellationReasonRepository.deleteAll()
    appointmentRepository.deleteAll()
    actionPlanRepository.deleteAll()
    endOfServiceReportRepository.deleteAll()
    supplierAssessmentRepository.deleteAll()
    entityManager.flush()

    referralRepository.deleteAll()
    interventionRepository.deleteAll()
    dynamicFrameworkContractRepository.deleteAll()
    authUserRepository.deleteAll()
    entityManager.flush()
  }

  @Nested
  inner class sent {
    @Test
    fun `only sent referrals are returned`() {
      val sent = referralFactory.createSent()
      val sentReferralSummary = referralSumariesFactory.getReferralSummary(sent)
      val result = sentReferralSummariesRepository.findAll(ReferralSpecifications.sent())
      assertThat(result)
        .usingRecursiveFieldByFieldElementComparator()
        .containsExactly(sentReferralSummary)
    }
  }

  @Nested
  inner class concluded {
    @Test
    fun `only concluded referrals are returned`() {
      val sent = referralFactory.createSent()
      val cancelled = referralFactory.createEnded(endRequestedAt = OffsetDateTime.now(), concludedAt = OffsetDateTime.now(), endOfServiceReport = null)
      val completed = referralFactory.createEnded(endRequestedAt = OffsetDateTime.now(), concludedAt = OffsetDateTime.now())
      val endOfServiceReport = endOfServiceReportFactory.create(referral = completed)
      val sentReferralSummary = referralSumariesFactory.getReferralSummary(sent)
      val cancelledReferralSummary = referralSumariesFactory.getReferralSummary(cancelled)
      val completedReferralSummary = referralSumariesFactory.getReferralSummary(completed, endOfServiceReport)
      val result = sentReferralSummariesRepository.findAll(ReferralSpecifications.concluded())
      assertThat(result)
        .usingRecursiveFieldByFieldElementComparator()
        .containsExactlyInAnyOrder(completedReferralSummary, cancelledReferralSummary)
      assertThat(result).doesNotContain(sentReferralSummary)
    }
  }

  @Nested
  inner class cancelled {
    @Test
    fun `only cancelled referrals are returned`() {
      val cancelled = referralFactory.createEnded(endRequestedAt = OffsetDateTime.now(), concludedAt = OffsetDateTime.now(), endOfServiceReport = null)
      val completed = referralFactory.createEnded(endRequestedAt = OffsetDateTime.now(), concludedAt = OffsetDateTime.now())
      val endOfServiceReport = endOfServiceReportFactory.create(referral = completed)
      val cancelledReferralSummary = referralSumariesFactory.getReferralSummary(cancelled)
      val completedReferralSummary = referralSumariesFactory.getReferralSummary(completed, endOfServiceReport)
      val result = sentReferralSummariesRepository.findAll(ReferralSpecifications.cancelled())
      assertThat(result)
        .usingRecursiveFieldByFieldElementComparator()
        .containsExactly(cancelledReferralSummary)
      assertThat(result).doesNotContain(completedReferralSummary)
    }
  }

  @Nested
  inner class withSPAccess {
    @Test
    fun `sp with no contracts should never return a referral`() {
      referralFactory.createSent()
      referralFactory.createAssigned()

      val result = sentReferralSummariesRepository.findAll(ReferralSpecifications.withSPAccess(setOf()))
      assertThat(result).isEmpty()
    }

    @Test
    fun `only referrals that are part of the contract set are returned`() {
      val spContract = dynamicFrameworkContractFactory.create(contractReference = "spContractRef")
      val spIntervention = interventionFactory.create(contract = spContract)
      val referralWithSpContract = referralFactory.createSent(intervention = spIntervention)
      val referralSummaryWithSpContract = referralSumariesFactory.getReferralSummary(referralWithSpContract)

      val unrelatedSpContract = dynamicFrameworkContractFactory.create(contractReference = "unrelatedSpContractRef")

      val someOtherContract = dynamicFrameworkContractFactory.create(contractReference = "someOtherContractRef")
      val someOtherIntervention = interventionFactory.create(contract = someOtherContract)
      val someOtherReferral = referralFactory.createSent(intervention = someOtherIntervention)
      val someOtherReferralSummaryWithSpContract = referralSumariesFactory.getReferralSummary(referralWithSpContract)

      val result = sentReferralSummariesRepository.findAll(ReferralSpecifications.withSPAccess(setOf(spContract, unrelatedSpContract)))
      assertThat(result)
        .usingRecursiveFieldByFieldElementComparator()
        .containsExactly(referralSummaryWithSpContract)
      assertThat(result).doesNotContain(someOtherReferralSummaryWithSpContract)
    }
  }

  @Nested
  inner class assignedTo {
    @Test
    fun `returns referral if user is currently assigned`() {
      val user = authUserFactory.create(id = "loggedInUser")
      val someOtherUser = authUserFactory.create(id = "someOtherUser")

      val assignments: List<ReferralAssignment> = listOf(
        ReferralAssignment(OffsetDateTime.now(), assignedBy = someOtherUser, assignedTo = user),
        ReferralAssignment(OffsetDateTime.now().minusDays(1), assignedBy = someOtherUser, assignedTo = someOtherUser),
      )

      val assignedReferral = referralFactory.createAssigned(assignments = assignments)
      val assignedReferralSummary = referralSumariesFactory.getReferralSummary(assignedReferral)

      val result = sentReferralSummariesRepository.findAll(ReferralSpecifications.currentlyAssignedTo(user.id))
      assertThat(result)
        .usingRecursiveFieldByFieldElementComparator()
        .containsExactly(assignedReferralSummary)
    }

    @Test
    fun `does not return referral if user is previously assigned`() {
      val user = authUserFactory.create(id = "loggedInUser")
      val someOtherUser = authUserFactory.create(id = "someOtherUser")

      val assignments: List<ReferralAssignment> = listOf(
        ReferralAssignment(OffsetDateTime.now(), assignedBy = someOtherUser, assignedTo = someOtherUser),
        ReferralAssignment(OffsetDateTime.now().minusDays(1), assignedBy = someOtherUser, assignedTo = user),
      )

      referralFactory.createAssigned(assignments = assignments)
      val result = sentReferralSummariesRepository.findAll(ReferralSpecifications.currentlyAssignedTo(user.id))
      assertThat(result).isEmpty()
    }

    @Test
    fun `does not return referral if user has never been assigned`() {
      val user = authUserFactory.create(id = "loggedInUser")
      val someOtherUser = authUserFactory.create(id = "someOtherUser")

      val assignments: List<ReferralAssignment> = listOf(
        ReferralAssignment(OffsetDateTime.now(), assignedBy = someOtherUser, assignedTo = someOtherUser)
      )

      referralFactory.createAssigned(assignments = assignments)
      referralFactory.createAssigned(assignments = emptyList())

      val result = sentReferralSummariesRepository.findAll(ReferralSpecifications.currentlyAssignedTo(user.id))
      assertThat(result).isEmpty()
    }
  }
}
