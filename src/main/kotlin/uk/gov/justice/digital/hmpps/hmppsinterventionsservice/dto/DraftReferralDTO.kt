package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto

import com.fasterxml.jackson.annotation.JsonView
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.DraftReferral
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Referral
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

object Views {
  interface DraftReferral
  interface SentReferral
}

data class ReferralComplexityLevel(
  val serviceCategoryId: UUID,
  val complexityLevelId: UUID,
)

@JsonView(Views.DraftReferral::class, Views.SentReferral::class)
data class DraftReferralDTO(
  val id: UUID? = null,
  val createdAt: OffsetDateTime? = null,
  val completionDeadline: LocalDate? = null,
  val serviceCategoryIds: List<UUID>? = null,
  val complexityLevels: List<ReferralComplexityLevel>? = null,
  val furtherInformation: String? = null,
  val additionalNeedsInformation: String? = null,
  val accessibilityNeeds: String? = null,
  val needsInterpreter: Boolean? = null,
  val interpreterLanguage: String? = null,
  val hasAdditionalResponsibilities: Boolean? = null,
  val whenUnavailable: String? = null,

  // the risk information is a special field; it's set on the draft referral,
  // but as soon as the referral is sent we store the risk information in
  // 'assess risks and needs' and no longer store it in our database.
  // it is not sent with SentReferralDTO, which nests this DTO.
  // NOTE: it is the responsibility of the controller methods to set the
  // JsonView when returning SentReferralDTOs!
  @JsonView(Views.DraftReferral::class) val additionalRiskInformation: String? = null,

  val maximumEnforceableDays: Int? = null,
  val desiredOutcomes: List<SelectedDesiredOutcomesDTO>? = null,
  val serviceUser: ServiceUserDTO? = null,
  val serviceProvider: ServiceProviderDTO? = null,
  val relevantSentenceId: Long? = null,
  val interventionId: UUID? = null,
  val contractTypeName: String? = null,
) {
  companion object {
    fun from(referral: DraftReferral): DraftReferralDTO {
      val contract = referral.intervention.dynamicFrameworkContract
      return DraftReferralDTO(
        id = referral.id,
        createdAt = referral.createdAt,
        completionDeadline = referral.referralDetails?.completionDeadline,
        complexityLevels = referral.complexityLevelIds?.ifEmpty { null }
          ?.map { ReferralComplexityLevel(it.key, it.value) }
          ?.sortedBy { it.serviceCategoryId },
        furtherInformation = referral.referralDetails?.furtherInformation,
        additionalNeedsInformation = referral.additionalNeedsInformation,
        accessibilityNeeds = referral.accessibilityNeeds,
        needsInterpreter = referral.needsInterpreter,
        interpreterLanguage = referral.interpreterLanguage,
        hasAdditionalResponsibilities = referral.hasAdditionalResponsibilities,
        whenUnavailable = referral.whenUnavailable,
        additionalRiskInformation = referral.additionalRiskInformation,
        maximumEnforceableDays = referral.referralDetails?.maximumEnforceableDays,
        desiredOutcomes = referral.selectedDesiredOutcomes?.ifEmpty { null }
          ?.groupBy { it.serviceCategoryId }
          ?.toSortedMap()
          ?.map { (serviceCategoryId, desiredOutcomes) ->
            SelectedDesiredOutcomesDTO(
              serviceCategoryId,
              desiredOutcomes.map { it.desiredOutcomeId }.sorted()
            )
          },
        serviceUser = ServiceUserDTO.from(referral.serviceUserCRN, referral.serviceUserData),
        serviceProvider = ServiceProviderDTO.from(contract.primeProvider),
        relevantSentenceId = referral.relevantSentenceId,
        serviceCategoryIds = referral.selectedServiceCategories?.ifEmpty { null }
          ?.map { it.id }
          ?.sorted(),
        interventionId = referral.intervention.id,
        contractTypeName = referral.intervention.dynamicFrameworkContract.contractType.name,
      )
    }
    @Deprecated("deprecated as we will be using from(referral: DraftReferral) in the future")
    fun from(referral: Referral): DraftReferralDTO {
      val contract = referral.intervention.dynamicFrameworkContract
      return DraftReferralDTO(
        id = referral.id,
        createdAt = referral.createdAt,
        completionDeadline = referral.referralDetails?.completionDeadline,
        complexityLevels = referral.complexityLevelIds?.ifEmpty { null }
          ?.map { ReferralComplexityLevel(it.key, it.value) }
          ?.sortedBy { it.serviceCategoryId },
        furtherInformation = referral.referralDetails?.furtherInformation,
        additionalNeedsInformation = referral.additionalNeedsInformation,
        accessibilityNeeds = referral.accessibilityNeeds,
        needsInterpreter = referral.needsInterpreter,
        interpreterLanguage = referral.interpreterLanguage,
        hasAdditionalResponsibilities = referral.hasAdditionalResponsibilities,
        whenUnavailable = referral.whenUnavailable,
        additionalRiskInformation = referral.additionalRiskInformation,
        maximumEnforceableDays = referral.referralDetails?.maximumEnforceableDays,
        desiredOutcomes = referral.selectedDesiredOutcomes?.ifEmpty { null }
          ?.groupBy { it.serviceCategoryId }
          ?.toSortedMap()
          ?.map { (serviceCategoryId, desiredOutcomes) ->
            SelectedDesiredOutcomesDTO(
              serviceCategoryId,
              desiredOutcomes.map { it.desiredOutcomeId }.sorted()
            )
          },
        serviceUser = ServiceUserDTO.from(referral.serviceUserCRN, referral.serviceUserData),
        serviceProvider = ServiceProviderDTO.from(contract.primeProvider),
        relevantSentenceId = referral.relevantSentenceId,
        serviceCategoryIds = referral.selectedServiceCategories?.ifEmpty { null }
          ?.map { it.id }
          ?.sorted(),
        interventionId = referral.intervention.id,
        contractTypeName = referral.intervention.dynamicFrameworkContract.contractType.name,
      )
    }
  }
}
