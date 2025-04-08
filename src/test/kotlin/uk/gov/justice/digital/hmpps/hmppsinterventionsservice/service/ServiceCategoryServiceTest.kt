package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.DesiredOutcome
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.DesiredOutcomeFilterRule
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.RuleType
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ServiceCategoryRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.RepositoryTest
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ServiceCategoryFactory
import java.time.OffsetDateTime
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

  @Nested
  @DisplayName("Get service category by ID And contract reference")
  inner class GetServiceCategoryByIDAndContractReference {

    val serviceCategoryId: UUID = UUID.randomUUID()
    val contractReference = "abc123"

    private val desiredOutcomeId1 = UUID.randomUUID()
    private val desiredOutcomeId2 = UUID.randomUUID()
    private val desiredOutcomeId3 = UUID.randomUUID()

    @Test
    fun `get non-existent service category returns null`() {
      assertThat(serviceCategoryService.getServiceCategoryByIDAndContractReference(UUID.randomUUID(), "AAA")).isNull()
    }

    @Test
    fun `do not return desired outcomes as part of service category if there is an exclusion rule`() {
      val filterRule1 = DesiredOutcomeFilterRule(UUID.randomUUID(), desiredOutcomeId1, RuleType.EXCLUDE, "contract", mutableListOf(contractReference))
      val filterRule2 = DesiredOutcomeFilterRule(UUID.randomUUID(), desiredOutcomeId2, RuleType.EXCLUDE, "contract", mutableListOf(contractReference))

      val desiredOutcomes = listOf(
        DesiredOutcome(id = desiredOutcomeId1, "Outcome 1", serviceCategoryId, deprecatedAt = null, mutableSetOf(filterRule1)),
        DesiredOutcome(id = desiredOutcomeId2, "Outcome 2", serviceCategoryId, deprecatedAt = null, mutableSetOf(filterRule2)),
        DesiredOutcome(id = desiredOutcomeId3, "Outcome 3", serviceCategoryId, deprecatedAt = null, mutableSetOf()),
      )

      serviceCategoryFactory.create(id = serviceCategoryId, desiredOutcomes = desiredOutcomes)
      serviceCategoryFactory.create(id = UUID.randomUUID())

      val result = serviceCategoryService.getServiceCategoryByIDAndContractReference(serviceCategoryId, contractReference)
      assertThat(result).isNotNull
      assertThat(result!!.id).isEqualTo(serviceCategoryId)
      assertThat(result.desiredOutcomes.count()).isEqualTo(1)
      result.desiredOutcomes.forEach { assertThat(it.id !== desiredOutcomeId1 && it.id == desiredOutcomeId2) }
    }

    @Test
    fun `do not return desired outcomes as part of service category if it isn't specified in INCLUSION rule`() {
      val filterRule = DesiredOutcomeFilterRule(UUID.randomUUID(), desiredOutcomeId1, RuleType.INCLUDE, "contract", mutableListOf(contractReference))

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
      assertThat(result.desiredOutcomes.count()).isEqualTo(3)
      assertThat(result.desiredOutcomes.any { it.id == desiredOutcomeId1 })
    }

    @Test
    fun `deprecated desired outcomes are not returned as part of service category`() {
      val desiredOutcomes = listOf(
        DesiredOutcome(id = desiredOutcomeId1, "Outcome 1", serviceCategoryId, deprecatedAt = OffsetDateTime.now(), mutableSetOf()),
        DesiredOutcome(id = desiredOutcomeId2, "Outcome 2", serviceCategoryId, deprecatedAt = null, mutableSetOf()),
        DesiredOutcome(id = desiredOutcomeId3, "Outcome 3", serviceCategoryId, deprecatedAt = OffsetDateTime.now(), mutableSetOf()),
      )

      serviceCategoryFactory.create(id = serviceCategoryId, desiredOutcomes = desiredOutcomes)
      serviceCategoryFactory.create(id = UUID.randomUUID())

      val result = serviceCategoryService.getServiceCategoryByIDAndContractReference(serviceCategoryId, contractReference)
      assertThat(result).isNotNull
      assertThat(result!!.id).isEqualTo(serviceCategoryId)
      assertThat(result.desiredOutcomes.count()).isEqualTo(1)
      assertThat(result.desiredOutcomes[0].id).isEqualTo(desiredOutcomeId2)
    }

    @Test
    fun `return only valid desired outcomes when there a deprecated desired outcomes and filter rules`() {
      val filterRule = DesiredOutcomeFilterRule(UUID.randomUUID(), desiredOutcomeId3, RuleType.EXCLUDE, "contract", mutableListOf(contractReference))

      val desiredOutcomes = listOf(
        DesiredOutcome(id = desiredOutcomeId1, "Outcome 1", serviceCategoryId, deprecatedAt = OffsetDateTime.now(), mutableSetOf()),
        DesiredOutcome(id = desiredOutcomeId2, "Outcome 2", serviceCategoryId, deprecatedAt = null, mutableSetOf()),
        DesiredOutcome(id = desiredOutcomeId3, "Outcome 3", serviceCategoryId, deprecatedAt = null, mutableSetOf(filterRule)),
      )

      serviceCategoryFactory.create(id = serviceCategoryId, desiredOutcomes = desiredOutcomes)
      serviceCategoryFactory.create(id = UUID.randomUUID())

      val result = serviceCategoryService.getServiceCategoryByIDAndContractReference(serviceCategoryId, contractReference)
      assertThat(result).isNotNull
      assertThat(result!!.id).isEqualTo(serviceCategoryId)
      assertThat(result.desiredOutcomes.count()).isEqualTo(1)
      assertThat(result.desiredOutcomes[0].id).isEqualTo(desiredOutcomeId2)
    }

    @Test
    fun `Do not exclude desired outcomes that do not match the match-data on an exclusion rule`() {
      val filterRule = DesiredOutcomeFilterRule(UUID.randomUUID(), desiredOutcomeId3, RuleType.EXCLUDE, "contract", mutableListOf(contractReference))

      val desiredOutcomes = listOf(
        DesiredOutcome(id = desiredOutcomeId1, "Outcome 1", serviceCategoryId, deprecatedAt = null, mutableSetOf()),
        DesiredOutcome(id = desiredOutcomeId2, "Outcome 2", serviceCategoryId, deprecatedAt = null, mutableSetOf()),
        DesiredOutcome(id = desiredOutcomeId3, "Outcome 3", serviceCategoryId, deprecatedAt = null, mutableSetOf(filterRule)),
      )

      serviceCategoryFactory.create(id = serviceCategoryId, desiredOutcomes = desiredOutcomes)
      serviceCategoryFactory.create(id = UUID.randomUUID())

      val result = serviceCategoryService.getServiceCategoryByIDAndContractReference(serviceCategoryId, "REFERENCE-01")
      assertThat(result).isNotNull
      assertThat(result!!.id).isEqualTo(serviceCategoryId)
      assertThat(result.desiredOutcomes.count()).isEqualTo(3)
    }
  }
}
