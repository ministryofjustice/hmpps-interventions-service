package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.json.JsonTest
import org.springframework.boot.test.json.JacksonTester
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ComplexityLevel
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ComplexityLevelTitle
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.DesiredOutcome
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ServiceCategoryFactory
import java.util.UUID

@JsonTest
class ServiceCategoryFullDTOTest(@Autowired private val json: JacksonTester<ServiceCategoryFullDTO>) {
  private val serviceCategoryFactory = ServiceCategoryFactory()

  @Test
  fun `test serialization of service category values when all the complexity level is available and ordered by complexity levels`() {
    val serviceCategory = serviceCategoryFactory.create(
      id = UUID.fromString("e30c24e5-3aa7-4820-82ee-b028909876e6"),
      complexityLevels = listOf(
        ComplexityLevel(UUID.fromString("11c6d1f1-a22e-402a-b343-e57595876857"), "Low complexity", "Low complexity", ComplexityLevelTitle.LOW_COMPLEXITY),
        ComplexityLevel(UUID.fromString("11c6d1f1-a22e-402a-b343-e57595876858"), "Medium complexity", "Medium complexity", ComplexityLevelTitle.MEDIUM_COMPLEXITY),
        ComplexityLevel(UUID.fromString("11c6d1f1-a22e-402a-b343-e57595876859"), "High complexity", "High complexity", ComplexityLevelTitle.HIGH_COMPLEXITY)
      ),
      desiredOutcomes = listOf(DesiredOutcome(UUID.fromString("5a9d6e60-c314-4bf9-bf1e-b42b366b9398"), "good accommodation", UUID.fromString("e30c24e5-3aa7-4820-82ee-b028909876e6")))
    )

    val out = json.write(ServiceCategoryFullDTO.from(serviceCategory))
    assertThat(out).isEqualToJson(
      """
      {
        "id": "e30c24e5-3aa7-4820-82ee-b028909876e6",
        "complexityLevels": [{
           "id": "11c6d1f1-a22e-402a-b343-e57595876857",
           "title": "Low complexity",
           "description":"Low complexity"
        },
        {
           "id": "11c6d1f1-a22e-402a-b343-e57595876858",
           "title": "Medium complexity",
           "description":"Medium complexity"
        },
        {
           "id": "11c6d1f1-a22e-402a-b343-e57595876859",
           "title": "High complexity",
           "description":"High complexity"
        }],
        "desiredOutcomes": [{
           "id": "5a9d6e60-c314-4bf9-bf1e-b42b366b9398",
           "description": "good accommodation"
        }]
      }
    """
    )
  }

  @Test
  fun `test serialization of service category values when some complexity levels are not available and ordered by complexity levels`() {
    val serviceCategory = serviceCategoryFactory.create(
      id = UUID.fromString("e30c24e5-3aa7-4820-82ee-b028909876e6"),
      complexityLevels = listOf(
        ComplexityLevel(UUID.fromString("11c6d1f1-a22e-402a-b343-e57595876857"), "Low complexity", "Low complexity", ComplexityLevelTitle.LOW_COMPLEXITY),
        ComplexityLevel(UUID.fromString("11c6d1f1-a22e-402a-b343-e57595876859"), "High complexity", "High complexity", ComplexityLevelTitle.HIGH_COMPLEXITY)
      ),
      desiredOutcomes = listOf(DesiredOutcome(UUID.fromString("5a9d6e60-c314-4bf9-bf1e-b42b366b9398"), "good accommodation", UUID.fromString("e30c24e5-3aa7-4820-82ee-b028909876e6")))
    )

    val out = json.write(ServiceCategoryFullDTO.from(serviceCategory))
    assertThat(out).isEqualToJson(
      """
      {
        "id": "e30c24e5-3aa7-4820-82ee-b028909876e6",
        "complexityLevels": [{
           "id": "11c6d1f1-a22e-402a-b343-e57595876857",
           "title": "Low complexity",
           "description":"Low complexity"
        },
        {
           "id": "11c6d1f1-a22e-402a-b343-e57595876859",
           "title": "High complexity",
           "description":"High complexity"
        }],
        "desiredOutcomes": [{
           "id": "5a9d6e60-c314-4bf9-bf1e-b42b366b9398",
           "description": "good accommodation"
        }]
      }
    """
    )
  }
}
