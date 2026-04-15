package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.controller

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.authorization.ServiceProviderAccessScopeMapper
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.authorization.UserMapper
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Intervention
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.SampleData
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.InterventionService
import java.time.OffsetDateTime
import java.util.UUID

internal class InterventionControllerTest {
  private val interventionService = mock<InterventionService>()
  private val userMapper = mock<UserMapper>()
  private val serviceProviderAccessScopeMapper = mock<ServiceProviderAccessScopeMapper>()
  private val interventionController =
    InterventionController(interventionService, userMapper, serviceProviderAccessScopeMapper)

  @Test
  fun `getInterventionByID throws 404 when intervention does not exist`() {
    val id = UUID.randomUUID()
    whenever(interventionService.getIntervention(id)).thenReturn(null)

    val exception = assertThrows<ResponseStatusException> {
      interventionController.getInterventionByID(id)
    }

    assertThat(exception.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
  }

  @Test
  fun `getInterventionByID throws 410 when intervention contract has expired`() {
    val id = UUID.randomUUID()
    val expiredContract = SampleData.sampleContract(
      primeProvider = SampleData.sampleServiceProvider(),
      npsRegion = SampleData.sampleNPSRegion(),
      referralEndAt = OffsetDateTime.now().minusDays(1),
    )
    val intervention = SampleData.sampleIntervention(id = id, dynamicFrameworkContract = expiredContract)
    whenever(interventionService.getIntervention(id)).thenReturn(intervention)

    val exception = assertThrows<ResponseStatusException> {
      interventionController.getInterventionByID(id)
    }

    assertThat(exception.statusCode).isEqualTo(HttpStatus.GONE)
  }

  @Test
  fun `getInterventionByID returns intervention when referralEndAt is null`() {
    val id = UUID.randomUUID()
    val contract = SampleData.sampleContract(
      primeProvider = SampleData.sampleServiceProvider(),
      npsRegion = SampleData.sampleNPSRegion(),
      referralEndAt = null,
    )
    val intervention = SampleData.sampleIntervention(id = id, dynamicFrameworkContract = contract)
    val pccRegions = emptyList<uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.PCCRegion>()
    whenever(interventionService.getIntervention(id)).thenReturn(intervention)
    whenever(interventionService.getPCCRegions(intervention)).thenReturn(pccRegions)

    val result = interventionController.getInterventionByID(id)

    assertThat(result.id).isEqualTo(id)
  }

  @Test
  fun `getInterventionByID returns intervention when referralEndAt is in the future`() {
    val id = UUID.randomUUID()
    val contract = SampleData.sampleContract(
      primeProvider = SampleData.sampleServiceProvider(),
      npsRegion = SampleData.sampleNPSRegion(),
      referralEndAt = OffsetDateTime.now().plusDays(1),
    )
    val intervention = SampleData.sampleIntervention(id = id, dynamicFrameworkContract = contract)
    val pccRegions = emptyList<uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.PCCRegion>()
    whenever(interventionService.getIntervention(id)).thenReturn(intervention)
    whenever(interventionService.getPCCRegions(intervention)).thenReturn(pccRegions)

    val result = interventionController.getInterventionByID(id)

    assertThat(result.id).isEqualTo(id)
  }

  @Test
  fun `getInterventions returns interventions based on filtering`() {
    val locations = emptyList<String>()
    val interventions = emptyList<Intervention>()
    whenever(interventionService.getInterventions(locations, allowsFemale = true, allowsMale = true, 18, 25)).thenReturn(interventions)

    val responseDTOs = interventionController.getInterventions(locations, allowsFemale = true, allowsMale = true, 18, 25)

    verify(interventionService).getInterventions(locations, allowsFemale = true, allowsMale = true, 18, 25)

    responseDTOs.forEachIndexed { i, it ->
      assertSame(it.id, responseDTOs[i].id)
    }
  }

  @Test
  fun `getInterventions returns interventions unfiltered when no parameters supplied`() {
    val locations: List<String>? = null
    val interventions = emptyList<Intervention>()
    whenever(interventionService.getInterventions(emptyList(), null, null, null, null)).thenReturn(interventions)

    val responseDTOs = interventionController.getInterventions(locations, null, null, null, null)

    verify(interventionService).getInterventions(emptyList(), null, null, null, null)
    responseDTOs.forEachIndexed { i, it ->
      assertSame(it.id, responseDTOs[i].id)
    }
  }
}
