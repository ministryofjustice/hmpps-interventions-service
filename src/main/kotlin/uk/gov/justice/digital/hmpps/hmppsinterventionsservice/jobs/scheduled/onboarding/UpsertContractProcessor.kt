package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jobs.scheduled.onboarding

import mu.KLogging
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.item.ItemProcessor
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.DynamicFrameworkContract
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Intervention
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ContractTypeRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.DynamicFrameworkContractRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.InterventionRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.NPSRegionRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.PCCRegionRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ServiceProviderRepository
import java.time.LocalDate
import java.time.OffsetDateTime
import kotlin.jvm.optionals.getOrElse

private const val DEFAULT_REFERRAL_EMAIL = "interventions-referrals-fallback@digital.justice.gov.uk"
private const val DEFAULT_DESCRIPTION = ""
private val defaultEndDate = LocalDate.of(2100, 12, 31)

@Component
@JobScope
class UpsertContractProcessor(
  val dynamicFrameworkContractRepository: DynamicFrameworkContractRepository,
  val interventionRepository: InterventionRepository,
  val providerRepository: ServiceProviderRepository,
  val contractTypeRepository: ContractTypeRepository,
  val npsRegionRepository: NPSRegionRepository,
  val pccRegionRepository: PCCRegionRepository,
) : ItemProcessor<ContractDefinition, Intervention> {
  companion object : KLogging()

  override fun process(item: ContractDefinition): Intervention? {
    logger.info("Processing ${item.contractReference}")

    val errors = verifyReferenceDataExists(item.contractTypeCode, item.region, item.providers)
    if (errors.isNotEmpty()) {
      logger.error(
        "${item.contractReference} is missing reference data; " +
          "please add these in a migration before this task: {}",
        errors,
      )
      return null
    }
    return upsertIntervention(upsertContract(item), item).also {
      logger.info("${item.contractReference} completed")
    }
  }

  private fun verifyReferenceDataExists(
    contractTypeCode: String,
    region: RegionDefinition,
    providers: ProviderDefinition,
  ): List<String> {
    return mutableListOf<String>().also {
      it.addAll(validateContractType(contractTypeCode))
      it.addAll(validateRegions(region))
      it.addAll(validateProviders(providers))
    }
  }

  private fun validateContractType(contractTypeCode: String): List<String> {
    return if (contractTypeRepository.findByCode(contractTypeCode) == null) {
      listOf("Cannot find $contractTypeCode contract type")
    } else {
      listOf()
    }
  }

  private fun validateProviders(providers: ProviderDefinition): List<String> {
    val errors = mutableListOf<String>()

    providerRepository.findById(providers.primeProviderId)
      .getOrElse { errors.add("Cannot find ${providers.primeProviderId} provider") }

    val subcontractors = providerRepository.findAllById(providers.subcontractorIds).toSet()
    if (subcontractors.size != providers.subcontractorIds.size) {
      val missing = providers.subcontractorIds.toSet() - subcontractors.map { it.id }.toSet()
      errors.add("Cannot resolve $missing subcontractor providers")
    }

    return errors
  }

  private fun validateRegions(region: RegionDefinition): List<String> {
    val errors = mutableListOf<String>()

    if (region.npsCode != null) {
      npsRegionRepository.findById(region.npsCode)
        .getOrElse { errors.add("Cannot find ${region.npsCode} NPS region") }
    }
    if (region.pccCode != null) {
      pccRegionRepository.findById(region.pccCode)
        .getOrElse { errors.add("Cannot find ${region.pccCode} PCC region") }
    }
    if (region.npsCode == null && region.pccCode == null) {
      errors.add("Either NPS or PCC code must exist")
    }
    if (region.npsCode != null && region.pccCode != null) {
      errors.add("NPS and PCC region codes are mutually exclusive, only one should exist")
    }

    return errors
  }

  private fun upsertContract(item: ContractDefinition): DynamicFrameworkContract {
    val contractType = contractTypeRepository.findByCode(item.contractTypeCode)!!
    val prime = providerRepository.findById(item.providers.primeProviderId).get()
    val subcontractors = providerRepository.findAllById(item.providers.subcontractorIds).toMutableSet()
    val pccRegion = if (item.region.pccCode != null) pccRegionRepository.findById(item.region.pccCode).get() else null
    val npsRegion = if (item.region.npsCode != null) npsRegionRepository.findById(item.region.npsCode).get() else null

    val definedContract = DynamicFrameworkContract(
      id = item.internalContractId,
      contractReference = item.contractReference,
      contractType = contractType,
      minimumAge = item.eligibility.minimumAge,
      maximumAge = item.eligibility.maximumAge,
      allowsFemale = item.eligibility.allowsFemale,
      allowsMale = item.eligibility.allowsMale,
      startDate = item.scheduling.hideBefore,
      endDate = item.scheduling.hideAfter?.toLocalDate() ?: defaultEndDate,
      referralEndAt = item.scheduling.hideAfter,
      referralStartDate = item.scheduling.hideBefore,
      primeProvider = prime,
      subcontractorProviders = subcontractors,
      pccRegion = pccRegion,
      npsRegion = npsRegion,
    )

    val foundContract = dynamicFrameworkContractRepository.findByContractReference(item.contractReference)
    if (foundContract == null) {
      logger.info("Creating missing contract ${item.contractReference} with ID ${item.internalContractId}")
      return dynamicFrameworkContractRepository.save(definedContract)
    } else {
      // allowable overrides
      foundContract.referralEndAt = item.scheduling.hideAfter
      foundContract.referralStartDate = item.scheduling.hideBefore
      foundContract.subcontractorProviders.addAll(subcontractors)
      foundContract.subcontractorProviders.retainAll(subcontractors)
    }

    return dynamicFrameworkContractRepository.save(foundContract)
  }

  private fun upsertIntervention(contract: DynamicFrameworkContract, item: ContractDefinition): Intervention {
    val description = descriptionWithActivities(
      item.shortDescription ?: DEFAULT_DESCRIPTION,
      item.activities ?: listOf(),
    )

    val definedIntervention = Intervention(
      id = item.internalInterventionId,
      title = item.interventionTitle,
      description = description,
      incomingReferralDistributionEmail = item.incomingDistributionEmail ?: DEFAULT_REFERRAL_EMAIL,
      dynamicFrameworkContract = contract,
      createdAt = OffsetDateTime.now(),
    )

    val foundIntervention = interventionRepository.findByDynamicFrameworkContractContractReference(item.contractReference)
    if (foundIntervention == null) {
      logger.info("Creating missing intervention for contract ${item.contractReference} with ID ${item.internalInterventionId}")
      return interventionRepository.save(definedIntervention)
    } else {
      foundIntervention.title = item.interventionTitle
      foundIntervention.description = description
    }
    return interventionRepository.save(foundIntervention)
  }

  private fun descriptionWithActivities(description: String, activities: List<ActivityDefinition>): String {
    val buf = StringBuilder(description)
    buf.append("\n\nActivities:")

    activities.groupBy {
      mapOf(
        "area" to it.needArea,
        "location" to it.deliveryLocation,
        "method" to it.deliveryMethod,
      )
    }.forEach { (key, matchingActivities) ->
      buf.append("\n\n${key["area"]}")
      buf.append("\n${key["method"]}; ${key["location"]}")
      matchingActivities.map { it.activity }.sorted().distinct().forEach {
        buf.append("\n• $it")
      }
    }
    return buf.toString().trim()
  }
}
