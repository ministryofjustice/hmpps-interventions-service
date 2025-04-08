package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.DesiredOutcome
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.DesiredOutcomeFilterRule
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.RuleType
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ServiceCategoryRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.RepositoryTest
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ServiceCategoryFactory
import java.util.UUID

@RepositoryTest
class ServiceCategoryServiceTest @Autowired constructor(
  val entityManager: TestEntityManager,
  val serviceCategoryRepository: ServiceCategoryRepository,
) {
  private val serviceCategoryService = ServiceCategoryService(serviceCategoryRepository)
  private val serviceCategoryFactory = ServiceCategoryFactory(entityManager)

  @Test
  fun `get non-existent service category returns null`() {
    assertThat(serviceCategoryService.getServiceCategoryByID(UUID.randomUUID())).isNull()
  }

  @Test
  fun `get valid service category`() {
    val idToFind = UUID.randomUUID()

    val desiredOutcomes = listOf(
      DesiredOutcome(id = UUID.randomUUID(), "Outcome 1", idToFind, deprecatedAt = null, mutableSetOf()),
      DesiredOutcome(id = UUID.randomUUID(), "Outcome 2", idToFind, deprecatedAt = null, mutableSetOf()),
      DesiredOutcome(id = UUID.randomUUID(), "Outcome 3", idToFind, deprecatedAt = null, mutableSetOf()),
    )

    serviceCategoryFactory.create(id = idToFind, desiredOutcomes = desiredOutcomes)
    serviceCategoryFactory.create(id = UUID.randomUUID())

    val serviceCategory = serviceCategoryService.getServiceCategoryByID(idToFind)
    assertThat(serviceCategory).isNotNull
    assertThat(serviceCategory!!.desiredOutcomes.count()).isEqualTo(3)
  }

  @Test
  fun `filter service categories`() {
    val serviceCategoryId = UUID.randomUUID()
    val contractReference = "abc123"

    val desiredOutcomeId1 = UUID.randomUUID()
    val desiredOutcomeId2 = UUID.randomUUID()
    val desiredOutcomeId3 = UUID.randomUUID()

    val filterRule = DesiredOutcomeFilterRule(UUID.randomUUID(), desiredOutcomeId1, RuleType.EXCLUDE, "contract", mutableListOf(contractReference))

    val desiredOutcomes = listOf(
      DesiredOutcome(id = desiredOutcomeId1, "Outcome 1", serviceCategoryId, deprecatedAt = null, mutableSetOf(filterRule)),
      DesiredOutcome(id = desiredOutcomeId2, "Outcome 2", serviceCategoryId, deprecatedAt = null, mutableSetOf()),
      DesiredOutcome(id = desiredOutcomeId3, "Outcome 3", serviceCategoryId, deprecatedAt = null, mutableSetOf()),
    )

    serviceCategoryFactory.create(id = serviceCategoryId, desiredOutcomes = desiredOutcomes)
    serviceCategoryFactory.create(id = UUID.randomUUID())

    val result = serviceCategoryService.getServiceCategoryByIDAndContractReference(serviceCategoryId, contractReference)
    assertThat(result).isNotNull
    assertThat(result!!.id).isEqualTo(serviceCategoryId)
    assertThat(result.desiredOutcomes.count()).isEqualTo(2)
    result.desiredOutcomes.forEach { assertThat(it.id !== desiredOutcomeId1) }
  }
}
