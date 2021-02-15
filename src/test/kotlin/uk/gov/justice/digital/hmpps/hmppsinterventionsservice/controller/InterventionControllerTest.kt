package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.controller

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.InterventionDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.InterventionService

internal class InterventionControllerTest {
  private val interventionService = mock<InterventionService>()
  private val interventionController = InterventionController(interventionService)

  @Test
  fun `getInterventions returns interventions based on filtering`() {
    val pccRegionIds = emptyList<String>()
    val minimumAge: Int? = 18
    val maximumAge: Int? = 24
    val interventionDTOs = emptyList<InterventionDTO>()
    whenever(interventionService.getInterventions(pccRegionIds, minimumAge, maximumAge)).thenReturn(interventionDTOs)

    val responseDTOs = interventionController.getInterventions(pccRegionIds, minimumAge, maximumAge)

    verify(interventionService).getInterventions(pccRegionIds, minimumAge, maximumAge)
    assertSame(interventionDTOs, responseDTOs)
  }

  @Test
  fun `getInterventions returns interventions unfiltered when no parameters supplied`() {
    val pccRegionIds: List<String>? = null
    val minimumAge: Int? = null
    val maximumAge: Int? = null
    val interventionDTOs = emptyList<InterventionDTO>()
    whenever(interventionService.getInterventions(emptyList(), minimumAge, maximumAge)).thenReturn(interventionDTOs)

    val responseDTOs = interventionController.getInterventions(pccRegionIds, minimumAge, maximumAge)

    verify(interventionService).getInterventions(emptyList(), minimumAge, maximumAge)
    assertSame(interventionDTOs, responseDTOs)
  }
}
