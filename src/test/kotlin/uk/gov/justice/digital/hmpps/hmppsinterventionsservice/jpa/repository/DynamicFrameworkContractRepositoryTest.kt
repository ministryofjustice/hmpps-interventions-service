package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ActionPlan
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Appointment
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AuthUser
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.DeliverySession
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.DynamicFrameworkContract
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.EndOfServiceReport
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Intervention
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Referral
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.SupplierAssessment
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.DynamicFrameworkContractFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.RepositoryTest
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ServiceProviderFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.deleteAll

@RepositoryTest
class DynamicFrameworkContractRepositoryTest @Autowired constructor(
  val entityManager: TestEntityManager,
  val actionPlanRepository: ActionPlanRepository,
  val deliverySessionRepository: DeliverySessionRepository,
  val interventionRepository: InterventionRepository,
  val referralRepository: ReferralRepository,
  val authUserRepository: AuthUserRepository,
  val appointmentRepository: AppointmentRepository,
  val supplierAssessmentRepository: SupplierAssessmentRepository,
  val endOfServiceReportRepository: EndOfServiceReportRepository,
  val dynamicFrameworkContractRepository: DynamicFrameworkContractRepository,
) {
  private val dynamicFrameworkContractFactory = DynamicFrameworkContractFactory(entityManager)
  private val serviceProviderFactory = ServiceProviderFactory(entityManager)

  @BeforeEach
  fun setup() {
    entityManager.deleteAll(
      DeliverySession::class,
      SupplierAssessment::class,
      Appointment::class,
      ActionPlan::class,
      EndOfServiceReport::class,
      Referral::class,
      Intervention::class,
      DynamicFrameworkContract::class,
      AuthUser::class,
    )
  }

  @Test
  fun `can store and retrieve a contract with a subcontractor`() {
    val serviceProvider = serviceProviderFactory.create()
    val contract = dynamicFrameworkContractFactory.create(subcontractorProviders = mutableSetOf(serviceProvider))

    val savedContract = dynamicFrameworkContractRepository.findById(contract.id).get()
    assertThat(savedContract.id).isEqualTo(contract.id)
    assertThat(savedContract.subcontractorProviders.size).isEqualTo(1)
  }
}
