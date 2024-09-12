package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.specification

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
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
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.AppointmentFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.AssignmentsFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.AuthUserFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.DynamicFrameworkContractFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.EndOfServiceReportFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.InterventionFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ReferralFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.RepositoryTest
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.SentReferralSummariesFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.SupplierAssessmentFactory
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
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
  private val assignmentsFactory = AssignmentsFactory(entityManager)
  private val appointmentFactory = AppointmentFactory(entityManager)
  private val supplierAssessmentFactory = SupplierAssessmentFactory(entityManager)
  private val recursiveComparisonConfigurationBuilder = RecursiveComparisonConfiguration.builder()
  private lateinit var recursiveComparisonConfiguration: RecursiveComparisonConfiguration

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
    val truncateSeconds: Comparator<OffsetDateTime> = Comparator { a, exp ->
      if (exp != null && a != null) {
        if (a
            .truncatedTo(ChronoUnit.SECONDS)
            .isEqual(exp.truncatedTo(ChronoUnit.SECONDS))
        ) {
          0
        } else {
          1
        }
      } else {
        0
      }
    }
    recursiveComparisonConfiguration = recursiveComparisonConfigurationBuilder
      .withComparatorForType(truncateSeconds, OffsetDateTime::class.java)
      .build()
  }

  @Nested
  inner class WithSPAccess {
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
      referralFactory.createSent(intervention = someOtherIntervention)
      val someOtherReferralSummaryWithSpContract = referralSumariesFactory.getReferralSummary(referralWithSpContract)

      val result = sentReferralSummariesRepository.findAll(ReferralSpecifications.withSPAccess(setOf(spContract, unrelatedSpContract)))
      assertThat(result)
        .usingRecursiveFieldByFieldElementComparator(recursiveComparisonConfiguration)
        .containsExactly(referralSummaryWithSpContract)
      assertThat(result).doesNotContain(someOtherReferralSummaryWithSpContract)
    }
  }
}
