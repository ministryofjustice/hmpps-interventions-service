package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service
import org.apache.commons.io.IOUtils
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.config.Code
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.config.ValidationError
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.InterventionHelper
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ServiceCategory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ServiceProvider
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.DynamicFrameworkContractRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.InterventionRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.NPSRegionRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.PCCRegionRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ServiceCategoryRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ServiceProviderRepository
import java.time.OffsetDateTime
import java.util.UUID

@DataJpaTest
@ActiveProfiles("jpa-test")
@Disabled
class InterventionCsvServiceTest @Autowired constructor(
  val entityManager: TestEntityManager,
  val interventionRepository: InterventionRepository,
  val serviceProviderRepository: ServiceProviderRepository,
  val serviceCategoryRepository: ServiceCategoryRepository,
  val dynamicFrameworkContractRepository: DynamicFrameworkContractRepository,
  val npsRegionRepository: NPSRegionRepository,
  val pccRegionRepository: PCCRegionRepository,
) {

  private val interventionService = InterventionService(interventionRepository)
  private val interventionCsvService = InterventionCsvService(interventionService, serviceCategoryRepository,
    serviceProviderRepository,npsRegionRepository, pccRegionRepository, dynamicFrameworkContractRepository)

  @Test
  fun `create and persist single intervention from a csv file`() {
    val serviceCategory = InterventionHelper.sampleServiceCategory(desiredOutcomes= emptyList())
    entityManager.persist(serviceCategory)

    val serviceProvider = InterventionHelper.sampleServiceProvider()
    entityManager.persist(serviceProvider)
    entityManager.flush()

    val headers = "title, description,npsRegionId,pccRegionId,serviceCategoryId,serviceProviderId,startDate,endDate,minimumAge, maximumAge,allowsFemale, allowsMale"
    val body = "Harmony Living Help,Helping others find accommodation,G,,428ee70f-3001-4399-95a6-ad25eaaede16,HARMONY_LIVING,2020-11-02,2023-12-15,18,25,true,false"

    val uploads = interventionCsvService.uploadCsvFile(
      MockMultipartFile(
        "single",
        IOUtils.toInputStream(
          headers + IOUtils.LINE_SEPARATOR + body,
          "UTF-8"
        )
      )
    )
    Assertions.assertThat(uploads.size).isEqualTo(1)
    Assertions.assertThat(uploads[0].title).isEqualTo("Harmony Living Help")
    Assertions.assertThat(uploads[0].description).isEqualTo("Harmony Living Help")
  }

  @Test
  fun `empty file fails gracefully`() {
    val emptyData = ByteArray(0)
    val error = assertThrows<ValidationError> {
      // ValidationError expected
      interventionCsvService.uploadCsvFile(MockMultipartFile("emptyFile", emptyData))
    }
    Assertions.assertThat(error.errors.size).isEqualTo(1)
    Assertions.assertThat(error.errors[0].field).isEqualTo("csv file")
    Assertions.assertThat(error.errors[0].error).isEqualTo(Code.CANNOT_BE_EMPTY)
  }

  @Test
  fun `missing columns fails gracefully`() {
    val headers = "title"
    val body = "Accommodation Service"

    val error = assertThrows<ValidationError> {
      // ValidationError expected
      interventionCsvService.uploadCsvFile(MockMultipartFile("missing", IOUtils.toInputStream(headers + IOUtils.LINE_SEPARATOR + body, "UTF-8")))
    }
    Assertions.assertThat(error.errors.size).isEqualTo(1)
  }

  @Test
  fun `empty data fails gracefully`() {
    val error = assertThrows<ValidationError> {
      // ValidationError expected
      val uploads =
        interventionCsvService.uploadCsvFile(MockMultipartFile("single", IOUtils.toInputStream("title", "UTF-8")))
    }
    Assertions.assertThat(error.message).isEqualTo("No interventions found")
  }
}
