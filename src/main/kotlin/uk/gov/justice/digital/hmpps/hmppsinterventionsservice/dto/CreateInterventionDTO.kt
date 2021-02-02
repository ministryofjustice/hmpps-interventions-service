package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto

import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Intervention
import java.time.format.DateTimeFormatter

// This needs to change to only allow nullable fields on pccRegion and npsRegion
// It currently uses opencsv which needs the fields to be nullable (is there a flag to set to avoid this?)
// Alternatively - read using supercsv or parse each line
data class CreateInterventionDTO(
  val title: String? = null,
  val description: String? = null,

  val serviceCategoryId: String? = null,
  val serviceProviderId: String? = null,
  val npsRegionId: Char? = null,
  val pccRegionId: String? = null,

  val startDate: String? = null,
  val endDate: String? = null,

  val minimumAge: Int? = null,
  val maximumAge: Int? = null,

  val allowsFemale: Boolean? = null,
  val allowsMale: Boolean? = null,
) {
  companion object {
    fun from(intervention: Intervention): CreateInterventionDTO {
      return CreateInterventionDTO(
        title = intervention.title,
        description = intervention.description,
        serviceCategoryId = intervention.dynamicFrameworkContract.serviceCategory.id.toString(),
        serviceProviderId = intervention.dynamicFrameworkContract.serviceProvider.id,
        npsRegionId = if (intervention.dynamicFrameworkContract.npsRegion == null) null else intervention.dynamicFrameworkContract.npsRegion.id,
        pccRegionId = if (intervention.dynamicFrameworkContract.pccRegion == null) null else intervention.dynamicFrameworkContract.pccRegion.id,
        startDate = intervention.dynamicFrameworkContract.startDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
        endDate = intervention.dynamicFrameworkContract.endDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
        minimumAge = intervention.dynamicFrameworkContract.contractEligibility.minimumAge,
        maximumAge = intervention.dynamicFrameworkContract.contractEligibility.maximumAge,

        allowsFemale = intervention.dynamicFrameworkContract.contractEligibility.allowsFemale,
        allowsMale = intervention.dynamicFrameworkContract.contractEligibility.allowsMale,
      )
    }
  }
}
