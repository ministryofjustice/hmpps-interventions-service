package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service

import com.opencsv.bean.CsvToBean
import com.opencsv.bean.CsvToBeanBuilder
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.config.Code
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.config.FieldError
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.config.ValidationError
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.CreateInterventionDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ContractEligibility
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.DynamicFrameworkContract
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Intervention
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.NPSRegion
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.PCCRegion
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.DynamicFrameworkContractRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.NPSRegionRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.PCCRegionRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ServiceCategoryRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ServiceProviderRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.utils.StringToUUIDConverter
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Service
class InterventionCsvService(
  val interventionService: InterventionService,
  val serviceCategoryRepository: ServiceCategoryRepository,
  val serviceProviderRepository: ServiceProviderRepository,
  val npsRegionRepository: NPSRegionRepository,
  val pccRegionRepository: PCCRegionRepository,
  val dynamicFrameworkContractRepository: DynamicFrameworkContractRepository,
) {
  fun uploadCsvFile(file: MultipartFile): List<CreateInterventionDTO> {
    var fileReader: BufferedReader? = null
    try {
      ensureFileIsNotEmpty(file)
      fileReader = BufferedReader(InputStreamReader(file.inputStream))
      val csvToBean = createCSVToBean(fileReader)
      val dtoList = csvToBean.parse()
      if (dtoList.isEmpty()) throw ValidationError("No interventions found", listOf(FieldError(field = "file", error = Code.FILE_UPLOAD_ERROR)))

      return validateAndSave(dtoList)
    } catch (ex: ValidationError) {
      // rethrow any expected errors
      throw ex
    } catch (ex: Exception) {
      throw ValidationError("Error during csv import $ex.message", listOf(FieldError(field = "file", error = Code.FILE_UPLOAD_ERROR)))
    } finally {
      closeFileReader(fileReader)
    }
  }

  private fun validateAndSave(interventionDTOs: List<CreateInterventionDTO>): List<CreateInterventionDTO> {
    val errors = mutableListOf<FieldError>()
    val interventionList = mutableListOf<Intervention>()

    interventionDTOs.forEach { dto ->

      val serviceCategory = serviceCategoryRepository.findByIdOrNull(StringToUUIDConverter.parseID(dto.serviceCategoryId!!))
      serviceCategory.let {
        errors.add(FieldError(field = "serviceCategory", error = Code.DOES_NOT_EXIST))
      }
      val serviceProvider = serviceProviderRepository.findById(dto.serviceProviderId!!)
      serviceProvider.let {
        errors.add(FieldError(field = "serviceProvider", error = Code.DOES_NOT_EXIST))
      }

      var npsRegion: NPSRegion? = null
      dto.npsRegionId?.let {
        npsRegion = npsRegionRepository.findByIdOrNull(dto.npsRegionId)
        npsRegion.let {
          errors.add(FieldError(field = "npsRegion", error = Code.DOES_NOT_EXIST))
        }
      }

      var pccRegion: PCCRegion? = null
      dto.pccRegionId?.let {
        pccRegion = pccRegionRepository.findByIdOrNull(dto.pccRegionId)
        pccRegion.let {
          errors.add(FieldError(field = "pccRegion", error = Code.DOES_NOT_EXIST))
        }
      }

      // Add the contract object
      // Move this knowledge to Intervention service?
      val dynamicFrameworkContract = DynamicFrameworkContract(
        serviceCategory = serviceCategory!!,
        serviceProvider = serviceProvider!!,
        npsRegion = npsRegion,
        pccRegion = pccRegion,
        startDate = LocalDate.parse(dto.startDate!!, DateTimeFormatter.ISO_LOCAL_DATE),
        endDate = LocalDate.parse(dto.endDate!!, DateTimeFormatter.ISO_LOCAL_DATE),
        contractEligibility = ContractEligibility(
          allowsFemale = dto.allowsFemale!!,
          allowsMale = dto.allowsMale!!,
          minimumAge = dto.minimumAge!!,
          maximumAge = dto.maximumAge!!
        )
      )
      val savedDynamicFrameworkContract = dynamicFrameworkContractRepository.save(dynamicFrameworkContract)

      interventionList.add(
        Intervention(
          title = dto.title!!,
          description = dto.description!!,
          dynamicFrameworkContract = savedDynamicFrameworkContract
        )
      )
    }

    return interventionService.createInterventions(interventionList).map { CreateInterventionDTO.from(it) }
  }

  private fun createCSVToBean(fileReader: BufferedReader?): CsvToBean<CreateInterventionDTO> =
    CsvToBeanBuilder<CreateInterventionDTO>(fileReader)
      .withType(CreateInterventionDTO::class.java)
      .withIgnoreLeadingWhiteSpace(true)
      .build()

  private fun ensureFileIsNotEmpty(file: MultipartFile) {
    if (file.isEmpty) {
      val errors = listOf<FieldError>(FieldError(field = "csv file", error = Code.CANNOT_BE_EMPTY))
      throw ValidationError("Error during csv import", errors)
    }
  }

  private fun closeFileReader(fileReader: BufferedReader?) {
    try {
      fileReader!!.close()
    } catch (ex: IOException) {
      val errors = listOf<FieldError>(FieldError(field = "csv file", error = Code.FILE_UPLOAD_ERROR))
      throw ValidationError("Error during csv import", errors)
    }
  }
}
