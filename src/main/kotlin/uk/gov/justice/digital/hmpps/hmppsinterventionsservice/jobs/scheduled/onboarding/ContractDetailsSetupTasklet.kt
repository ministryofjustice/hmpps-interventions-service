package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jobs.scheduled.onboarding

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import mu.KLogging
import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.util.ResourceUtils
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ComplexityLevel
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ContractType
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.DesiredOutcome
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ServiceCategory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ComplexityLevelRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ContractTypeRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.DesiredOutcomeRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ServiceCategoryRepository
import java.time.OffsetDateTime
import java.util.UUID

@Component
class ContractDetailsSetupTasklet @Autowired constructor(
  val contractTypeRepository: ContractTypeRepository,
  val serviceCategoryRepository: ServiceCategoryRepository,
  val desiredOutcomeRepository: DesiredOutcomeRepository,
  val complexityLevelRepository: ComplexityLevelRepository,
) : Tasklet {
  companion object : KLogging()

  override fun execute(contribution: StepContribution, chunkContext: ChunkContext): RepeatStatus {
    logger.info("Setting up contract types, desired outcomes and complexity levels")

    val contractDetails = ResourceUtils.getFile("classpath:desiredoutcomes/contractDetails.json")
    val contractDetailsDefinitions =
      ObjectMapper().readValue(contractDetails, object : TypeReference<List<ContractDetailsDefinition>>() {})
    contractDetailsDefinitions.forEach {
      val contractType = upsertContractType(it)
      val upsertServiceCategory = upsertServiceCategory(it)
      val upsertDesiredOutcomes = upsertDesiredOutcomes(it.desiredOutcomes, upsertServiceCategory.id)
      val upsertComplexityLevels = upsertComplexityLevels(it.complexity, upsertServiceCategory.id)
      val serviceCategory = serviceCategoryRepository.save(
        ServiceCategory(
          id = upsertServiceCategory.id,
          created = upsertServiceCategory.created,
          name = upsertServiceCategory.name,
          complexityLevels = upsertComplexityLevels,
          desiredOutcomes = upsertDesiredOutcomes,
        ),
      )
      contractTypeRepository.save(
        ContractType(id = contractType.id, name = contractType.name, code = contractType.code, serviceCategories = setOf(serviceCategory)),
      )
    }

    return RepeatStatus.FINISHED
  }

  private fun upsertContractType(contractDefinition: ContractDetailsDefinition): ContractType {
    val contractType = contractTypeRepository.findByCode(contractDefinition.contractCode)
    if (contractType == null) {
      logger.info("Creating missing contract type ${contractDefinition.contractCode}")
      return contractTypeRepository.save(
        ContractType(
          id = UUID.randomUUID(),
          name = contractDefinition.contractType,
          code = contractDefinition.contractCode,
        ),
      )
    }
    return contractType
  }

  private fun upsertServiceCategory(contractDefinition: ContractDetailsDefinition): ServiceCategory {
    val serviceCategory = serviceCategoryRepository.findByName(contractDefinition.serviceCategory)
    if (serviceCategory == null) {
      logger.info("Creating missing service category ${contractDefinition.serviceCategory}")
      return serviceCategoryRepository.save(
        ServiceCategory(
          id = UUID.randomUUID(),
          created = OffsetDateTime.now(),
          name = contractDefinition.serviceCategory,
          emptyList(),
          emptyList(),
        ),
      )
    }
    return serviceCategory
  }

  private fun upsertDesiredOutcomes(desiredOutComes: List<String>, serviceCategoryId: UUID): List<DesiredOutcome> {
    val newDesiredOutcomes = mutableListOf<DesiredOutcome>()
    val existingDesiredOutcomes = desiredOutcomeRepository.findByServiceCategoryId(serviceCategoryId)
    if (existingDesiredOutcomes.isEmpty()) {
      logger.info("Creating missing desired outcome for the service category $serviceCategoryId")
      desiredOutComes.forEach {
        newDesiredOutcomes.add(
          desiredOutcomeRepository.save(
            DesiredOutcome(id = UUID.randomUUID(), description = it, serviceCategoryId = serviceCategoryId),
          ),
        )
      }
    } else {
      return existingDesiredOutcomes
    }
    return newDesiredOutcomes
  }

  private fun upsertComplexityLevels(complexities: List<Complexity>, serviceCategoryId: UUID): List<ComplexityLevel> {
    val newComplexityLevels = mutableListOf<ComplexityLevel>()
    val existingComplexityLevels = complexityLevelRepository.findByServiceCategoryId(serviceCategoryId)
    if (existingComplexityLevels.isEmpty()) {
      logger.info("Creating missing desired outcome for the service category $serviceCategoryId")
      complexities.forEach {
        newComplexityLevels.add(
          complexityLevelRepository.save(
            ComplexityLevel(
              id = UUID.randomUUID(),
              description = it.description,
              title = deriveTitle(it.severity),
              complexity = uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Complexity.valueOf(it.severity),
              serviceCategoryId = serviceCategoryId,
            ),
          ),
        )
      }
    } else {
      return existingComplexityLevels
    }
    return newComplexityLevels
  }

  private fun deriveTitle(severity: String): String {
    return when (severity) {
      "LOW" -> "Low complexity"
      "MEDIUM" -> "Medium complexity"
      "HIGH" -> "High complexity"
      else -> ""
    }
  }
}
